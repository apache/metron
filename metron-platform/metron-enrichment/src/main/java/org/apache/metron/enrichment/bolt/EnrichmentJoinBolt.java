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
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.MessageUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnrichmentJoinBolt extends JoinBolt<JSONObject> {

  protected static final Logger LOG = LoggerFactory
          .getLogger(EnrichmentJoinBolt.class);

  public EnrichmentJoinBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  @Override
  public void prepare(Map map, TopologyContext topologyContext) {

  }

  @Override
  public Set<String> getStreamIds(JSONObject message) {
    Set<String> streamIds = new HashSet<>();
    String sourceType = MessageUtils.getSensorType(message);
    if(sourceType == null) {
      String errorMessage = "Unable to find source type for message: " + message;
      throw new IllegalStateException(errorMessage);
    }
    Map<String, List<String>>  fieldMap = getFieldMap(sourceType);
    if(fieldMap != null) {
      for (String enrichmentType : getFieldMap(sourceType).keySet()) {
        streamIds.add(enrichmentType);
      }
    }
    streamIds.add("message");
    return streamIds;
  }


  @Override
  public JSONObject joinMessages(Map<String, JSONObject> streamMessageMap) {
    JSONObject message = new JSONObject();
    for (String key : streamMessageMap.keySet()) {
      JSONObject obj = streamMessageMap.get(key);
      message.putAll(obj);
    }
    List<Object> emptyKeys = new ArrayList<>();
    for(Object key : message.keySet()) {
      Object value = message.get(key);
      if(value.toString().length() == 0) {
        emptyKeys.add(key);
      }
    }
    for(Object o : emptyKeys) {
      message.remove(o);
    }
    message.put(getClass().getSimpleName().toLowerCase() + ".joiner.ts", "" + System.currentTimeMillis());
    return  message;
  }

  public Map<String, List<String>> getFieldMap(String sourceType) {
    if(sourceType != null) {
      SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sourceType);
      if (config != null && config.getEnrichment() != null) {
        return config.getEnrichment().getFieldMap();
      }
      else {
        LOG.error("Unable to retrieve a sensor enrichment config of " + sourceType);
      }
    }
    else {
      LOG.error("Trying to retrieve a field map with source type of null");
    }
    return null;
  }
}
