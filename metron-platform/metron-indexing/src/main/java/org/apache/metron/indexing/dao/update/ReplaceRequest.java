package org.apache.metron.indexing.dao.update;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class ReplaceRequest {
  Map<String, Object> replacement;
  String uuid;
  String sensorType;

  public String getSensorType() {
    return sensorType;
  }

  public void setSensorType(String sensorType) {
    this.sensorType = sensorType;
  }

  public Map<String, Object> getReplacement() {
    return replacement;
  }

  public void setReplacement(Map<String, Object> replacement) {
    this.replacement = replacement;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
}
