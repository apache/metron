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

import com.google.common.base.Joiner;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.configuration.enrichment.threatintel.RuleScore;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatScore;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.enrichment.adapters.geo.GeoLiteDatabase;
import org.apache.metron.enrichment.utils.ThreatIntelUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.threatintel.triage.ThreatTriageProcessor;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreatIntelJoinBolt extends EnrichmentJoinBolt {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  /**
   * The Stellar function resolver.
   */
  private FunctionResolver functionResolver;

  /**
   * The execution context for Stellar.
   */
  private Context stellarContext;

  public ThreatIntelJoinBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  @Override
  protected Map<String, ConfigHandler> getFieldToHandlerMap(String sensorType) {
    if(sensorType != null) {
      SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sensorType);
      if (config != null) {
        return config.getThreatIntel().getEnrichmentConfigs();
      } else {
        LOG.info("Unable to retrieve a sensor enrichment config of {}", sensorType);
      }
    } else {
      LOG.error("Trying to retrieve a field map with sensor type of null");
    }
    return new HashMap<>();
  }

  @Override
  public void prepare(Map map, TopologyContext topologyContext) {
    super.prepare(map, topologyContext);
    GeoLiteDatabase.INSTANCE.update((String)getConfigurations().getGlobalConfig().get(GeoLiteDatabase.GEO_HDFS_FILE));
    initializeStellar();
  }

  protected void initializeStellar() {
    this.stellarContext = new Context.Builder()
                                .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
                                .with(Context.Capabilities.GLOBAL_CONFIG, () -> getConfigurations().getGlobalConfig())
                                .with(Context.Capabilities.STELLAR_CONFIG, () -> getConfigurations().getGlobalConfig())
                                .build();
    StellarFunctions.initialize(stellarContext);
    this.functionResolver = StellarFunctions.FUNCTION_RESOLVER();
  }

  @Override
  public Map<String, Object> getFieldMap(String sourceType) {
    SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sourceType);
    if(config != null) {
      return config.getThreatIntel().getFieldMap();
    }
    else {
      LOG.info("Unable to retrieve sensor config: {}", sourceType);
      return null;
    }
  }


  @Override
  public JSONObject joinMessages(Map<String, Tuple> streamMessageMap, MessageGetStrategy messageGetStrategy) {
    JSONObject ret = super.joinMessages(streamMessageMap, messageGetStrategy);
    String sourceType = MessageUtils.getSensorType(ret);
    return ThreatIntelUtils.triage(ret, getConfigurations().getSensorEnrichmentConfig(sourceType), functionResolver, stellarContext);
  }

  @Override
  public void reloadCallback(String name, ConfigurationType type) {
    super.reloadCallback(name, type);
    if(type == ConfigurationType.GLOBAL) {
      GeoLiteDatabase.INSTANCE.updateIfNecessary(getConfigurations().getGlobalConfig());
    }
  }


}
