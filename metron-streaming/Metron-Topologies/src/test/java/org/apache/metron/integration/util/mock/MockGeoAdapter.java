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

import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;

import java.io.Serializable;

public class MockGeoAdapter implements EnrichmentAdapter<String>,
        Serializable {

  @Override
  public void logAccess(String value) {

  }

  public JSONObject enrich(String metadata) {
    JSONObject enriched = new JSONObject();
    enriched.put("locID", "1");
    enriched.put("country", "test country");
    enriched.put("city", "test city");
    enriched.put("postalCode", "test postalCode");
    enriched.put("latitude", "test latitude");
    enriched.put("longitude", "test longitude");
    enriched.put("dmaCode", "test dmaCode");
    enriched.put("location_point", enriched.get("longitude") + "," + enriched.get("latitude"));
    return enriched;
  }

  public boolean initializeAdapter() {
    return true;
  }

  public void cleanup() {

  }
}
