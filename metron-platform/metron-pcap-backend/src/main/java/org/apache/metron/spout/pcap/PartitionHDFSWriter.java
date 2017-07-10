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

package org.apache.metron.spout.pcap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.metron.pcap.PcapHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.Map;

/**
 * This class is intended to handle the writing of an individual file.
 */
public class PartitionHDFSWriter implements AutoCloseable, Serializable {
  static final long serialVersionUID = 0xDEADBEEFL;
  private static final Logger LOG = LoggerFactory.getLogger(PartitionHDFSWriter.class);


  public static interface SyncHandler {
    void sync(FSDataOutputStream outputStream) throws IOException;
  }

  /*
  The sync handlers are FileSystem specific implementations of sync'ing.  The more often you sync, the more atomic the
  writing is.  There is a natural tradeoff between sync'ing often and performance.
   */
  public static enum SyncHandlers implements SyncHandler{
    DEFAULT(new SyncHandler() {

      @Override
      public void sync(FSDataOutputStream outputStream) throws IOException {
        outputStream.hflush();
        outputStream.hsync();
      }
    })
    ,HDFS(new SyncHandler() {
      @Override
      public void sync(FSDataOutputStream outputStream) throws IOException {
        outputStream.hflush();
        outputStream.hsync();
        ((HdfsDataOutputStream) outputStream).hsync(EnumSet.of(HdfsDataOutputStream.SyncFlag.UPDATE_LENGTH));
      }
    })
    ,LOCAL(new SyncHandler() {

      @Override
      public void sync(FSDataOutputStream outputStream) throws IOException {
        outputStream.getWrappedStream().flush();
        outputStream.getWrappedStream();
      }
    })
    ;
    private SyncHandler func;
    SyncHandlers(SyncHandler func) {
      this.func = func;
    }

    private SyncHandler getHandler() {
      return func;
    }

    @Override
    public void sync(FSDataOutputStream input) {
      try {
        func.sync(input);
      }
      catch(IOException ioe) {
        LOG.warn("Problems during sync, but this shouldn't be too concerning as long as it's intermittent: " + ioe.getMessage(), ioe);
      }
    }
  }


  private String topic;
  private int partition;
  private String uuid;
  private FileSystem fs;
  private FSDataOutputStream outputStream;
  private SequenceFile.Writer writer;
  private HDFSWriterConfig config;
  private SyncHandler syncHandler;
  private long batchStartTime;
  private long numWritten;
  private Configuration fsConfig = new Configuration();

  public PartitionHDFSWriter(String topic, int partition, String uuid, HDFSWriterConfig config) {
    this.topic = topic;
    this.partition = partition;
    this.uuid = uuid;
    this.config = config;

    try {
      int replicationFactor = config.getReplicationFactor();
      if (replicationFactor > 0) {
        fsConfig.set("dfs.replication", String.valueOf(replicationFactor));
      }
      if(config.getHDFSConfig() != null && !config.getHDFSConfig().isEmpty()) {
        for(Map.Entry<String, Object> entry : config.getHDFSConfig().entrySet()) {
          if(entry.getValue() instanceof Integer) {
            fsConfig.setInt(entry.getKey(), (int)entry.getValue());
          }
          else if(entry.getValue() instanceof Boolean)
          {
            fsConfig.setBoolean(entry.getKey(), (Boolean) entry.getValue());
          }
          else if(entry.getValue() instanceof Long)
          {
            fsConfig.setLong(entry.getKey(), (Long) entry.getValue());
          }
          else if(entry.getValue() instanceof Float)
          {
            fsConfig.setFloat(entry.getKey(), (Float) entry.getValue());
          }
          else
          {
            fsConfig.set(entry.getKey(), String.valueOf(entry.getValue()));
          }
        }
      }
      this.fs = FileSystem.get(fsConfig);
    } catch (IOException e) {
      throw new RuntimeException("Unable to get FileSystem", e);
    }
  }

  public String timestampToString(long ts) {
    return Long.toUnsignedString(ts);
  }

  public void handle(long ts, byte[] value) throws IOException {
    turnoverIfNecessary(ts);
    BytesWritable bw = new BytesWritable(value);
    try {
      writer.append(new LongWritable(ts), bw);
    }
    catch(ArrayIndexOutOfBoundsException aioobe) {
      LOG.warn("This appears to be HDFS-7765 (https://issues.apache.org/jira/browse/HDFS-7765), " +
              "which is an issue with syncing and not problematic: " + aioobe.getMessage(), aioobe);
    }
    numWritten++;
    if(numWritten % config.getSyncEvery() == 0) {
      syncHandler.sync(outputStream);
    }
  }

  public String getTopic() {
    return topic;
  }

  public int getPartition() {
    return partition;
  }


  @Override
  public void close() throws IOException {
    if(writer != null) {
      writer.close();
    }
    if(outputStream != null) {
      outputStream.close();
    }
  }

  private Path getPath(long ts) {
    String fileName = PcapHelper.toFilename(topic, ts, partition + "", uuid);
    return new Path(config.getOutputPath(), fileName);
  }

  private void turnoverIfNecessary(long ts) throws IOException {
    turnoverIfNecessary(ts, false);
  }

  private void turnoverIfNecessary(long ts, boolean force) throws IOException {
    long duration = ts - batchStartTime;
    boolean initial = outputStream == null;
    boolean overDuration = config.getMaxTimeNS() <= 0 ? false : Long.compareUnsigned(duration, config.getMaxTimeNS()) >= 0;
    boolean tooManyPackets = numWritten >= config.getNumPackets();
    if(force || initial || overDuration || tooManyPackets ) {
      //turnover
      Path path = getPath(ts);
      close();

      if(fs instanceof LocalFileSystem) {
        outputStream = new FSDataOutputStream(new FileOutputStream(new File(path.toString())));
        syncHandler = SyncHandlers.LOCAL.getHandler();
      }
      else {
        outputStream = fs.create(path, true);
        if (outputStream instanceof HdfsDataOutputStream) {
          if (initial) {
            LOG.info("Using the HDFS sync handler.");
          }
          syncHandler = SyncHandlers.HDFS.getHandler();
        } else {
          if (initial) {
            LOG.info("Using the default sync handler, which cannot guarantee atomic appends at the record level, be forewarned!");
          }
          syncHandler = SyncHandlers.DEFAULT.getHandler();
        }

      }
      writer = SequenceFile.createWriter(this.fsConfig
              , SequenceFile.Writer.keyClass(LongWritable.class)
              , SequenceFile.Writer.valueClass(BytesWritable.class)
              , SequenceFile.Writer.stream(outputStream)
              , SequenceFile.Writer.compression(SequenceFile.CompressionType.NONE)
      );
      //reset state
      LOG.info("Turning over and writing to {}: [duration={} NS, force={}, initial={}, overDuration={}, tooManyPackets={}]", path, duration, force, initial, overDuration, tooManyPackets);
      batchStartTime = ts;
      numWritten = 0;
    }
  }

}
