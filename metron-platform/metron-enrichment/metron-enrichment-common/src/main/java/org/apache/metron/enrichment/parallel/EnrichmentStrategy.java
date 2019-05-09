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
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Enrichment strategy.  This interface provides a mechanism to interface with the enrichment config and any
 * post processing steps that are needed to be done after-the-fact.
 *
 * The reasoning behind this is that the key difference between enrichments and threat intel is that they pull
 * their configurations from different parts of the SensorEnrichmentConfig object and as a post-join step, they differ
 * slightly.
 *
 */
public interface EnrichmentStrategy {

  /**
   * Get the underlying configuration for this phase from the sensor enrichment config.
   * @return
   */
  EnrichmentConfig getUnderlyingConfig(SensorEnrichmentConfig config);

  /**
   * Retrieves the error type, so that error messages can be constructed appropriately.
   */
  Constants.ErrorType getErrorType();

  /**
   * Takes the enrichment type and the field and returns a unique key to prefix the output of the enrichment.  For
   * less adaptable enrichments than Stellar, this is important to allow for namespacing in the new fields created.
   * @param type The enrichment type name
   * @param field The input field
   * @return
   */
  String fieldToEnrichmentKey(String type, String field);


  /**
   * Post-process callback after messages are enriched and joined.  By default, this is noop.
   * @param message The input message.
   * @param config The enrichment configuration
   * @param context The enrichment context
   * @return
   */
  default JSONObject postProcess(JSONObject message, SensorEnrichmentConfig config, EnrichmentContext context) {
    return message;
  }
}
