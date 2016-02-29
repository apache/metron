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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.log4j.Logger;
import storm.kafka.Callback;
import storm.kafka.EmitContext;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class HDFSWriterCallback implements Callback {
  static final long serialVersionUID = 0xDEADBEEFL;
  private static final Logger LOG = Logger.getLogger(HDFSWriterCallback.class);
  public static final byte[] PCAP_GLOBAL_HEADER = new byte[] {
          (byte) 0xd4, (byte) 0xc3, (byte) 0xb2, (byte) 0xa1, 0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00
          ,0x00, 0x00, 0x00, 0x00, (byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00
  };

  private static final List<Object> RET_TUPLE = ImmutableList.of((Object)Byte.valueOf((byte) 0x00), Byte.valueOf((byte)0x00));
  private FileSystem fs;
  private SequenceFile.Writer writer;
  private HDFSWriterConfig config;
  private long batchStartTime;
  private long numWritten;
  private EmitContext context;

  public HDFSWriterCallback() {
    //this.config = config;
  }

  public HDFSWriterCallback withConfig(HDFSWriterConfig config) {
    LOG.info("Configured: " + config);
    this.config = config;
    return this;
  }

  @Override
  public List<Object> apply(List<Object> tuple, EmitContext context) {

    LongWritable ts = (LongWritable) tuple.get(0);
    BytesWritable rawPacket = (BytesWritable)tuple.get(1);
    try {
      turnoverIfNecessary(ts.get());
      writer.append(ts, headerize(rawPacket.getBytes()));
      writer.hflush();
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      //drop?  not sure..
    }
    return RET_TUPLE;
  }

  private static BytesWritable headerize(byte[] packet) {
    byte[] ret = new byte[packet.length + PCAP_GLOBAL_HEADER.length];
    int offset = 0;
    System.arraycopy(PCAP_GLOBAL_HEADER, 0, ret, offset, PCAP_GLOBAL_HEADER.length);
    offset += PCAP_GLOBAL_HEADER.length;
    System.arraycopy(packet, 0, ret, offset, packet.length);
    return new BytesWritable(ret);
  }


  private synchronized void turnoverIfNecessary(long ts) throws IOException {
    long duration = ts - batchStartTime;
    if(batchStartTime == 0L || duration > config.getMaxTimeMS() || numWritten > config.getNumPackets()) {
      //turnover
      Path path = getPath(ts);
      if(writer != null) {
        writer.close();
      }
      writer = SequenceFile.createWriter(new Configuration()
              , SequenceFile.Writer.file(path)
              , SequenceFile.Writer.keyClass(LongWritable.class)
              , SequenceFile.Writer.valueClass(BytesWritable.class)
      );
      //reset state
      LOG.info("Turning over and writing to " + path);
      batchStartTime = ts;
      numWritten = 0;
    }
  }

  private Path getPath(long ts) {
    String fileName = Joiner.on("_").join("pcap"
            , "" + ts
            , context.get(EmitContext.Type.UUID)
    );
    return new Path(config.getOutputPath(), fileName);
  }

  @Override
  public void initialize(EmitContext context) {
    this.context = context;
    try {
      fs = FileSystem.get(new Configuration());
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create filesystem", e);
    }
  }

  /**
   * Closes this resource, relinquishing any underlying resources.
   * This method is invoked automatically on objects managed by the
   * {@code try}-with-resources statement.
   * <p/>
   * <p>While this interface method is declared to throw {@code
   * Exception}, implementers are <em>strongly</em> encouraged to
   * declare concrete implementations of the {@code close} method to
   * throw more specific exceptions, or to throw no exception at all
   * if the close operation cannot fail.
   * <p/>
   * <p><em>Implementers of this interface are also strongly advised
   * to not have the {@code close} method throw {@link
   * InterruptedException}.</em>
   * <p/>
   * This exception interacts with a thread's interrupted status,
   * and runtime misbehavior is likely to occur if an {@code
   * InterruptedException} is {@linkplain Throwable#addSuppressed
   * suppressed}.
   * <p/>
   * More generally, if it would cause problems for an
   * exception to be suppressed, the {@code AutoCloseable.close}
   * method should not throw it.
   * <p/>
   * <p>Note that unlike the {@link Closeable#close close}
   * method of {@link Closeable}, this {@code close} method
   * is <em>not</em> required to be idempotent.  In other words,
   * calling this {@code close} method more than once may have some
   * visible side effect, unlike {@code Closeable.close} which is
   * required to have no effect if called more than once.
   * <p/>
   * However, implementers of this interface are strongly encouraged
   * to make their {@code close} methods idempotent.
   *
   * @throws Exception if this resource cannot be closed
   */
  @Override
  public void close() throws Exception {
    if(writer != null) {
      writer.close();
    }
  }
}
