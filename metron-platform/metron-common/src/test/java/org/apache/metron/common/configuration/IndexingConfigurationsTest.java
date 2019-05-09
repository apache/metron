package org.apache.metron.common.configuration;

import org.adrianwalker.multilinestring.Multiline;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IndexingConfigurationsTest {

  /**
   * {
   *  "indexing.writer.metronId" : "true"
   * }
   */
  @Multiline
  private static String globalConfig;

  /**
   * {
   *  "writer" : {
   *    "metronId": true
   *  }
   * }
   */
  @Multiline
  private static String sensorConfig;

  private IndexingConfigurations configurations;

  @Before
  public void setup() {
    configurations = new IndexingConfigurations();
  }

  @Test
  public void shouldReturnMetronId() throws Exception {
    // verify false by default
    assertFalse(configurations.isMetronId("sensor", "writer"));

    {
      // verify global config setting applies to any sensor
      configurations.updateGlobalConfig(globalConfig.getBytes(StandardCharsets.UTF_8));

      assertTrue(configurations.isMetronId("sensor", "writer"));
      assertTrue(configurations.isMetronId("anySensor", "writer"));
    }

    {
      // verify sensor config only applies to that sensor
      configurations.updateGlobalConfig(new HashMap<>());
      configurations.updateSensorIndexingConfig("sensor", sensorConfig.getBytes(StandardCharsets.UTF_8));

      assertTrue(configurations.isMetronId("sensor", "writer"));
      assertFalse(configurations.isMetronId("anySensor", "writer"));
    }
  }
}
