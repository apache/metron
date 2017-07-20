package org.apache.metron.indexing.dao.update;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class PatchRequest {
  JsonNode patch;
  Map<String, Object> source;
  String uuid;
  String sensorType;

  public JsonNode getPatch() {
    return patch;
  }

  public void setPatch(JsonNode patch) {
    this.patch = patch;
  }

  public Map<String, Object> getSource() {
    return source;
  }

  public void setSource(Map<String, Object> source) {
    this.source = source;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getSensorType() {
    return sensorType;
  }

  public void setSensorType(String sensorType) {
    this.sensorType = sensorType;
  }
}
