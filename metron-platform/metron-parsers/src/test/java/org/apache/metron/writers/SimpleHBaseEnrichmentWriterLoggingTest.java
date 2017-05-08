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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    verify(logger).warn("No config object found for key: '" + config.getKey() + "'");
  }

  @Test
  public void shouldWarnOnMissedConfigConversion() {
    getHbaseTableConfig().getAndConvert(Collections.emptyMap(), String.class);
    verify(logger).warn("No object of type '" + String.class + "' found in config");
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
    verify(logger).debug("Transformed message: '" + value + "'");
  }

  @Test
  public void shouldDebugSensorConfig() {
    String sensorName = "someSensor";
    hbaseEnrichmentWriter.configure(sensorName, getMockWriterConfiguration());
    verify(logger).debug("Sensor: '" + sensorName + "': {Provider: '" + MockTableProvider.class.getName() + "', " +
        "Converter: '" + EnrichmentConverter.class.getName() + "'}");
  }

  @Test
  public void shouldDebugFetchedHBaseTable() throws IOException {
    String sensorName = "someSensor";
    String tableName = "someTable";
    hbaseEnrichmentWriter.configure(sensorName, getMockWriterConfiguration());
    hbaseEnrichmentWriter.getTable(tableName, COLUMN_FAMILY);
    verify(logger).debug("Fetching table '" + tableName + "', column family: '" + COLUMN_FAMILY + "'");
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

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(logger, atLeastOnce()).debug(argumentCaptor.capture());

    List<String> capturedLogs = argumentCaptor.getAllValues();
    assertFalse(capturedLogs.isEmpty());
    assertEquals(
        "Put: {" +
            "Column Family: '" + COLUMN_FAMILY + "', " +
            "Key: 'EnrichmentKey{indicator=':', type='testEnrichment'}', " +
            "Value: 'EnrichmentValue{metadata={" + key + "=" + value + "}}'}",
        capturedLogs.get(capturedLogs.size() - 1));
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
