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
package org.apache.metron.integration.util.mock;

import com.google.common.base.Joiner;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;

import java.io.Serializable;

public class MockGeoAdapter implements EnrichmentAdapter<CacheKey>,
        Serializable {

  public static final String DEFAULT_LOC_ID = "1";
  public static final String DEFAULT_COUNTRY = "test country";
  public static final String DEFAULT_CITY = "test city";
  public static final String DEFAULT_POSTAL_CODE = "test postalCode";
  public static final String DEFAULT_LATITUDE = "test latitude";
  public static final String DEFAULT_LONGITUDE = "test longitude";
  public static final String DEFAULT_DMACODE= "test dmaCode";
  public static final String DEFAULT_LOCATION_POINT= Joiner.on(',').join(DEFAULT_LONGITUDE, DEFAULT_LATITUDE);

  @Override
  public void logAccess(CacheKey value) {

  }

  public JSONObject enrich(CacheKey cache ) {
    JSONObject enriched = new JSONObject();
    enriched.put("locID", DEFAULT_LOC_ID);
    enriched.put("country", DEFAULT_COUNTRY);
    enriched.put("city", DEFAULT_CITY);
    enriched.put("postalCode", DEFAULT_POSTAL_CODE);
    enriched.put("latitude", DEFAULT_LATITUDE);
    enriched.put("longitude", DEFAULT_LONGITUDE);
    enriched.put("dmaCode", DEFAULT_DMACODE);
    enriched.put("location_point", DEFAULT_LOCATION_POINT);
    return enriched;
  }

  public boolean initializeAdapter() {
    return true;
  }

  public void cleanup() {

  }
}
