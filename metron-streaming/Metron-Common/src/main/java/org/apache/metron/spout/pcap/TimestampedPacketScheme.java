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

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.google.common.collect.ImmutableList;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import storm.kafka.KeyValueScheme;

import java.util.List;

public class TimestampedPacketScheme implements KeyValueScheme {
    private static final String KV_FIELD = "kv";

    @Override
    public List<Object> deserializeKeyAndValue(byte[] key, byte[] value) {
        Long ts = Bytes.toLong(key);
        System.out.println("Scheme: " + ts);
        return new Values(ImmutableList.of(new LongWritable(ts), new BytesWritable(value)));
    }

    @Override
    public List<Object> deserialize(byte[] ser) {
        throw new UnsupportedOperationException("Really only interested in deserializing a key and a value");
    }

    @Override
    public Fields getOutputFields() {
        return new Fields(KV_FIELD);
    }
}
