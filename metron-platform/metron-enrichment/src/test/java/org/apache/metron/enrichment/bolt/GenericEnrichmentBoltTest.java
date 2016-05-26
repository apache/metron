/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.enrichment.bolt;

import backtype.storm.tuple.Values;
import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.TestConstants;
import org.apache.metron.test.bolt.BaseEnrichmentBoltTest;
import org.apache.metron.enrichment.configuration.Enrichment;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.hamcrest.Description;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GenericEnrichmentBoltTest extends BaseEnrichmentBoltTest {

  protected class EnrichedMessageMatcher extends ArgumentMatcher<Values> {

    private String expectedKey;
    private JSONObject expectedMessage;

    public EnrichedMessageMatcher(String expectedKey, JSONObject expectedMessage) {
      this.expectedKey = expectedKey;
      this.expectedMessage = expectedMessage;
    }

    @Override
    public boolean matches(Object o) {
      Values values = (Values) o;
      String actualKey = (String) values.get(0);
      JSONObject actualMessage = (JSONObject) values.get(1);
      removeTimingFields(actualMessage);
      return expectedKey.equals(actualKey) && expectedMessage.equals(actualMessage);
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(String.format("[%s]", expectedMessage));
    }

  }

  /**
   {
   "field1": "value1",
   "field2": "value2",
   "source.type": "test"
   }
   */
  @Multiline
  private String originalMessageString;

  /**
   {
   "enrichedField1": "enrichedValue1"
   }
   */
  @Multiline
  private String enrichedField1String;

  /**
   {
   "enrichedField2": "enrichedValue2"
   }
   */
  @Multiline
  private String enrichedField2String;

  /**
   {
   "field1.enrichedField1": "enrichedValue1",
   "field2.enrichedField2": "enrichedValue2",
   "source.type": "test"
   }
   */
  @Multiline
  private String enrichedMessageString;

  private JSONObject originalMessage;
  private JSONObject enrichedField1;
  private JSONObject enrichedField2;
  private JSONObject enrichedMessage;

  @Before
  public void parseMessages() throws ParseException {
    JSONParser parser = new JSONParser();
    originalMessage = (JSONObject) parser.parse(originalMessageString);
    enrichedField1 = (JSONObject) parser.parse(enrichedField1String);
    enrichedField2 = (JSONObject) parser.parse(enrichedField2String);
    enrichedMessage = (JSONObject) parser.parse(enrichedMessageString);
  }

  @Mock
  public EnrichmentAdapter<CacheKey> enrichmentAdapter;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void test() throws IOException {
    String key = "someKey";
    String enrichmentType = "enrichmentType";
    Enrichment<EnrichmentAdapter<CacheKey>> testEnrichment = new Enrichment<>();
    testEnrichment.setType(enrichmentType);
    testEnrichment.setAdapter(enrichmentAdapter);
    GenericEnrichmentBolt genericEnrichmentBolt = new GenericEnrichmentBolt("zookeeperUrl");
    genericEnrichmentBolt.setCuratorFramework(client);
    genericEnrichmentBolt.setTreeCache(cache);
    genericEnrichmentBolt.getConfigurations().updateSensorEnrichmentConfig(sensorType, new FileInputStream(sampleSensorEnrichmentConfigPath));
    try {
      genericEnrichmentBolt.prepare(new HashMap(), topologyContext, outputCollector);
      fail("Should fail if a maxCacheSize property is not set");
    } catch(IllegalStateException e) {}
    genericEnrichmentBolt.withMaxCacheSize(100);
    try {
      genericEnrichmentBolt.prepare(new HashMap(), topologyContext, outputCollector);
      fail("Should fail if a maxTimeRetain property is not set");
    } catch(IllegalStateException e) {}
    genericEnrichmentBolt.withMaxTimeRetain(10000);
    try {
      genericEnrichmentBolt.prepare(new HashMap(), topologyContext, outputCollector);
      fail("Should fail if an adapter is not set");
    } catch(IllegalStateException e) {}
    genericEnrichmentBolt.withEnrichment(testEnrichment);
    when(enrichmentAdapter.initializeAdapter()).thenReturn(true);
    genericEnrichmentBolt.prepare(new HashMap(), topologyContext, outputCollector);
    verify(enrichmentAdapter, times(1)).initializeAdapter();
    when(enrichmentAdapter.initializeAdapter()).thenReturn(false);
    try {
      genericEnrichmentBolt.prepare(new HashMap(), topologyContext, outputCollector);
      fail("An exception should be thrown if enrichment adapter initialization fails");
    } catch(IllegalStateException e) {}
    genericEnrichmentBolt.declareOutputFields(declarer);
    verify(declarer, times(1)).declareStream(eq(enrichmentType), argThat(new FieldsMatcher("key", "message")));
    verify(declarer, times(1)).declareStream(eq("error"), argThat(new FieldsMatcher("message")));
    when(tuple.getStringByField("key")).thenReturn(null);
    genericEnrichmentBolt.execute(tuple);
    verify(outputCollector, times(1)).emit(eq("error"), any(Values.class));
    when(tuple.getStringByField("key")).thenReturn(key);
    when(tuple.getValueByField("message")).thenReturn(originalMessage);
    genericEnrichmentBolt.execute(tuple);
    verify(outputCollector, times(1)).emit(eq(enrichmentType), argThat(new EnrichedMessageMatcher(key, new JSONObject(ImmutableMap.of("source.type", "test")))));
    reset(enrichmentAdapter);

    SensorEnrichmentConfig sensorEnrichmentConfig = SensorEnrichmentConfig.
            fromBytes(ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(TestConstants.SAMPLE_CONFIG_PATH).get(sensorType));
    CacheKey cacheKey1 = new CacheKey("field1", "value1", sensorEnrichmentConfig);
    CacheKey cacheKey2 = new CacheKey("field2", "value2", sensorEnrichmentConfig);
    when(enrichmentAdapter.enrich(cacheKey1)).thenReturn(enrichedField1);
    when(enrichmentAdapter.enrich(cacheKey2)).thenReturn(enrichedField2);
    genericEnrichmentBolt.execute(tuple);
    verify(enrichmentAdapter, times(1)).logAccess(cacheKey1);
    verify(enrichmentAdapter, times(1)).logAccess(cacheKey2);
    verify(outputCollector, times(1)).emit(eq(enrichmentType), argThat(new EnrichedMessageMatcher(key, enrichedMessage)));


  }
}
