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
import storm.kafka.KeyValueSchemeAsMultiScheme;

public enum TimestampScheme {
  FROM_KEY(new KeyValueSchemeAsMultiScheme(new FromKeyScheme()))
  ,FROM_PACKET(new FromPacketScheme());
  ;
  public static final String KV_FIELD = "kv";
  MultiScheme scheme;
  TimestampScheme(MultiScheme scheme)
  {
    this.scheme = scheme;
  }
  public MultiScheme getScheme() {
    return scheme;
  }

  public static MultiScheme getScheme(String scheme) {
    try {
      TimestampScheme ts = TimestampScheme.valueOf(scheme.toUpperCase());
      return ts.getScheme();
    }
    catch(IllegalArgumentException iae) {
      return TimestampScheme.FROM_KEY.getScheme();
    }
  }

}
