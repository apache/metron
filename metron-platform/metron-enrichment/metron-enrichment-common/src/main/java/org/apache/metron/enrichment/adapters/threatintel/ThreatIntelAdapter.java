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
package org.apache.metron.enrichment.adapters.threatintel;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.EnrichmentLookupFactory;
import org.apache.metron.enrichment.lookup.accesstracker.BloomAccessTracker;
import org.apache.metron.enrichment.lookup.accesstracker.PersistentAccessTracker;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class ThreatIntelAdapter implements EnrichmentAdapter<CacheKey>,Serializable {
  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected ThreatIntelConfig config;
  protected EnrichmentLookup lookup;

  public ThreatIntelAdapter() {
  }

  public ThreatIntelAdapter(ThreatIntelConfig config) {
    withConfig(config);
  }

  public ThreatIntelAdapter withConfig(ThreatIntelConfig config) {
    this.config = config;
    return this;
  }

  public ThreatIntelAdapter withLookup(EnrichmentLookup lookup) {
    this.lookup = lookup;
    return this;
  }

  @Override
  public void logAccess(CacheKey value) {
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
      enriched = doEnrich(value, enrichmentTypes);
    }

    return enriched;
  }

  private JSONObject doEnrich(CacheKey value, List<String> enrichmentTypes) {
    JSONObject enriched = new JSONObject();
    Iterable<EnrichmentKey> enrichmentKeys = toEnrichmentKeys(value, value.getConfig().getThreatIntel());
    int i = 0;
    try {
      for (Boolean isThreat : lookup.exists(enrichmentKeys)) {
        String enrichmentType = enrichmentTypes.get(i++);
        if (isThreat) {
          enriched.put(enrichmentType, "alert");
          LOG.trace("Theat Intel Enriched value => {}", enriched);
        }
      }
    } catch(IOException e) {
      String msg = String.format("Unable to lookup enrichments in Threat Intel; error=%s", e.getMessage());
      LOG.error(msg, e);
      initializeAdapter(null);
      throw new RuntimeException(msg, e);
    }
    return enriched;
  }

  public boolean isInitialized() {
    return lookup != null && lookup.isInitialized();
  }

  @Override
  public boolean initializeAdapter(Map<String, Object> configuration) {
    if(lookup == null) {
      String hbaseTable = config.getHBaseTable();
      int expectedInsertions = config.getExpectedInsertions();
      double falsePositives = config.getFalsePositiveRate();
      String trackerHBaseTable = config.getTrackerHBaseTable();
      String trackerHBaseCF = config.getTrackerHBaseCF();
      long millisecondsBetweenPersist = config.getMillisecondsBetweenPersists();
      BloomAccessTracker bat = new BloomAccessTracker(hbaseTable, expectedInsertions, falsePositives);
      Configuration hbaseConfig = HBaseConfiguration.create();
      try {
        HBaseConnectionFactory connectionFactory = config.getConnectionFactory();
        PersistentAccessTracker accessTracker = new PersistentAccessTracker(hbaseTable
                , UUID.randomUUID().toString()
                , trackerHBaseTable
                , trackerHBaseCF
                , bat
                , millisecondsBetweenPersist
                , connectionFactory
                , hbaseConfig);
        EnrichmentLookupFactory lookupFactory = config.getEnrichmentLookupFactory();
        lookup = lookupFactory.create(connectionFactory, hbaseTable, config.getHBaseCF(), accessTracker);

      } catch (IOException e) {
        LOG.error("Unable to initialize adapter", e);
        return false;
      }
    }

    return true;
  }

  @Override
  public void updateAdapter(Map<String, Object> config) {
    // nothing to do
  }

  @Override
  public void cleanup() {
    try {
      lookup.close();
    } catch (Exception e) {
      throw new RuntimeException("Unable to cleanup access tracker", e);
    }
  }

  @Override
  public String getOutputPrefix(CacheKey value) {
    return value.getField();
  }

  private Iterable<EnrichmentKey> toEnrichmentKeys(CacheKey cacheKey, EnrichmentConfig config) {
    List<EnrichmentKey> keys = new ArrayList<>();
    String indicator = cacheKey.coerceValue(String.class);
    List<String> enrichmentTypes = getEnrichmentTypes(cacheKey);
    for(String enrichmentType: enrichmentTypes) {
      keys.add(new EnrichmentKey(enrichmentType, indicator));
    }

    LOG.debug("Looking for {} threat enrichment(s); indicator={}", keys.size(), indicator);
    return keys;
  }

  private List<String> getEnrichmentTypes(CacheKey value) {
    EnrichmentConfig config = value.getConfig().getThreatIntel();
    return config
            .getFieldToTypeMap()
            .get(EnrichmentUtils.toTopLevelField(value.getField()));
  }
}
