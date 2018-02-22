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
import org.apache.metron.enrichment.utils.EnrichmentUtils;

import java.util.Map;

/**
 * Enrichment strategy which pulls from the enrichment portion of the SensorEnrichmentConfig.
 */
public class EnrichmentStrategy extends ParallelStrategy {
  protected EnrichmentStrategy() {
  }

  @Override
    public Map<String, Object> enrichmentFieldMap(SensorEnrichmentConfig config) {
      return config.getEnrichment().getFieldMap();
    }

    @Override
    public Map<String, ConfigHandler> fieldToHandler(SensorEnrichmentConfig config) {
      return config.getEnrichment().getEnrichmentConfigs();
    }

    @Override
    public String fieldToEnrichmentKey(String type, String field) {
      return EnrichmentUtils.getEnrichmentKey(type, field);
    }

    @Override
    public Constants.ErrorType getErrorType() {
      return Constants.ErrorType.ENRICHMENT_ERROR;
    }
}
