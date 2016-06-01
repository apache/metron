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

package org.apache.metron.common.spout.kafka;

import storm.kafka.BrokerHosts;

public class SpoutConfig extends storm.kafka.SpoutConfig {
  public static enum Offset {
    BEGINNING, END, WHERE_I_LEFT_OFF;
  }
  public SpoutConfig(BrokerHosts hosts, String topic, String zkRoot, String id) {
    super(hosts, topic, zkRoot, id);
  }

  public SpoutConfig from(String offset) {
    try {
      Offset o = Offset.valueOf(offset);
      from(o);
    }
    catch(IllegalArgumentException iae) {
      from(Offset.WHERE_I_LEFT_OFF);
    }
    ignoreZkOffsets = true;
    startOffsetTime = kafka.api.OffsetRequest.EarliestTime();
    return this;
  }

  public SpoutConfig from(Offset offset) {
    if(offset == Offset.BEGINNING) {
      ignoreZkOffsets = true;
      startOffsetTime = kafka.api.OffsetRequest.EarliestTime();
    }
    else if(offset == Offset.END) {
      ignoreZkOffsets = true;
      startOffsetTime = kafka.api.OffsetRequest.LatestTime();
    }
    else if(offset == Offset.WHERE_I_LEFT_OFF) {
      ignoreZkOffsets = false;
      startOffsetTime = kafka.api.OffsetRequest.LatestTime();
    }
    return this;
  }
}
