package org.apache.metron.domain;

import junit.framework.Assert;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.metron.utils.ConfigurationsUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class SensorEnrichmentConfigTest {

  public String sampleConfigRoot = "../Metron-Testing/src/main/resources/sample/config/";

  @Test
  public void test() throws IOException {
    EqualsVerifier.forClass(SensorEnrichmentConfig.class).suppress(Warning.NONFINAL_FIELDS).usingGetClass().verify();
    Map<String, byte[]> testSensorConfigMap = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(sampleConfigRoot);
    byte[] sensorConfigBytes = testSensorConfigMap.get("yaf");
    SensorEnrichmentConfig sensorEnrichmentConfig = SensorEnrichmentConfig.fromBytes(sensorConfigBytes);
    Assert.assertNotNull(sensorEnrichmentConfig);
    Assert.assertTrue(sensorEnrichmentConfig.toString() != null && sensorEnrichmentConfig.toString().length() > 0);
  }
}
