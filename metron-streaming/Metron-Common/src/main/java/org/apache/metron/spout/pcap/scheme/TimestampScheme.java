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
import org.apache.metron.spout.pcap.Endianness;
import storm.kafka.KeyValueSchemeAsMultiScheme;

public enum TimestampScheme {
  FROM_KEY( (converter, endianness) -> new KeyValueSchemeAsMultiScheme(new FromKeyScheme().withTimestampConverter(converter).withEndianness(endianness)))
  ,FROM_PACKET((converter, endianness) -> new FromPacketScheme().withTimestampConverter(converter).withEndianness(endianness));
  ;
  public static final String KV_FIELD = "kv";
  TimestampSchemeCreator creator;
  TimestampScheme(TimestampSchemeCreator creator)
  {
    this.creator = creator;
  }

  public static MultiScheme getScheme(String scheme, TimestampConverter converter, Endianness endianness) {
    try {
      TimestampScheme ts = TimestampScheme.valueOf(scheme.toUpperCase());
      return ts.creator.create(converter, endianness);
    }
    catch(IllegalArgumentException iae) {
      return TimestampScheme.FROM_KEY.creator.create(converter, endianness);
    }
  }

}
