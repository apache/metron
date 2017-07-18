package org.apache.metron.indexing.dao;

import java.util.HashMap;
import java.util.Map;

public class AccessConfig {
  private Integer maxSearchResults;
  private Map<String, String> optionalSettings = new HashMap<>();

  public Integer getMaxSearchResults() {
    return maxSearchResults;
  }

  public void setMaxSearchResults(Integer maxSearchResults) {
    this.maxSearchResults = maxSearchResults;
  }

  public Map<String, String> getOptionalSettings() {
    return optionalSettings;
  }

  public void setOptionalSettings(Map<String, String> optionalSettings) {
    this.optionalSettings = optionalSettings;
  }
}
