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

import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.storm.task.TopologyContext;
import com.google.common.base.Joiner;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    Map<String, Object>  fieldMap = getFieldMap(sourceType);
    Map<String, ConfigHandler> handlerMap = getFieldToHandlerMap(sourceType);
    if(fieldMap != null) {
      for (String enrichmentType : fieldMap.keySet()) {
        ConfigHandler handler = handlerMap.get(enrichmentType);
        List<String> subgroups = handler.getType().getSubgroups(handler.getType().toConfig(handler.getConfig()));
        for(String subgroup : subgroups) {
          streamIds.add(Joiner.on(":").join(enrichmentType, subgroup));
        }
      }
    }
    streamIds.add("message:");
    return streamIds;
  }


  @Override
  public JSONObject joinMessages(Map<String, Tuple> streamMessageMap, MessageGetStrategy messageGetStrategy) {
    JSONObject message = new JSONObject();
    for (String key : streamMessageMap.keySet()) {
      Tuple tuple = streamMessageMap.get(key);
      JSONObject obj = (JSONObject) messageGetStrategy.get(tuple);
      message.putAll(obj);
    }
    List<Object> emptyKeys = new ArrayList<>();
    for(Object key : message.keySet()) {
      Object value = message.get(key);
      if(value == null || value.toString().length() == 0) {
        emptyKeys.add(key);
      }
    }
    for(Object o : emptyKeys) {
      message.remove(o);
    }
    message.put(getClass().getSimpleName().toLowerCase() + ".joiner.ts", "" + System.currentTimeMillis());
    return  message;
  }

 protected Map<String, ConfigHandler> getFieldToHandlerMap(String sensorType) {
    if(sensorType != null) {
      SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sensorType);
      if (config != null) {
        return config.getEnrichment().getEnrichmentConfigs();
      } else {
        LOG.info("Unable to retrieve a sensor enrichment config of {}", sensorType);
      }
    } else {
      LOG.error("Trying to retrieve a field map with sensor type of null");
    }
    return new HashMap<>();
  }

  public Map<String, Object> getFieldMap(String sourceType) {
    if(sourceType != null) {
      SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sourceType);
      if (config != null && config.getEnrichment() != null) {
        return config.getEnrichment().getFieldMap();
      }
      else {
        LOG.info("Unable to retrieve a sensor enrichment config of {}", sourceType);
      }
    }
    else {
      LOG.error("Trying to retrieve a field map with source type of null");
    }
    return null;
  }
}
