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

      if (this.syncPolicy.mark(null, this.offset)) {
        LOG.debug("Calling hsync per Sync Policy");
        if (this.out instanceof HdfsDataOutputStream) {
          ((HdfsDataOutputStream) this.out)
              .hsync(EnumSet.of(HdfsDataOutputStream.SyncFlag.UPDATE_LENGTH));
        } else {
          this.out.hsync();
        }
        //recreate the sync policy for the next batch just in case something changed in the config
        //and the sync policy depends on the config.
        LOG.debug("Recreating sync policy");
        this.syncPolicy = syncPolicyCreator.create(sensor, config);
      }
    }

    if (this.rotationPolicy.mark(null, this.offset)) {
      LOG.debug("Rotating due to rotationPolicy");
      rotateOutputFile(); // synchronized
      this.offset = 0;
      this.rotationPolicy.reset();
    }
  }

  private void initialize() throws IOException {
    LOG.debug("Initializing Source Handler");
    this.fs = FileSystem.get(new Configuration());
    this.currentFile = createOutputFile();
    LOG.debug("Source Handler initialized with starting file: {}", currentFile);
    if(this.rotationPolicy instanceof TimedRotationPolicy){
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
      this.rotationTimer.scheduleAtFixedRate(task, interval, interval);
    }
  }

  // Closes the output file, but ensures any RotationActions are performed.
  protected void rotateOutputFile() throws IOException {
    LOG.debug("Rotating output file...");
    long start = System.currentTimeMillis();
    synchronized (this.writeLock) {
      closeOutputFile();
      // Want to use the callback to make sure we have an accurate count of open files.
      cleanupCallback();

      LOG.debug("Performing {} file rotation actions.", this.rotationActions.size());
      for (RotationAction action : this.rotationActions) {
        action.execute(this.fs, this.currentFile);
      }
    }
    long time = System.currentTimeMillis() - start;
    LOG.info("File rotation took {} ms", time);
  }

  private Path createOutputFile() throws IOException {
    // The rotation is set to 0. With the way open files are tracked and managed with the callback, there will
    // never be data that would go into a rotation > 0. Instead a new SourceHandler, and by extension file, will
    // be created.
    Path path = new Path(this.fileNameFormat.getPath(), this.fileNameFormat.getName(0, System.currentTimeMillis()));
    LOG.debug("Creating new output file: {}", path.getName());
    if(fs.getScheme().equals("file")) {
      //in the situation where we're running this in a local filesystem, flushing doesn't work.
      fs.mkdirs(path.getParent());
      this.out = new FSDataOutputStream(new FileOutputStream(path.toString()), null);
    }
    else {
      this.out = this.fs.create(path);
    }
    return path;
  }

  protected void closeOutputFile() throws IOException {
    this.out.close();
  }

  private void cleanupCallback() {
    this.cleanupCallback.removeKey();
  }

  public void close() {
    try {
      closeOutputFile();
      if(rotationTimer != null) {
        rotationTimer.cancel();
      }
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
            ", out=" + out +
            ", writeLock=" + writeLock +
            ", rotationTimer=" + rotationTimer +
            ", fs=" + fs +
            ", currentFile=" + currentFile +
            '}';
  }
}
