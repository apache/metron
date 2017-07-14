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

package org.apache.metron.writers;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.integration.mock.MockTableProvider;
import org.apache.metron.enrichment.writer.SimpleHbaseEnrichmentWriter;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SimpleHbaseEnrichmentWriter.class, LoggerFactory.class, HBaseConfiguration.class})
public class SimpleHBaseEnrichmentWriterLoggingTest {

  private static Logger logger;

  private static final String COLUMN_FAMILY = "cf1";

  private SimpleHbaseEnrichmentWriter hbaseEnrichmentWriter;

  @BeforeClass
  public static void init() {
    mockStatic(LoggerFactory.class);
    logger = mock(Logger.class);
    when(LoggerFactory.getLogger(SimpleHbaseEnrichmentWriter.class)).thenReturn(logger);
  }

  @Before
  public void setUp() {
    hbaseEnrichmentWriter = new SimpleHbaseEnrichmentWriter();
    mockStatic(HBaseConfiguration.class);
    Configuration mockConfig = mock(Configuration.class);
    when(mockConfig.getInt(eq("hbase.column.max.version"), anyInt())).thenReturn(1);
    when(HBaseConfiguration.create()).thenReturn(mockConfig);
  }

  @Test
  public void shouldWarnOnMissingConfig() {
    SimpleHbaseEnrichmentWriter.Configurations config = getHbaseTableConfig();
    config.get(Collections.emptyMap());
    verify(logger).warn("No config object found for key: '{}'", config.getKey());
  }

  @Test
  public void shouldWarnOnMissedConfigConversion() {
    getHbaseTableConfig().getAndConvert(Collections.emptyMap(), String.class);
    verify(logger).warn("No object of type '{}' found in config", String.class);
  }

  @Test
  public void shouldDebugTransformedMessage() {

    // An expected property
    String key = "message";
    String value = "Hello World!";

    // A received message bearing the property
    SimpleHbaseEnrichmentWriter.KeyTransformer keyTransformer = new SimpleHbaseEnrichmentWriter.KeyTransformer(key);
    JSONObject message = newJSONObject(key, value);

    // Message transformation
    keyTransformer.transform(message);

    // Proof the result has been captured
    verify(logger).debug("Transformed message: '{}'", value);
  }

  @Test
  public void shouldDebugSensorConfig() {
    String sensorName = "someSensor";
    hbaseEnrichmentWriter.configure(sensorName, getMockWriterConfiguration());
    verify(logger).debug("Sensor: '{}': {Provider: '{}', Converter: '{}'}",
        sensorName, MockTableProvider.class.getName(), EnrichmentConverter.class.getName());
  }

  @Test
  public void shouldDebugFetchedHBaseTable() throws IOException {
    String sensorName = "someSensor";
    String tableName = "someTable";
    hbaseEnrichmentWriter.configure(sensorName, getMockWriterConfiguration());
    hbaseEnrichmentWriter.getTable(tableName, COLUMN_FAMILY);
    verify(logger).debug("Fetching table '{}', column family: '{}'", tableName, COLUMN_FAMILY);
  }

  @Test
  public void shouldDebugWriteOperation() throws Exception {
    String sensorName = "someSensor";
    String key = "testKey";
    String value = "testValue";
    WriterConfiguration mockWriterConfiguration = getMockWriterConfiguration();
    hbaseEnrichmentWriter.configure(sensorName, mockWriterConfiguration);
    hbaseEnrichmentWriter.write(sensorName,
        mockWriterConfiguration,
        Collections.emptyList(),
        Collections.singletonList(newJSONObject(key, value)));

    ArgumentCaptor<String> logTemplateCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> logArgsCaptor = ArgumentCaptor.forClass(Object.class);
    verify(logger, atLeastOnce()).debug(logTemplateCaptor.capture(),
        logArgsCaptor.capture(), logArgsCaptor.capture(), logArgsCaptor.capture());

    assertEquals("Put: {Column Family: '{}', Key: '{}', Value: '{}'}", logTemplateCaptor.getValue());

    List<Object> logArgs = logArgsCaptor.getAllValues();
    assertEquals(COLUMN_FAMILY, assertAndGetArg(logArgs, 3));
    assertEquals("EnrichmentKey{indicator=':', type='testEnrichment'}", assertAndGetArg(logArgs, 2).toString());
    assertEquals("EnrichmentValue{metadata={testKey=testValue}}", assertAndGetArg(logArgs, 1).toString());
  }

  private <T> T assertAndGetArg(List<Object> capturedArgs, int index) {
    assertTrue(capturedArgs.size() - index >= 0);
    T result = (T) capturedArgs.get(capturedArgs.size() - index);
    assertNotNull(result);
    return result;
  }

  private JSONObject newJSONObject(String key, String value) {
    return new JSONObject(Collections.singletonMap(key, value));
  }

  private SimpleHbaseEnrichmentWriter.Configurations getHbaseTableConfig() {
    return SimpleHbaseEnrichmentWriter.Configurations.HBASE_TABLE;
  }

  private WriterConfiguration getMockWriterConfiguration() {
    WriterConfiguration writerConfiguration = mock(WriterConfiguration.class);
    Map<String, Object> configMap = new HashMap<>();

    String tableName = SimpleHBaseEnrichmentWriterLoggingTest.class.getSimpleName();
    MockTableProvider.addTable(tableName, COLUMN_FAMILY);

    configMap.put(SimpleHbaseEnrichmentWriter.Configurations.HBASE_PROVIDER.getKey(), MockTableProvider.class);
    configMap.put(SimpleHbaseEnrichmentWriter.Configurations.KEY_COLUMNS.getKey(), Arrays.asList("col1", "col2"));
    configMap.put(SimpleHbaseEnrichmentWriter.Configurations.HBASE_TABLE.getKey(), tableName);
    configMap.put(SimpleHbaseEnrichmentWriter.Configurations.HBASE_CF.getKey(), COLUMN_FAMILY);
    configMap.put(SimpleHbaseEnrichmentWriter.Configurations.ENRICHMENT_TYPE.getKey(), "testEnrichment");
    when(writerConfiguration.getSensorConfig(anyString())).thenReturn(configMap);
    return writerConfiguration;
  }
}
