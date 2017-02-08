package org.apache.metron.profiler.bolt;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Tests the KafkaDestinationHandler.
 */
public class KafkaDestinationHandlerTest {

  /**
   * {
   *   "profile": "profile-one-destination",
   *   "foreach": "ip_src_addr",
   *   "init":   { "x": "0" },
   *   "update": { "x": "x + 1" },
   *   "result": "x",
   *   "destination": ["hbase"]
   * }
   */
  @Multiline
  private String profileDefinition;

  private KafkaDestinationHandler handler;
  private ProfileConfig profile;
  private ProfileMeasurement measurement;
  private OutputCollector collector;

  @Before
  public void setup() throws Exception {
    handler = new KafkaDestinationHandler();

    profile = createDefinition(profileDefinition);

    measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withValue(22)
            .withDefinition(profile);

    collector = Mockito.mock(OutputCollector.class);
  }

  /**
   * The handler must serialize the ProfileMeasurement into a JSONObject.
   */
  @Test
  public void testJSONSerialization() throws Exception {

    handler.emit(measurement, collector);

    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(collector, times(1)).emit(eq(handler.getStreamId()), arg.capture());

    // expect a JSONObject
    Values values = arg.getValue();
    assertTrue(values.get(0) instanceof JSONObject);

    // validate the json
    JSONObject actual = (JSONObject) values.get(0);
    JSONObject expected = JSONUtils.INSTANCE.toJSONObject(measurement);
    assertEquals(expected, actual);
  }

  /**
   * Creates a profile definition based on a string of JSON.
   * @param json The string of JSON.
   */
  private ProfileConfig createDefinition(String json) throws IOException {
    return JSONUtils.INSTANCE.load(json, ProfileConfig.class);
  }
}
