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


import com.google.common.collect.Iterables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.enrichment.lookup.accesstracker.NoopAccessTracker;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SimpleHBaseAdapter implements EnrichmentAdapter<CacheKey>,Serializable {
  protected static final Logger _LOG = LoggerFactory.getLogger(SimpleHBaseAdapter.class);
  protected SimpleHBaseConfig config;
  protected EnrichmentLookup lookup;

  public SimpleHBaseAdapter() {
  }
  public SimpleHBaseAdapter(SimpleHBaseConfig config) {
    withConfig(config);
  }

  public SimpleHBaseAdapter withConfig(SimpleHBaseConfig config) {
    this.config = config;
    return this;
  }

  @Override
  public void logAccess(CacheKey value) {
  }


  @Override
  public JSONObject enrich(CacheKey value) {
    JSONObject enriched = new JSONObject();
    List<String> enrichmentTypes = value.getConfig()
                                        .getFieldToEnrichmentTypeMap()
                                        .get(EnrichmentUtils.toTopLevelField(value.getField()));
    if(enrichmentTypes != null && value.getValue() != null) {
      try {
        for (LookupKV<EnrichmentKey, EnrichmentValue> kv :
                lookup.get(Iterables.transform(enrichmentTypes
                                              , new EnrichmentUtils.TypeToKey(value.getValue())
                                              )
                          , lookup.getTable()
                          , false
                          )
            )
        {
          if (kv != null && kv.getValue() != null && kv.getValue().getMetadata() != null) {
            for (Map.Entry<String, String> values : kv.getValue().getMetadata().entrySet()) {
              enriched.put(kv.getKey().type + "." + values.getKey(), values.getValue());
            }
            _LOG.trace("Enriched type " + kv.getKey().type + " => " + enriched);
          }
        }
      }
      catch (IOException e) {
        _LOG.error("Unable to retrieve value: " + e.getMessage(), e);
        throw new RuntimeException("Unable to retrieve value: " + e.getMessage(), e);
      }
    }
    return enriched;
  }

  @Override
  public boolean initializeAdapter() {
    String hbaseTable = config.getHBaseTable();
    Configuration hbaseConfig = HBaseConfiguration.create();
    try {
      lookup = new EnrichmentLookup( config.getProvider().getTable(hbaseConfig, hbaseTable)
                                   , config.getHBaseCF()
                                   , new NoopAccessTracker()
                                   );
    } catch (IOException e) {
      throw new RuntimeException("Unable to initialize adapter: " + e.getMessage(), e);
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
