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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream;
import org.apache.hadoop.hdfs.util.MD5FileUtils;
import org.apache.hadoop.io.MD5Hash;
import org.apache.log4j.Logger;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.TimedRotationPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.hdfs.common.rotation.RotationAction;
import org.apache.storm.hdfs.common.security.HdfsSecurityUtil;
import org.json.simple.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class SourceHandler {
  private static final Logger LOG = Logger.getLogger(SourceHandler.class);
  List<RotationAction> rotationActions = new ArrayList<>();
  FileRotationPolicy rotationPolicy;
  SyncPolicy syncPolicy;
  FileNameFormat fileNameFormat;
  private long offset = 0;
  private int rotation = 0;
  private transient FSDataOutputStream out;
  private transient Object writeLock;
  protected transient Timer rotationTimer; // only used for TimedRotationPolicy
  protected transient FileSystem fs;
  protected transient Path currentFile;
  public SourceHandler(List<RotationAction> rotationActions
                      , FileRotationPolicy rotationPolicy
                      , SyncPolicy syncPolicy
                      , FileNameFormat fileNameFormat
                      , Map config
                      ) throws IOException {
    this.rotationActions = rotationActions;
    this.rotationPolicy = rotationPolicy;
    this.syncPolicy = syncPolicy;
    this.fileNameFormat = fileNameFormat;
    initialize(config);
  }

  public void handle(List<JSONObject> messages) throws Exception{

    for(JSONObject message : messages) {
      byte[] bytes = (message.toJSONString() + "\n").getBytes();
      synchronized (this.writeLock) {
        out.write(bytes);
        this.offset += bytes.length;

        if (this.syncPolicy.mark(null, this.offset)) {
          if (this.out instanceof HdfsDataOutputStream) {
            ((HdfsDataOutputStream) this.out).hsync(EnumSet.of(HdfsDataOutputStream.SyncFlag.UPDATE_LENGTH));
          } else {
            this.out.hsync();
          }
          this.syncPolicy.reset();
        }
      }

      if (this.rotationPolicy.mark(null, this.offset)) {
        rotateOutputFile(); // synchronized
        this.offset = 0;
        this.rotationPolicy.reset();
      }
    }
  }

  private void initialize(Map config) throws IOException {
    this.writeLock = new Object();
    Configuration hdfsConfig = new Configuration();
    this.fs = FileSystem.get(new Configuration());
    HdfsSecurityUtil.login(config, hdfsConfig);
    this.currentFile = createOutputFile();
    if(this.rotationPolicy instanceof TimedRotationPolicy){
      long interval = ((TimedRotationPolicy)this.rotationPolicy).getInterval();
      this.rotationTimer = new Timer(true);
      TimerTask task = new TimerTask() {
        @Override
        public void run() {
          try {
            rotateOutputFile();
          } catch(IOException e){
            LOG.warn("IOException during scheduled file rotation.", e);
          }
        }
      };
      this.rotationTimer.scheduleAtFixedRate(task, interval, interval);
    }
  }

  protected void rotateOutputFile() throws IOException {
    LOG.info("Rotating output file...");
    long start = System.currentTimeMillis();
    synchronized (this.writeLock) {
      closeOutputFile();
      this.rotation++;

      Path newFile = createOutputFile();
      LOG.info("Performing " +  this.rotationActions.size() + " file rotation actions." );
      for (RotationAction action : this.rotationActions) {
        action.execute(this.fs, this.currentFile);
      }
      this.currentFile = newFile;
    }
    long time = System.currentTimeMillis() - start;
    LOG.info("File rotation took " + time + " ms.");
  }

  private Path createOutputFile() throws IOException {
    Path path = new Path(this.fileNameFormat.getPath(), this.fileNameFormat.getName(this.rotation, System.currentTimeMillis()));
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

  private void closeOutputFile() throws IOException {
    this.out.close();
  }


  public void close() {
    try {
      closeOutputFile();
    } catch (IOException e) {
      throw new RuntimeException("Unable to close output file.", e);
    }
  }
}
