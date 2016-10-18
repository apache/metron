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

package org.apache.metron.spout.pcap.scheme;

import org.apache.kafka.common.utils.Utils;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import com.google.common.collect.ImmutableList;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.log4j.Logger;
import org.apache.metron.common.utils.timestamp.TimestampConverter;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.spout.pcap.Endianness;
import org.apache.storm.kafka.KeyValueScheme;

import java.nio.ByteBuffer;
import java.util.List;

public class FromKeyScheme implements KeyValueScheme, KeyConvertible {
  private static final Logger LOG = Logger.getLogger(FromKeyScheme.class);

  private TimestampConverter converter = TimestampConverters.MICROSECONDS;
  private static Endianness endianness = Endianness.getNativeEndianness();
  @Override
  public List<Object> deserializeKeyAndValue(ByteBuffer key, ByteBuffer value) {
    Long ts = converter.toNanoseconds(key.asLongBuffer().get());
    byte[] packetHeaderized = PcapHelper.addPacketHeader(ts, Utils.toArray(value), endianness);
    byte[] globalHeaderized= PcapHelper.addGlobalHeader(packetHeaderized, endianness);
    return new Values(ImmutableList.of(new LongWritable(ts), new BytesWritable(globalHeaderized)));
  }

  @Override
  public List<Object> deserialize(ByteBuffer ser) {
    throw new UnsupportedOperationException("Really only interested in deserializing a key and a value");
  }

  @Override
  public Fields getOutputFields() {
    return new Fields(TimestampScheme.KV_FIELD);
  }

  @Override
  public FromKeyScheme withTimestampConverter(TimestampConverter converter) {
    try {
      this.converter = converter;
    }
    catch(IllegalArgumentException iae) {
      LOG.error(iae.getMessage(), iae);
    }
    return this;
  }


}
