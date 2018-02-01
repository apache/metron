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
package org.apache.metron.management;

import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jakewharton.fliptables.FlipTable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.LoggerFactory;

public class EnrichmentConfigFunctions {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public enum Type {
    ENRICHMENT, THREAT_INTEL, THREATINTEL;
  }
  public static Map<String, Object> getStellarHandler(EnrichmentConfig enrichmentConfig) {
    Map<String, Object> fieldMap = enrichmentConfig.getFieldMap();
    Map<String, Object> stellarHandler = (Map<String, Object>) fieldMap.getOrDefault("stellar", new HashMap<>());
    fieldMap.put("stellar", stellarHandler);
    stellarHandler.putIfAbsent("config", new LinkedHashMap<String, Object>());
    return stellarHandler;
  }

  public static EnrichmentConfig getConfig(SensorEnrichmentConfig sensorConfig, Type type) {
      EnrichmentConfig enrichmentConfig = null;
      switch(type) {
        case ENRICHMENT:
          enrichmentConfig = sensorConfig.getEnrichment();
          break;
        case THREAT_INTEL:
        case THREATINTEL:
          enrichmentConfig = sensorConfig.getThreatIntel();
      }
      return enrichmentConfig;
  }

  @Stellar(
           namespace = "ENRICHMENT_STELLAR_TRANSFORM"
          ,name = "PRINT"
          ,description = "Retrieve stellar enrichment transformations."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"type - ENRICHMENT or THREAT_INTEL"
                    }
          ,returns = "The String representation of the transformations"
          )
  public static class GetStellarTransformation implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String config = (String) args.get(0);
      SensorEnrichmentConfig configObj;
      String[] headers = new String[] { "Group", "Field", "Transformation"};
      if(config == null || config.isEmpty()) {
        return FlipTable.of(headers, new String[0][3]);
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }
      Type type = Type.valueOf((String) args.get(1));
      EnrichmentConfig enrichmentConfig = getConfig(configObj, type);

      Map<String, Object> stellarHandler = getStellarHandler(enrichmentConfig);
      Map<String, Object> transforms = (Map<String, Object>) stellarHandler.get("config");
      List<String[]> objs = new ArrayList<>();
      for(Map.Entry<String, Object> kv : transforms.entrySet()) {
        if(kv.getValue() instanceof Map) {
          Map<String, String> groupMap = (Map<String, String>) kv.getValue();
          for(Map.Entry<String, String> groupKv : groupMap.entrySet()) {
            objs.add(new String[]{kv.getKey(), groupKv.getKey(), groupKv.getValue().toString()});
          }
        }
        else {
          objs.add(new String[]{"(default)", kv.getKey(), kv.getValue().toString()});
        }
      }
      String[][] data = new String[objs.size()][3];
      for(int i = 0;i < objs.size();++i) {
        data[i] = objs.get(i);
      }
      return FlipTable.of(headers, data);
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(
           namespace = "ENRICHMENT_STELLAR_TRANSFORM"
          ,name = "ADD"
          ,description = "Add stellar field transformation."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"type - ENRICHMENT or THREAT_INTEL"
                    ,"stellarTransforms - A Map associating fields to stellar expressions"
                    ,"group - Group to add to (optional)"
                    }
          ,returns = "The String representation of the config in zookeeper"
          )
  public static class AddStellarTransformation implements StellarFunction{

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      int i = 0;
      String config = (String) args.get(i++);
      SensorEnrichmentConfig configObj;
      if(config == null || config.isEmpty()) {
        throw new IllegalStateException("Invalid config: " + config);
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }
      Type type = Type.valueOf((String) args.get(i++));
      EnrichmentConfig enrichmentConfig = getConfig(configObj, type);

      Map<String, Object> stellarHandler = getStellarHandler(enrichmentConfig);

      Map<String, String> transformsToAdd = (Map<String, String>) args.get(i++);
      String group = null;
      if(i < args.size() ) {
        group = (String) args.get(i++);
      }
      Map<String, Object> baseTransforms = (Map<String, Object>) stellarHandler.get("config");
      Map<String, Object> groupMap = baseTransforms;
      if(group != null) {
        groupMap = (Map<String, Object>) baseTransforms.getOrDefault(group, new LinkedHashMap<>());
        baseTransforms.put(group, groupMap);
      }
      for(Map.Entry<String, String> kv : transformsToAdd.entrySet()) {
        groupMap.put(kv.getKey(), kv.getValue());
      }
      if(group != null && groupMap.isEmpty()) {
        baseTransforms.remove(group);
      }
      try {
        return JSONUtils.INSTANCE.toJSON(configObj, true);
      } catch (JsonProcessingException e) {
        LOG.error("Unable to convert object to JSON: {}", configObj, e);
        return config;
      }
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(
           namespace = "ENRICHMENT_STELLAR_TRANSFORM"
          ,name = "REMOVE"
          ,description = "Remove one or more stellar field transformations."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"type - ENRICHMENT or THREAT_INTEL"
                    ,"stellarTransforms - A list of removals"
                    ,"group - Group to remove from (optional)"
                    }
          ,returns = "The String representation of the config in zookeeper"
          )
  public static class RemoveStellarTransformation implements StellarFunction{

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      int i = 0;
      String config = (String) args.get(i++);
      SensorEnrichmentConfig configObj;
      if(config == null || config.isEmpty()) {
        throw new IllegalStateException("Invalid config: " + config);
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }
      Type type = Type.valueOf((String) args.get(i++));
      EnrichmentConfig enrichmentConfig = getConfig(configObj, type);

      Map<String, Object> stellarHandler = getStellarHandler(enrichmentConfig);

      List<String> removals = (List<String>) args.get(i++);
      String group = null;
      if(i < args.size() ) {
        group = (String) args.get(i++);
      }
      Map<String, Object> baseTransforms = (Map<String, Object>) stellarHandler.get("config");
      Map<String, Object> groupMap = baseTransforms;
      if(group != null) {
        groupMap = (Map<String, Object>) baseTransforms.getOrDefault(group, new LinkedHashMap<>());
        baseTransforms.put(group, groupMap);
      }
      for(String remove : removals) {
        groupMap.remove(remove);
      }
      if(group != null && groupMap.isEmpty()) {
        baseTransforms.remove(group);
      }
      if(baseTransforms.isEmpty()) {
        enrichmentConfig.getFieldMap().remove("stellar");
      }
      try {
        return JSONUtils.INSTANCE.toJSON(configObj, true);
      } catch (JsonProcessingException e) {
        LOG.error("Unable to convert object to JSON: {}", configObj, e);
        return config;
      }
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }


}
