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

import com.google.common.cache.Cache;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.apache.metron.enrichment.utils.ThreatIntelUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.concurrent.Executor;

public enum EnrichmentStrategies implements Strategy {
  ENRICHMENT(new EnrichmentStrategy()),
  THREAT_INTEL( new ThreatIntelStrategy())
  ;

  Strategy strategy;
  EnrichmentStrategies(Strategy strategy) {
    this.strategy = strategy;
  }

  public Map<String, Object> enrichmentFieldMap(SensorEnrichmentConfig config) {
    return strategy.enrichmentFieldMap(config);
  }

  public Map<String, ConfigHandler> fieldToHandler(SensorEnrichmentConfig config) {
    return strategy.fieldToHandler(config);
  }

  public String fieldToEnrichmentKey(String type, String field) {
    return strategy.fieldToEnrichmentKey(type, field);
  }

  public synchronized void initializeThreading(int numThreads, long maxCacheSize, long maxTimeRetain) {
    strategy.initializeThreading(numThreads, maxCacheSize, maxTimeRetain);
  }

  public Executor getExecutor() {
    return strategy.getExecutor();
  }

  public Cache<CacheKey, JSONObject> getCache() {
    return strategy.getCache();
  }


  public JSONObject postProcess(JSONObject message, SensorEnrichmentConfig config, FunctionResolver functionResolver, Context stellarContext)  {
    return strategy.postProcess(message, config, functionResolver, stellarContext);
  }

  @Override
  public Constants.ErrorType getErrorType() {
    return strategy.getErrorType();
  }
}
