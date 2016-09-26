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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.jakewharton.fliptables.FlipTable;
import org.apache.log4j.Logger;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatIntelConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;
import static org.apache.metron.management.EnrichmentConfigFunctions.getConfig;

public class ThreatTriageFunctions {
  private static final Logger LOG = Logger.getLogger(ConfigurationFunctions.class);

  @Stellar(
           namespace = "THREAT_TRIAGE"
          ,name = "PRINT"
          ,description = "Retrieve stellar enrichment transformations."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    }
          ,returns = "The String representation of the threat triage rules"
          )
  public static class GetStellarTransformation implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String config = (String) args.get(0);
      SensorEnrichmentConfig configObj;
      if(config == null || config.isEmpty()) {
        configObj = new SensorEnrichmentConfig();
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }
      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(configObj, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        return "";
      }
      ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        return "";
      }
      Map<String, Number> triageRules = triageConfig.getRiskLevelRules();
      if(triageRules == null) {
        triageRules = new LinkedHashMap<>();
      }
      String[] headers = new String[] {"Triage Rule", "Score"};
      String[][] data = new String[triageRules.size()][2];
      int i = 0;
      for(Map.Entry<String, Number> kv : triageRules.entrySet()) {
        double d = kv.getValue().doubleValue();
        String val = d == (long)d ? String.format("%d", (long)d) : String.format("%s", d);
        data[i++]  = new String[] {kv.getKey(), val};
      }
      String ret = FlipTable.of(headers, data);
      if(!triageRules.isEmpty()) {
        ret += "\n\n";

        ret += "Aggregation: " + triageConfig.getAggregator().name();
      }
      return ret;
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
           namespace = "THREAT_TRIAGE"
          ,name = "ADD"
          ,description = "Add a threat triage rule."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"stellarTransforms - A Map associating stellar rules to scores"
                    }
          ,returns = "The String representation of the threat triage rules"
          )
  public static class AddStellarTransformation implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String config = (String) args.get(0);
      SensorEnrichmentConfig configObj;
      if(config == null || config.isEmpty()) {
        configObj = new SensorEnrichmentConfig();
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }
      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(configObj, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        tiConfig = new ThreatIntelConfig();
        configObj.setThreatIntel(tiConfig);
      }
      ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        triageConfig = new ThreatTriageConfig();
        tiConfig.setTriageConfig(triageConfig);
      }
      Map<String, Number> triageRules = triageConfig.getRiskLevelRules();
      if(triageRules == null) {
        triageRules = new LinkedHashMap<>();
        triageConfig.setRiskLevelRules(triageRules);
      }
      Map<String, Object> newRules = (Map<String, Object>) args.get(1);
      for(Map.Entry<String, Object> kv : newRules.entrySet()) {
        if(kv.getKey() == null || kv.getKey().equals("null")) {
          continue;
        }
        Double ret = ConversionUtils.convert(kv.getValue(), Double.class);
        triageConfig.getRiskLevelRules().put(kv.getKey(), ret);
      }
      try {
        return JSONUtils.INSTANCE.toJSON(configObj, true);
      } catch (JsonProcessingException e) {
        LOG.error("Unable to convert object to JSON: " + configObj, e);
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
           namespace = "THREAT_TRIAGE"
          ,name = "REMOVE"
          ,description = "Remove stellar threat triage rule(s)."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"stellarTransforms - A list of stellar rules to remove"
                    }
          ,returns = "The String representation of the enrichment config"
          )
  public static class RemoveStellarTransformation implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String config = (String) args.get(0);
      SensorEnrichmentConfig configObj;
      if(config == null || config.isEmpty()) {
        configObj = new SensorEnrichmentConfig();
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }
      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(configObj, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        tiConfig = new ThreatIntelConfig();
        configObj.setThreatIntel(tiConfig);
      }
      ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        triageConfig = new ThreatTriageConfig();
        tiConfig.setTriageConfig(triageConfig);
      }
      Map<String, Number> triageRules = triageConfig.getRiskLevelRules();
      if(triageRules == null) {
        triageRules = new LinkedHashMap<>();
        triageConfig.setRiskLevelRules(triageRules);
      }
      List<String> rulesToRemove = (List<String>) args.get(1);
      for(String rule : rulesToRemove) {
        triageConfig.getRiskLevelRules().remove(rule);
      }
      try {
        return JSONUtils.INSTANCE.toJSON(configObj, true);
      } catch (JsonProcessingException e) {
        LOG.error("Unable to convert object to JSON: " + configObj, e);
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
           namespace = "THREAT_TRIAGE"
          ,name = "SET_AGGREGATOR"
          ,description = "Set the threat triage aggregator."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"aggregator - Aggregator to use.  One of MIN, MAX, MEAN, SUM, POSITIVE_MEAN"
                    ,"aggregatorConfig - Optional config for aggregator"
                    }
          ,returns = "The String representation of the enrichment config"
          )
  public static class SetAggregator implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String config = (String) args.get(0);

      SensorEnrichmentConfig configObj;
      if(config == null || config.isEmpty()) {
        configObj = new SensorEnrichmentConfig();
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }

      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(configObj, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        tiConfig = new ThreatIntelConfig();
        configObj.setThreatIntel(tiConfig);
      }
      ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        triageConfig = new ThreatTriageConfig();
        tiConfig.setTriageConfig(triageConfig);
      }
      Map<String, Number> triageRules = triageConfig.getRiskLevelRules();
      if(triageRules == null) {
        triageRules = new LinkedHashMap<>();
        triageConfig.setRiskLevelRules(triageRules);
      }
      String aggregator = (String) args.get(1);
      triageConfig.setAggregator(aggregator);
      if(args.size() > 2) {
        Map<String, Object> aggConfig = (Map<String, Object>) args.get(2);
        triageConfig.setAggregationConfig(aggConfig);
      }
      try {
        return JSONUtils.INSTANCE.toJSON(configObj, true);
      } catch (JsonProcessingException e) {
        LOG.error("Unable to convert object to JSON: " + configObj, e);
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
