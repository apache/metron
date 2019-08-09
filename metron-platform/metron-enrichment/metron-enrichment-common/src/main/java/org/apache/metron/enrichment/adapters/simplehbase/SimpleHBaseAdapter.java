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

package org.apache.metron.enrichment.adapters.simplehbase;

import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.EnrichmentLookupFactory;
import org.apache.metron.enrichment.lookup.EnrichmentResult;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class SimpleHBaseAdapter implements EnrichmentAdapter<CacheKey>, Serializable {
  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected SimpleHBaseConfig config;
  protected EnrichmentLookup lookup;

  public SimpleHBaseAdapter() {
  }

  public SimpleHBaseAdapter withLookup(EnrichmentLookup lookup) {
    this.lookup = lookup;
    return this;
  }

  public SimpleHBaseAdapter withConfig(SimpleHBaseConfig config) {
    this.config = config;
    return this;
  }

  @Override
  public void logAccess(CacheKey value) {
  }

  public boolean isInitialized() {
    return lookup != null && lookup.isInitialized();
  }

  @Override
  public JSONObject enrich(CacheKey value) {
    if(!isInitialized()) {
      initializeAdapter(null);
    }

    JSONObject enriched = new JSONObject();
    List<String> enrichmentTypes = getEnrichmentTypes(value);
    if(!isInitialized()) {
      LOG.error("Not initialized, cannot enrich.");

    } else if(isEmpty(enrichmentTypes)) {
      LOG.debug("No enrichments configured for field={}", value.getField());

    } else if(value.getValue() == null) {
      LOG.debug("Enrichment indicator value is unknown, cannot enrich.");

    } else {
      enriched = doEnrich(value);
    }

    return enriched;
  }

  private JSONObject doEnrich(CacheKey value) {
    JSONObject enriched = new JSONObject();
    try {
      Iterable<EnrichmentKey> enrichmentKeys = toEnrichmentKeys(value, value.getConfig().getEnrichment());
      for (EnrichmentResult result: lookup.get(enrichmentKeys)) {
        appendEnrichment(enriched, result);
      }

    } catch (IOException e) {
      String msg = String.format("Unable to lookup enrichments; error=%s", e.getMessage());
      LOG.error(msg, e);
      initializeAdapter(null);
      throw new RuntimeException(msg, e);
    }

    LOG.trace("SimpleHBaseAdapter succeeded: {}", enriched);
    return enriched;
  }

  @Override
  public boolean initializeAdapter(Map<String, Object> configuration) {
    try {
      if(lookup == null) {
        EnrichmentLookupFactory factory = config.getEnrichmentLookupFactory();
        lookup = factory.create(config.getConnectionFactory(), config.getHBaseTable(), config.getHBaseCF(), null);
      }
    } catch (IOException e) {
      LOG.error("Unable to initialize adapter: {}", e.getMessage(), e);
      return false;
    }

    return true;
  }

  @Override
  public void updateAdapter(Map<String, Object> config) {
  }

  @Override
  public void cleanup() {
    try {
      lookup.close();
    } catch (Exception e) {
      LOG.error("Unable to cleanup access tracker", e);
    }
  }

  @Override
  public String getOutputPrefix(CacheKey value) {
    return value.getField();
  }

  private void appendEnrichment(JSONObject enriched, EnrichmentResult result) {
    if(result == null || result.getValue() == null || result.getValue().getMetadata() == null) {
      return; // nothing to do
    }

    EnrichmentValue enrichmentValue = result.getValue();
    for (Map.Entry<String, Object> values : enrichmentValue.getMetadata().entrySet()) {
      String fieldName = result.getKey().getType() + "." + values.getKey();
      enriched.put(fieldName, values.getValue());
    }

    LOG.debug("Found {} enrichment(s) for type={} and indicator={}",
            enrichmentValue.getMetadata().entrySet().size(), result.getKey().getType(), result.getKey().getIndicator());
  }

  private Iterable<EnrichmentKey> toEnrichmentKeys(CacheKey cacheKey, EnrichmentConfig config) {
    List<EnrichmentKey> keys = new ArrayList<>();
    String indicator = cacheKey.coerceValue(String.class);
    List<String> enrichmentTypes = config
            .getFieldToTypeMap()
            .get(EnrichmentUtils.toTopLevelField(cacheKey.getField()));
    for(String enrichmentType: enrichmentTypes) {
      keys.add(new EnrichmentKey(enrichmentType, indicator));
    }

    LOG.debug("Looking for {} enrichment(s) for field={} where indicator={}",
            keys.size(), cacheKey.getField(), indicator);
    return keys;
  }

  private List<String> getEnrichmentTypes(CacheKey value) {
    EnrichmentConfig config = value.getConfig().getEnrichment();
    return config
            .getFieldToTypeMap()
            .get(EnrichmentUtils.toTopLevelField(value.getField()));
  }
}
