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
package org.apache.metron.enrichment.adapters.geo;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.enrichment.adapters.maxmind.geo.GeoLiteCityDatabase;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoAdapter implements EnrichmentAdapter<CacheKey>, Serializable {
  protected static final Logger _LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void logAccess(CacheKey value) {
  }

  @Override
  public String getOutputPrefix(CacheKey value) {
    return value.getField();
  }

  @Override
  public JSONObject enrich(CacheKey value) {
    JSONObject enriched = new JSONObject();
    Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get(value.coerceValue(String.class));
    if(!result.isPresent()) {
      return new JSONObject();
    }

    enriched = new JSONObject(result.get());
    _LOG.trace("GEO Enrichment success: {}", enriched);
    return enriched;
  }

  @Override
  public boolean initializeAdapter(Map<String, Object> config) {
    GeoLiteCityDatabase.INSTANCE.update((String)config.get(GeoLiteCityDatabase.GEO_HDFS_FILE));
    return true;
  }

  @Override
  public void updateAdapter(Map<String, Object> config) {
    GeoLiteCityDatabase.INSTANCE.updateIfNecessary(config);
  }

  @Override
  public void cleanup() {
  }
}
