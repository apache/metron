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

import java.io.*;
import java.math.BigInteger;
import java.util.EnumSet;

public class PartitionHDFSWriter implements AutoCloseable, Serializable {
    static final long serialVersionUID = 0xDEADBEEFL;
    private static final Logger LOG = Logger.getLogger(PartitionHDFSWriter.class);
    public static final byte[] PCAP_GLOBAL_HEADER = new byte[] {
            (byte) 0xd4, (byte) 0xc3, (byte) 0xb2, (byte) 0xa1, 0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00
            ,0x00, 0x00, 0x00, 0x00, (byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00
    };



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
        writer.append(ts, headerize(value.getBytes()));
        syncHandler.sync(outputStream);
        numWritten++;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     * <p>
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     * <p>
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     * <p>
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     * <p>
     * <p>Note that unlike the {@link Closeable#close close}
     * method of {@link Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * <p>
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     *
     * @throws Exception if this resource cannot be closed
     */
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

        String fileName = Joiner.on("_").join("pcap"
                                             ,topic
                                             , timestampToString(ts)
                                             ,partition
                                             , uuid
        );
        return new Path(config.getOutputPath(), fileName);
    }

    private void turnoverIfNecessary(long ts) throws IOException {
        turnoverIfNecessary(ts, false);
    }

    private void turnoverIfNecessary(long ts, boolean force) throws IOException {
        long duration = ts - batchStartTime;
        boolean initial = outputStream == null;
        boolean overDuration = duration >= config.getMaxTimeMS();
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

    private static BytesWritable headerize(byte[] packet) {
        byte[] ret = new byte[packet.length + PCAP_GLOBAL_HEADER.length];
        int offset = 0;
        System.arraycopy(PCAP_GLOBAL_HEADER, 0, ret, offset, PCAP_GLOBAL_HEADER.length);
        offset += PCAP_GLOBAL_HEADER.length;
        System.arraycopy(packet, 0, ret, offset, packet.length);
        return new BytesWritable(ret);
    }
}
