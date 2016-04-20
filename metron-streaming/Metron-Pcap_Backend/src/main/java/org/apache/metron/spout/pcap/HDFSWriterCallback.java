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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.map.HashedMap;
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
import storm.kafka.PartitionManager;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HDFSWriterCallback implements Callback {
    static final long serialVersionUID = 0xDEADBEEFL;
    private static final Logger LOG = Logger.getLogger(HDFSWriterCallback.class);

    static class Partition {
        String topic;
        int partition;

        public Partition(String topic, int partition) {
            this.topic = topic;
            this.partition = partition;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Partition partition1 = (Partition) o;

            if (partition != partition1.partition) return false;
            return topic != null ? topic.equals(partition1.topic) : partition1.topic == null;

        }

        @Override
        public int hashCode() {
            int result = topic != null ? topic.hashCode() : 0;
            result = 31 * result + partition;
            return result;
        }

        @Override
        public String toString() {
            return "Partition{" +
                    "topic='" + topic + '\'' +
                    ", partition=" + partition +
                    '}';
        }
    }

    private HDFSWriterConfig config;
    private EmitContext context;
    private Map<Partition, PartitionHDFSWriter> writers = new HashMap<>();
    private PartitionHDFSWriter lastWriter = null;
    private String topic;
    public HDFSWriterCallback() {
    }

    public HDFSWriterCallback withConfig(HDFSWriterConfig config) {
        LOG.info("Configured: " + config);
        this.config = config;
        return this;
    }
    @Override
    public List<Object> apply(List<Object> tuple, EmitContext context) {

        List<Object> keyValue = (List<Object>) tuple.get(0);
        LongWritable ts = (LongWritable) keyValue.get(0);
        BytesWritable rawPacket = (BytesWritable)keyValue.get(1);
        try {
            getWriter(new Partition( topic
                                   , context.get(EmitContext.Type.PARTITION))
                     ).handle(ts, rawPacket);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            //drop?  not sure..
        }
        return tuple;
    }

    private PartitionHDFSWriter getWriter(Partition partition) {
        //if we have just one partition or a run from a single partition in this spout, we don't want a map lookup
        if(lastWriter != null && lastWriter.getTopic().equals(partition.topic) && lastWriter.getPartition() == partition.partition) {
            return lastWriter;
        }
        lastWriter = writers.get(partition);
        if(lastWriter == null) {
            lastWriter = new PartitionHDFSWriter( partition.topic
                                         , partition.partition
                                         , context.get(EmitContext.Type.UUID)
                                         , config
                                         );
            writers.put(partition, lastWriter);
        }
        return lastWriter;
    }

    @Override
    public void initialize(EmitContext context) {
        this.context = context;
        this.topic = context.get(EmitContext.Type.TOPIC);
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
        for(PartitionHDFSWriter writer : writers.values()) {
            writer.close();
        }
    }
}
