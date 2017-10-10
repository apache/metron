package org.apache.metron.common.zookeeper;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.integration.components.ZKServerComponent;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ZKConfigurationsCacheIntegrationTest {
  private static CuratorFramework client;

  /**
   *
   */
  @Multiline
  public static String testIndexingConfig;

  /**
   *
   */
  @Multiline
  public static String testEnrichmentConfig;

  /**
   *
   */
  @Multiline
  public static String testParserConfig;

  /**
   *
   */
  @Multiline
  public static String globalConfig;

  @BeforeClass
  public static void setupConfiguration() throws Exception {
    ZKServerComponent zkComponent = new ZKServerComponent();
    zkComponent.start();
    client = ConfigurationsUtils.getClient(zkComponent.getConnectionString());
    client.start();
    ConfigurationsUtils.uploadConfigsToZookeeper(TestConstants.SAMPLE_CONFIG_PATH, client);
  }

  @Test
  public void test() {

  }
}
