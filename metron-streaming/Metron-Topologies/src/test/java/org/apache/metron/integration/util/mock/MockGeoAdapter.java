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
