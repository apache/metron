package org.apache.metron.common.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;
import java.util.Map;

public class SensorParserConfig {

  private String parserClassName;
  private String sensorTopic;
  private Map<String, Object> parserConfig;

  public String getParserClassName() {
    return parserClassName;
  }

  public void setParserClassName(String parserClassName) {
    this.parserClassName = parserClassName;
  }

  public String getSensorTopic() {
    return sensorTopic;
  }

  public void setSensorTopic(String sensorTopic) {
    this.sensorTopic = sensorTopic;
  }

  public Map<String, Object> getParserConfig() {
    return parserConfig;
  }

  public void setParserConfig(Map<String, Object> parserConfig) {
    this.parserConfig = parserConfig;
  }

  public static SensorParserConfig fromBytes(byte[] config) throws IOException {
    return JSONUtils.INSTANCE.load(new String(config), SensorParserConfig.class);
  }

  public String toJSON() throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SensorParserConfig that = (SensorParserConfig) o;

    if (getParserClassName() != null ? !getParserClassName().equals(that.getParserClassName()) : that.getParserClassName() != null) return false;
    if (getSensorTopic() != null ? !getSensorTopic().equals(that.getSensorTopic()) : that.getSensorTopic() != null) return false;
    return getParserConfig() != null ? !getParserConfig().equals(that.getParserConfig()) : that.getParserConfig() != null;
  }

  @Override
  public String toString() {
    return "{parserClassName=" + parserClassName + ", sensorTopic=" + sensorTopic +
            ", parserConfig=" + parserConfig + "}";
  }

  @Override
  public int hashCode() {
    int result = getParserClassName() != null ? getParserClassName().hashCode() : 0;
    result = 31 * result + (getSensorTopic() != null ? getSensorTopic().hashCode() : 0);
    result = 31 * result + (getParserConfig() != null ? getParserConfig().hashCode() : 0);
    return result;
  }
}
