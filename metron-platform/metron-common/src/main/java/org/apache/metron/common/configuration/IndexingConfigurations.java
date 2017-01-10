package org.apache.metron.common.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.metron.common.utils.JSONUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class IndexingConfigurations extends Configurations {
  public Map<String, Object> getSensorIndexingConfig(String sensorType) {
    return (Map<String, Object>) configurations.get(getKey(sensorType));
  }

  public void updateSensorIndexingConfig(String sensorType, byte[] data) throws IOException {
    updateSensorIndexingConfig(sensorType, new ByteArrayInputStream(data));
  }

  public void updateSensorIndexingConfig(String sensorType, InputStream io) throws IOException {
    Map<String, Object> sensorIndexingConfig = JSONUtils.INSTANCE.load(io, new TypeReference<Map<String, Object>>() {
    });
    updateSensorIndexingConfig(sensorType, sensorIndexingConfig);
  }

  public void updateSensorIndexingConfig(String sensorType, Map<String, Object> sensorIndexingConfig) {
    configurations.put(getKey(sensorType), sensorIndexingConfig);
  }

  private String getKey(String sensorType) {
    return ConfigurationType.INDEXING.getName() + "." + sensorType;
  }
}
