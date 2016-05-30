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

import com.google.common.collect.Iterables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.accesstracker.BloomAccessTracker;
import org.apache.metron.enrichment.lookup.accesstracker.PersistentAccessTracker;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class ThreatIntelAdapter implements EnrichmentAdapter<CacheKey>,Serializable {
  protected static final Logger _LOG = LoggerFactory.getLogger(ThreatIntelAdapter.class);
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

  @Override
  public void logAccess(CacheKey value) {
    List<String> enrichmentTypes = value.getConfig().getThreatIntel().getFieldToTypeMap().get(value.getField());
    if(enrichmentTypes != null) {
      for(String enrichmentType : enrichmentTypes) {
        lookup.getAccessTracker().logAccess(new EnrichmentKey(enrichmentType, value.getValue()));
      }
    }
  }


  @Override
  public JSONObject enrich(CacheKey value) {
    if(!isInitialized()) {
      initializeAdapter();
    }
    JSONObject enriched = new JSONObject();
    List<String> enrichmentTypes = value.getConfig()
                                        .getThreatIntel().getFieldToTypeMap()
                                        .get(EnrichmentUtils.toTopLevelField(value.getField()));
    if(isInitialized() && enrichmentTypes != null) {
      int i = 0;
      try {
        for (Boolean isThreat :
                lookup.exists(Iterables.transform(enrichmentTypes
                                                 , new EnrichmentUtils.TypeToKey(value.getValue()
                                                                                , lookup.getTable()
                                                                                , value.getConfig().getThreatIntel()
                                                                                )
                                                 )
                             , false
                             )
            )
        {
          String enrichmentType = enrichmentTypes.get(i++);
          if (isThreat) {
            enriched.put(enrichmentType, "alert");
            _LOG.trace("Enriched value => " + enriched);
          }
        }
      }
      catch(IOException e) {
        _LOG.error("Unable to retrieve value: " + e.getMessage(), e);
        initializeAdapter();
        throw new RuntimeException("Unable to retrieve value", e);
      }
    }
    return enriched;
  }

  public boolean isInitialized() {
    return lookup != null && lookup.getTable() != null;
  }

  @Override
  public boolean initializeAdapter() {
    PersistentAccessTracker accessTracker;
    String hbaseTable = config.getHBaseTable();
    int expectedInsertions = config.getExpectedInsertions();
    double falsePositives = config.getFalsePositiveRate();
    String trackerHBaseTable = config.getTrackerHBaseTable();
    String trackerHBaseCF = config.getTrackerHBaseCF();
    long millisecondsBetweenPersist = config.getMillisecondsBetweenPersists();
    BloomAccessTracker bat = new BloomAccessTracker(hbaseTable, expectedInsertions, falsePositives);
    Configuration hbaseConfig = HBaseConfiguration.create();
    try {
      accessTracker = new PersistentAccessTracker( hbaseTable
              , UUID.randomUUID().toString()
              , config.getProvider().getTable(hbaseConfig, trackerHBaseTable)
              , trackerHBaseCF
              , bat
              , millisecondsBetweenPersist
      );
      lookup = new EnrichmentLookup(config.getProvider().getTable(hbaseConfig, hbaseTable), config.getHBaseCF(), accessTracker);
    } catch (IOException e) {
      _LOG.error("Unable to initialize ThreatIntelAdapter", e);
      return false;
    }

    return true;
  }

  @Override
  public void cleanup() {
    try {
      lookup.close();
    } catch (Exception e) {
      throw new RuntimeException("Unable to cleanup access tracker", e);
    }
  }
}
