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
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;
import org.apache.metron.spout.pcap.deserializer.KeyValueDeserializer;
import org.apache.storm.kafka.Callback;
import org.apache.storm.kafka.EmitContext;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A callback which gets executed as part of the spout to write pcap data to HDFS.
 */
public class HDFSWriterCallback implements Callback {

    static final long serialVersionUID = 0xDEADBEEFL;
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * A topic+partition.  We split the files up by topic+partition so the writers don't clobber each other
     */
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
    private boolean inited = false;
    public HDFSWriterCallback() {
    }

    public HDFSWriterCallback withConfig(HDFSWriterConfig config) {
        LOG.info("Configured: {}", config);
        this.config = config;
        return this;
    }

    @Override
    public List<Object> apply(List<Object> tuple, EmitContext context) {
        byte[] key = (byte[]) tuple.get(0);
        byte[] value = (byte[]) tuple.get(1);
        long tsDeserializeStart = System.nanoTime();
        KeyValueDeserializer.Result result = config.getDeserializer().deserializeKeyValue(key, value);
        long tsDeserializeEnd = System.nanoTime();

        if (LOG.isDebugEnabled() && !result.foundTimestamp) {
            List<String> debugStatements = new ArrayList<>();
            if (key != null) {
                debugStatements.add("Key length: " + key.length);
                debugStatements.add("Key: " + DatatypeConverter.printHexBinary(key));
            } else {
                debugStatements.add("Key is null!");
            }

            if (value != null) {
                debugStatements.add("Value length: " + value.length);
                debugStatements.add("Value: " + DatatypeConverter.printHexBinary(value));
            } else {
                debugStatements.add("Value is null!");
            }
            LOG.debug("Dropping malformed packet: {}", Joiner.on(" / ").join(debugStatements));
        }

        long tsWriteStart = System.nanoTime();
        try {
            getWriter(new Partition( topic
                                   , context.get(EmitContext.Type.PARTITION))
                     ).handle(result.key, result.value);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            //drop?  not sure..
        }
        long tsWriteEnd = System.nanoTime();
        if(LOG.isDebugEnabled() && (Math.random() < 0.001 || !inited)) {
            LOG.debug("Deserialize time (ns): {}", (tsDeserializeEnd - tsDeserializeStart));
            LOG.debug("Write time (ns): {}", (tsWriteEnd - tsWriteStart));
        }
        inited = true;
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
        KafkaSpoutConfig spoutConfig = context.get(EmitContext.Type.SPOUT_CONFIG);
        if(spoutConfig != null && spoutConfig.getSubscription() != null) {
            this.topic = spoutConfig.getSubscription().getTopicsString();
            if(this.topic.length() > 0) {
                int len = this.topic.length();
                if(this.topic.charAt(0) == '[' && this.topic.charAt(len - 1) == ']') {
                    this.topic = this.topic.substring(1, len - 1);
                }
            }
        }
        else {
            throw new IllegalStateException("Unable to initialize, because spout config is not correctly specified");
        }
    }

    @Override
    public void close() throws Exception {
        for(PartitionHDFSWriter writer : writers.values()) {
            writer.close();
        }
    }
}
