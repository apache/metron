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
package org.apache.metron.enrichment.bolt;

import backtype.storm.task.TopologyContext;
import org.apache.metron.bolt.JoinBolt;
import org.apache.metron.domain.Enrichment;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnrichmentJoinBolt extends JoinBolt<JSONObject> {

  protected static final Logger LOG = LoggerFactory
          .getLogger(EnrichmentJoinBolt.class);

  protected List<Enrichment> enrichments;

  protected String type = "enrichment";
  /**
   * @param enrichments A class for sending tuples to enrichment bolt
   * @return Instance of this class
   */
  public EnrichmentJoinBolt withEnrichments(List<Enrichment> enrichments) {
    this.enrichments = enrichments;
    return this;
  }

  public EnrichmentJoinBolt withType(String type) {
    this.type = type;
    return this;
  }

  @Override
  public void prepare(Map map, TopologyContext topologyContext) {

  }

  @Override
  public Set<String> getStreamIds() {
    Set<String> streamIds = new HashSet<>();
    for(Enrichment enrichment: enrichments) {
      streamIds.add(enrichment.getName());
    }
    return streamIds;
  }


  @Override
  public JSONObject joinValues(Map<String, JSONObject> streamValueMap) {
    JSONObject message = new JSONObject();
    if(streamValueMap.get("message").containsKey("message")) {
      message =  streamValueMap.get("message");
    }
    else {
      message.put("message", streamValueMap.get("message"));
    }
    JSONObject enrichment = new JSONObject();
    for(String streamId: getStreamIds()) {
      enrichment.put(streamId, streamValueMap.get(streamId));
    }
    message.put(type, enrichment);
    return message;
  }
}
