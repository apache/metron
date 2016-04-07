package org.apache.metron.bolt;

import org.apache.curator.test.TestingServer;
import org.apache.metron.Constants;
import org.apache.metron.utils.ConfigurationsUtils;
import org.junit.Before;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class BaseBoltTest {

  public String sampleConfigRoot = "../Metron-Testing/src/main/resources/sample/config/";
  protected String zookeeperUrl;
  protected Set<String> allConfigurationTypes = new HashSet<>();

  @Before
  public void setupConfiguration() throws Exception {
    TestingServer testZkServer = new TestingServer(true);
    this.zookeeperUrl = testZkServer.getConnectString();
    byte[] globalConfig = ConfigurationsUtils.readGlobalConfigFromFile(sampleConfigRoot);
    ConfigurationsUtils.writeGlobalConfigToZookeeper(globalConfig, zookeeperUrl);
    allConfigurationTypes.add(Constants.GLOBAL_CONFIG_NAME);
    Map<String, byte[]> sensorEnrichmentConfigs = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(sampleConfigRoot);
    for (String sensorType : sensorEnrichmentConfigs.keySet()) {
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, sensorEnrichmentConfigs.get(sensorType), zookeeperUrl);
      allConfigurationTypes.add(sensorType);
    }
  }
}
