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

import backtype.storm.spout.MultiScheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.google.common.collect.ImmutableList;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.log4j.Logger;
import org.apache.metron.common.utils.timestamp.TimestampConverter;
import org.apache.metron.pcap.PcapHelper;

import java.util.Collections;
import java.util.List;

public class FromPacketScheme implements MultiScheme,KeyConvertible {
  private static final Logger LOG = Logger.getLogger(FromPacketScheme.class);
  @Override
  public Iterable<List<Object>> deserialize(byte[] rawValue) {
    byte[] value = rawValue;
    Long ts = PcapHelper.getTimestamp(value);
    if(ts != null) {
      return ImmutableList.of(new Values(ImmutableList.of(new LongWritable(ts), new BytesWritable(rawValue))));
    }
    else {
      return ImmutableList.of(new Values(Collections.EMPTY_LIST));
    }
  }

  @Override
  public Fields getOutputFields() {
    return new Fields(TimestampScheme.KV_FIELD);
  }


  @Override
  public FromPacketScheme withTimestampConverter(TimestampConverter converter) {
    return this;
  }
}
