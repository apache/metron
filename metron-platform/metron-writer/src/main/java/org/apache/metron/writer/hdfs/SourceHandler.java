/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.writer.hdfs;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.TimedRotationPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.hdfs.common.rotation.RotationAction;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceHandler {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  List<RotationAction> rotationActions = new ArrayList<>();
  FileRotationPolicy rotationPolicy;
  SyncPolicy syncPolicy;
  FileNameFormat fileNameFormat;
  SourceHandlerCallback cleanupCallback;
  private long offset = 0;
  private int rotation = 0;
  private transient FSDataOutputStream out;
  private transient final Object writeLock = new Object();
  protected transient Timer rotationTimer; // only used for TimedRotationPolicy
  protected transient FileSystem fs;
  protected transient Path currentFile;
  public SourceHandler(List<RotationAction> rotationActions
                      , FileRotationPolicy rotationPolicy
                      , SyncPolicy syncPolicy
                      , FileNameFormat fileNameFormat
                      , SourceHandlerCallback cleanupCallback) throws IOException {
    this.rotationActions = rotationActions;
    this.rotationPolicy = rotationPolicy;
    this.syncPolicy = syncPolicy;
    this.fileNameFormat = fileNameFormat;
    this.cleanupCallback = cleanupCallback;
    initialize();
  }


  protected void handle(JSONObject message, String sensor, WriterConfiguration config, SyncPolicyCreator syncPolicyCreator) throws IOException {
    byte[] bytes = (message.toJSONString() + "\n").getBytes();
    synchronized (this.writeLock) {
      try {
        out.write(bytes);
      } catch (IOException writeException) {
        LOG.warn("IOException while writing output", writeException);
        // If the stream is closed, attempt to rotate the file and try again, hoping it's transient
        if (writeException.getMessage().contains("Stream Closed")) {
          LOG.warn("Output Stream was closed. Attempting to rotate file and continue");
          rotateOutputFile();
          // If this write fails, the exception will be allowed to bubble up.
          out.write(bytes);
        } else {
          throw writeException;
        }
      }
      this.offset += bytes.length;

      LOG.debug("Checking if hsync necessary");
      if (this.syncPolicy.mark(null, this.offset)) {
        LOG.debug("Calling hsync on {}", this.out.getClass().getSimpleName());
        if (this.out instanceof HdfsDataOutputStream) {
          ((HdfsDataOutputStream) this.out)
              .hsync(EnumSet.of(HdfsDataOutputStream.SyncFlag.UPDATE_LENGTH));
        } else {
          this.out.hsync();
        }
        //recreate the sync policy for the next batch just in case something changed in the config
        //and the sync policy depends on the config.
        LOG.debug("Recreating sync policy for next batch");
        this.syncPolicy = syncPolicyCreator.create(sensor, config);
      }
    }

    LOG.debug("checking for rotation");
    if (this.rotationPolicy.mark(null, this.offset)) {
      LOG.debug("Rotating output File from handle()");
      rotateOutputFile(); // synchronized
//      this.offset = 0;
//      this.rotationPolicy.reset();
    }
  }

  private void initialize() throws IOException {
    this.fs = FileSystem.get(new Configuration());
    this.currentFile = createOutputFile();
    if(this.rotationPolicy instanceof TimedRotationPolicy){
      LOG.debug("Creating timer task");
      long interval = ((TimedRotationPolicy)this.rotationPolicy).getInterval();
      this.rotationTimer = new Timer(true);
      TimerTask task = new TimerTask() {
        @Override
        public void run() {
          try {
            LOG.debug("Rotating output file from TimerTask");
            rotateOutputFile();
          } catch(IOException e){
            LOG.warn("IOException during scheduled file rotation.", e);
          }
        }
      };
      LOG.debug("Scheduling TimerTask at interval {}", interval);
      this.rotationTimer.scheduleAtFixedRate(task, interval, interval);
    }
  }

  protected void rotateOutputFile() throws IOException {
    LOG.info("Rotating output file...");
    long start = System.currentTimeMillis();
    synchronized (this.writeLock) {
      closeOutputFile();
      // Want to use the callback to make sure we have an accurate count of open files.
      cleanupCallback();
      this.rotation++;

      Path newFile = createOutputFile();
      LOG.info("Performing {} file rotation actions.", this.rotationActions.size());
      for (RotationAction action : this.rotationActions) {
        action.execute(this.fs, this.currentFile);
      }
      this.currentFile = newFile;
      this.offset = 0;
      this.rotationPolicy.reset();
    }
    long time = System.currentTimeMillis() - start;
    LOG.info("File rotation took {} ms", time);
  }

  private Path createOutputFile() throws IOException {
    Path path = new Path(this.fileNameFormat.getPath(), this.fileNameFormat.getName(this.rotation, System.currentTimeMillis()));
    LOG.debug("Creating new output file {}", path);
    if(fs.getScheme().equals("file")) {
      LOG.debug("Running on local file system");
      //in the situation where we're running this in a local filesystem, flushing doesn't work.
      fs.mkdirs(path.getParent());
      this.out = new FSDataOutputStream(new FileOutputStream(path.toString()), null);
    }
    else {
      LOG.debug("Running on remote file system");
      this.out = this.fs.create(path);
    }
    return path;
  }

  protected void closeOutputFile() throws IOException {
    LOG.debug("Closing output file");
    this.out.close();
  }

  private void cleanupCallback() {
    LOG.debug("Performing cleanupCallback");
    this.cleanupCallback.removeKey();
  }

  public void close() {
    try {
      closeOutputFile();
      // Don't call cleanup, to avoid HashMap's ConcurrentModificationException while iterating
    } catch (IOException e) {
      throw new RuntimeException("Unable to close output file.", e);
    }
  }

  @Override
  public String toString() {
    return "SourceHandler{" +
            "rotationActions=" + rotationActions +
            ", rotationPolicy=" + rotationPolicy +
            ", syncPolicy=" + syncPolicy +
            ", fileNameFormat=" + fileNameFormat +
            ", offset=" + offset +
            ", rotation=" + rotation +
            ", out=" + out +
            ", writeLock=" + writeLock +
            ", rotationTimer=" + rotationTimer +
            ", fs=" + fs +
            ", currentFile=" + currentFile +
            '}';
  }
}
