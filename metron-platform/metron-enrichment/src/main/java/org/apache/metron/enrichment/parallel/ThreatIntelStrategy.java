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
package org.apache.metron.enrichment.parallel;

import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.enrichment.bolt.ThreatIntelJoinBolt;
import org.apache.metron.enrichment.utils.ThreatIntelUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.json.simple.JSONObject;

import java.util.Map;

/**
 * The enrichment strategy for a threat intel enrichment.
 */
public class ThreatIntelStrategy extends ParallelStrategy {
  protected ThreatIntelStrategy() {}

  @Override
  public Constants.ErrorType getErrorType() {
    return Constants.ErrorType.THREAT_INTEL_ERROR;
  }

  @Override
  public Map<String, Object> enrichmentFieldMap(SensorEnrichmentConfig config) {
    return config.getThreatIntel().getFieldMap();
  }

  @Override
  public Map<String, ConfigHandler> fieldToHandler(SensorEnrichmentConfig config) {
    return config.getThreatIntel().getEnrichmentConfigs();
  }

  @Override
  public String fieldToEnrichmentKey(String type, String field) {
    return ThreatIntelUtils.getThreatIntelKey(type, field);
  }

  /**
   * Make sure to do the triage after-the-fact.
   * @param message
   * @param config
   * @param context
   * @return
   */
  @Override
  public JSONObject postProcess(JSONObject message, SensorEnrichmentConfig config, EnrichmentContext context) {
    return ThreatIntelUtils.triage(message, config, context.getFunctionResolver(), context.getStellarContext());
  }
}
