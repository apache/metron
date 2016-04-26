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
import org.apache.log4j.Logger;
import org.apache.metron.pcap.PcapHelper;

import java.io.*;
import java.util.EnumSet;

public class PartitionHDFSWriter implements AutoCloseable, Serializable {
  static final long serialVersionUID = 0xDEADBEEFL;
  private static final Logger LOG = Logger.getLogger(PartitionHDFSWriter.class);


  public static interface SyncHandler {
    void sync(FSDataOutputStream outputStream) throws IOException;
  }

  public static enum SyncHandlers implements SyncHandler{
    DEFAULT(new SyncHandler() {

      public void sync(FSDataOutputStream outputStream) throws IOException {
        outputStream.hflush();
        outputStream.hsync();
      }
    })
    ,HDFS(new SyncHandler() {
      public void sync(FSDataOutputStream outputStream) throws IOException{

        outputStream.hflush();
        outputStream.hsync();
        ((HdfsDataOutputStream)outputStream).hsync(EnumSet.of(HdfsDataOutputStream.SyncFlag.UPDATE_LENGTH));
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
    public void sync(FSDataOutputStream input) throws IOException {
      func.sync(input);
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

  public PartitionHDFSWriter(String topic, int partition, String uuid, HDFSWriterConfig config) {
    this.topic = topic;
    this.partition = partition;
    this.uuid = uuid;
    this.config = config;
    try {
      this.fs = FileSystem.get(new Configuration());
    } catch (IOException e) {
      throw new RuntimeException("Unable to get FileSystem", e);
    }
  }

  public String timestampToString(long ts) {
    return Long.toUnsignedString(ts);
  }

  public void handle(LongWritable ts, BytesWritable value) throws IOException {
    turnoverIfNecessary(ts.get());
    writer.append(ts, new BytesWritable(value.getBytes()));
    syncHandler.sync(outputStream);
    numWritten++;
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
    boolean overDuration = duration >= config.getMaxTimeNS();
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
      writer = SequenceFile.createWriter(new Configuration()
              , SequenceFile.Writer.keyClass(LongWritable.class)
              , SequenceFile.Writer.valueClass(BytesWritable.class)
              , SequenceFile.Writer.stream(outputStream)
              , SequenceFile.Writer.compression(SequenceFile.CompressionType.NONE)
      );
      //reset state
      LOG.info("Turning over and writing to " + path);
      batchStartTime = ts;
      numWritten = 0;
    }
  }

}
