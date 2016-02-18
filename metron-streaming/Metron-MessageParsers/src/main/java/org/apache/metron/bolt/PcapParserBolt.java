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
package org.apache.metron.bolt;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.json.simple.JSONObject;

import java.util.List;

public class PcapParserBolt extends TelemetryParserBolt {

  @Override
  public void declareOther(OutputFieldsDeclarer declarer) {
    declarer.declareStream("raw", new Fields("key", "value", "timestamp") );
  }

  @Override
  public void emitOther(Tuple tuple, List<JSONObject> messages) {
    for(JSONObject message: messages) {
      String key = (String) message.get("pcap_id");
      long timestamp = (long) message.get("ts_micro");
      collector.emit("raw", tuple, new Values(key, tuple.getBinary(0),
              timestamp));
    }
  }
}
