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

import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.spout.pcap.scheme.TimestampScheme;
import storm.kafka.BrokerHosts;

public class SpoutConfig extends org.apache.metron.common.spout.kafka.SpoutConfig{

  public SpoutConfig(BrokerHosts hosts, String topic, String zkRoot, String id) {
    super(hosts, topic, zkRoot, id);
  }

  public SpoutConfig withTimestampScheme(String scheme, String granularity) {
    super.scheme = TimestampScheme.getScheme(scheme, TimestampConverters.getConverter(granularity));
    return this;
  }
}
