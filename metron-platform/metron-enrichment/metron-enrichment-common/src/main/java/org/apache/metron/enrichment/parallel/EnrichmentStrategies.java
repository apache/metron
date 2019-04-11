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
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.apache.metron.enrichment.utils.ThreatIntelUtils;
import org.json.simple.JSONObject;

/**
 * The specific strategies to interact with the sensor enrichment config.
 * The approach presented here, in contrast to the inheritance-based approach
 * in the bolts, allows for an abstraction through composition whereby we
 * localize all the interactions with the sensor enrichment config in a strategy
 * rather than bind the abstraction to Storm, our distributed processing engine.
 */
public enum EnrichmentStrategies implements EnrichmentStrategy {
  /**
   * Interact with the enrichment portion of the enrichment config
   */
  ENRICHMENT(new EnrichmentStrategy() {
    @Override
    public EnrichmentConfig getUnderlyingConfig(SensorEnrichmentConfig config) {
      return config.getEnrichment();
    }

    @Override
    public Constants.ErrorType getErrorType() {
      return Constants.ErrorType.ENRICHMENT_ERROR;
    }

    @Override
    public String fieldToEnrichmentKey(String type, String field) {
      return EnrichmentUtils.getEnrichmentKey(type, field);
    }
  }),
  /**
   * Interact with the threat intel portion of the enrichment config.
   */
  THREAT_INTEL(new EnrichmentStrategy() {
    @Override
    public EnrichmentConfig getUnderlyingConfig(SensorEnrichmentConfig config) {
      return config.getThreatIntel();
    }

    @Override
    public Constants.ErrorType getErrorType() {
      return Constants.ErrorType.THREAT_INTEL_ERROR;
    }

    @Override
    public String fieldToEnrichmentKey(String type, String field) {
      return ThreatIntelUtils.getThreatIntelKey(type, field);
    }

    @Override
    public JSONObject postProcess(JSONObject message, SensorEnrichmentConfig config, EnrichmentContext context) {
      return ThreatIntelUtils.triage(message, config, context.getFunctionResolver(), context.getStellarContext());
    }
  })
  ;

  EnrichmentStrategy enrichmentStrategy;
  EnrichmentStrategies(EnrichmentStrategy enrichmentStrategy) {
    this.enrichmentStrategy = enrichmentStrategy;
  }

  /**
   * Get the underlying enrichment config.  If this is provided, then we need not retrieve
   * @return
   */
  @Override
  public EnrichmentConfig getUnderlyingConfig(SensorEnrichmentConfig config) {
    return enrichmentStrategy.getUnderlyingConfig(config);
  }

  public String fieldToEnrichmentKey(String type, String field) {
    return enrichmentStrategy.fieldToEnrichmentKey(type, field);
  }


  public JSONObject postProcess(JSONObject message, SensorEnrichmentConfig config, EnrichmentContext context)  {
    return enrichmentStrategy.postProcess(message, config, context);
  }

  @Override
  public Constants.ErrorType getErrorType() {
    return enrichmentStrategy.getErrorType();
  }

}
