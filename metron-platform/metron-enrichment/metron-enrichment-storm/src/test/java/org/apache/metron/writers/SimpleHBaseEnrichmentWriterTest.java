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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.enrichment.writer.SimpleHbaseEnrichmentWriter;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class SimpleHBaseEnrichmentWriterTest {
  private static final String SENSOR_TYPE= "dummy";
  private static final String TABLE_NAME= SENSOR_TYPE;
  private static final String TABLE_CF= "cf";
  private static final String ENRICHMENT_TYPE = "et";
  private static final Map<String, Object> BASE_WRITER_CONFIG = new HashMap<String, Object>() {{
    put(SimpleHbaseEnrichmentWriter.Configurations.HBASE_TABLE.getKey(), TABLE_NAME);
    put(SimpleHbaseEnrichmentWriter.Configurations.HBASE_CF.getKey(), TABLE_CF);
    put(SimpleHbaseEnrichmentWriter.Configurations.ENRICHMENT_TYPE.getKey(), ENRICHMENT_TYPE);
    put(SimpleHbaseEnrichmentWriter.Configurations.HBASE_PROVIDER.getKey(), MockHBaseTableProvider.class.getName());
  }};

  @Before
  public void setupMockTable() {
    MockHBaseTableProvider.addToCache(TABLE_NAME, TABLE_CF);
  }
  @Test
  public void testBatchOneNormalPath() throws Exception {
    final String sensorType = "dummy";
    SimpleHbaseEnrichmentWriter writer = new SimpleHbaseEnrichmentWriter();

    WriterConfiguration configuration = createConfig(1,
            new HashMap<String, Object>(BASE_WRITER_CONFIG) {{
              put(SimpleHbaseEnrichmentWriter.Configurations.KEY_COLUMNS.getKey(), "ip");
            }}
    );
    writer.configure(sensorType,configuration);

    writer.write( SENSOR_TYPE
            , configuration
            , new ArrayList<BulkMessage<JSONObject>>() {{
              add(new BulkMessage<>("messageId", new JSONObject(ImmutableMap.of("ip", "localhost", "user", "cstella", "foo", "bar"))));
            }}
    );
    List<LookupKV<EnrichmentKey, EnrichmentValue>> values = getValues();
    Assert.assertEquals(1, values.size());
    Assert.assertEquals("localhost", values.get(0).getKey().indicator);
    Assert.assertEquals("cstella", values.get(0).getValue().getMetadata().get("user"));
    Assert.assertEquals("bar", values.get(0).getValue().getMetadata().get("foo"));
    Assert.assertEquals(2, values.get(0).getValue().getMetadata().size());
  }

  @Test
  public void testFilteredKey() throws Exception {
    final String sensorType = "dummy";
    SimpleHbaseEnrichmentWriter writer = new SimpleHbaseEnrichmentWriter();

    WriterConfiguration configuration = createConfig(1,
            new HashMap<String, Object>(BASE_WRITER_CONFIG) {{
              put(SimpleHbaseEnrichmentWriter.Configurations.KEY_COLUMNS.getKey(), "ip");
              put(SimpleHbaseEnrichmentWriter.Configurations.VALUE_COLUMNS.getKey(), "user");
            }}
    );
    writer.configure(sensorType,configuration);

    writer.write( SENSOR_TYPE
            , configuration
            , new ArrayList<BulkMessage<JSONObject>>() {{
              add(new BulkMessage<>("messageId", new JSONObject(ImmutableMap.of("ip", "localhost", "user", "cstella", "foo", "bar"))));
            }}
    );
    List<LookupKV<EnrichmentKey, EnrichmentValue>> values = getValues();
    Assert.assertEquals(1, values.size());
    Assert.assertEquals("localhost", values.get(0).getKey().indicator);
    Assert.assertEquals("cstella", values.get(0).getValue().getMetadata().get("user"));
    Assert.assertNull(values.get(0).getValue().getMetadata().get("foo"));
    Assert.assertEquals(1, values.get(0).getValue().getMetadata().size());
  }

  @Test
  public void testFilteredKeys() throws Exception {
    final String sensorType = "dummy";
    SimpleHbaseEnrichmentWriter writer = new SimpleHbaseEnrichmentWriter();

    WriterConfiguration configuration = createConfig(1,
            new HashMap<String, Object>(BASE_WRITER_CONFIG) {{
              put(SimpleHbaseEnrichmentWriter.Configurations.KEY_COLUMNS.getKey(), "ip");
              put(SimpleHbaseEnrichmentWriter.Configurations.VALUE_COLUMNS.getKey(), ImmutableList.of("user", "ip"));
            }}
    );
    writer.configure(sensorType,configuration);

    writer.write( SENSOR_TYPE
            , configuration
            , new ArrayList<BulkMessage<JSONObject>>() {{
              add(new BulkMessage<>("messageId", new JSONObject(ImmutableMap.of("ip", "localhost", "user", "cstella", "foo", "bar"))));
            }}
    );
    List<LookupKV<EnrichmentKey, EnrichmentValue>> values = getValues();
    Assert.assertEquals(1, values.size());
    Assert.assertEquals("localhost", values.get(0).getKey().indicator);
    Assert.assertEquals("cstella", values.get(0).getValue().getMetadata().get("user"));
    Assert.assertEquals("localhost", values.get(0).getValue().getMetadata().get("ip"));
    Assert.assertNull(values.get(0).getValue().getMetadata().get("foo"));
    Assert.assertEquals(2, values.get(0).getValue().getMetadata().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigValidation_missing_enrichment_type() {
    final String sensorType = "dummy";
    SimpleHbaseEnrichmentWriter writer = new SimpleHbaseEnrichmentWriter();

    WriterConfiguration configuration = createConfig(1,
            new HashMap<String, Object>() {{
              put(SimpleHbaseEnrichmentWriter.Configurations.KEY_COLUMNS.getKey(), "ip");
            }}
    );
    try {
      writer.configure(sensorType, configuration);
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals(String.format("%s must be provided",
              SimpleHbaseEnrichmentWriter.Configurations.ENRICHMENT_TYPE.getKey()), ex.getMessage());
      throw ex;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigValidation_enrichment_type_is_not_a_string() {
    final String sensorType = "dummy";
    SimpleHbaseEnrichmentWriter writer = new SimpleHbaseEnrichmentWriter();

    WriterConfiguration configuration = createConfig(1,
            new HashMap<String, Object>() {{
              put(SimpleHbaseEnrichmentWriter.Configurations.KEY_COLUMNS.getKey(), "ip");
              put(SimpleHbaseEnrichmentWriter.Configurations.ENRICHMENT_TYPE.getKey(), 10);
            }}
    );
    try {
      writer.configure(sensorType, configuration);
    } catch (IllegalArgumentException ex) {
      Assert.assertEquals(String.format("%s must be a string",
              SimpleHbaseEnrichmentWriter.Configurations.ENRICHMENT_TYPE.getKey()), ex.getMessage());
      throw ex;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigValidation_enrichment_type_is_empty() {
    final String sensorType = "dummy";
    SimpleHbaseEnrichmentWriter writer = new SimpleHbaseEnrichmentWriter();

    WriterConfiguration configuration = createConfig(1,
            new HashMap<String, Object>() {{
              put(SimpleHbaseEnrichmentWriter.Configurations.KEY_COLUMNS.getKey(), "ip");
              put(SimpleHbaseEnrichmentWriter.Configurations.ENRICHMENT_TYPE.getKey(), "  ");
            }}
    );
    try {
      writer.configure(sensorType, configuration);
    } catch (IllegalArgumentException ex) {
        Assert.assertEquals(String.format("%s must not be an empty string",
                SimpleHbaseEnrichmentWriter.Configurations.ENRICHMENT_TYPE.getKey()), ex.getMessage());
      throw ex;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigValidation_missing_key_columns() {
    final String sensorType = "dummy";
    SimpleHbaseEnrichmentWriter writer = new SimpleHbaseEnrichmentWriter();

    WriterConfiguration configuration = createConfig(1,
            new HashMap<String, Object>() {{
              put(SimpleHbaseEnrichmentWriter.Configurations.ENRICHMENT_TYPE.getKey(), ENRICHMENT_TYPE);
            }}
    );
    try {
      writer.configure(sensorType, configuration);
    } catch (IllegalArgumentException ex) {
        Assert.assertEquals(String.format("%s must be provided",
                SimpleHbaseEnrichmentWriter.Configurations.KEY_COLUMNS.getKey()), ex.getMessage());
      throw ex;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigValidation_key_columns_contain_an_empty_value() {
    final String sensorType = "dummy";
    SimpleHbaseEnrichmentWriter writer = new SimpleHbaseEnrichmentWriter();

    WriterConfiguration configuration = createConfig(1,
            new HashMap<String, Object>() {{
              put(SimpleHbaseEnrichmentWriter.Configurations.ENRICHMENT_TYPE.getKey(), ENRICHMENT_TYPE);
              put(SimpleHbaseEnrichmentWriter.Configurations.KEY_COLUMNS.getKey(), Arrays.asList("ip", "  "));
            }}
    );
    try {
      writer.configure(sensorType, configuration);
    } catch (IllegalArgumentException ex) {
        Assert.assertEquals("Column name must not be empty", ex.getMessage());
      throw ex;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfigValidation_key_columns_contain_a_null_value() {
    final String sensorType = "dummy";
    SimpleHbaseEnrichmentWriter writer = new SimpleHbaseEnrichmentWriter();

    WriterConfiguration configuration = createConfig(1,
            new HashMap<String, Object>() {{
              put(SimpleHbaseEnrichmentWriter.Configurations.ENRICHMENT_TYPE.getKey(), ENRICHMENT_TYPE);
              put(SimpleHbaseEnrichmentWriter.Configurations.KEY_COLUMNS.getKey(), Arrays.asList("ip", null));
            }}
    );
    try {
      writer.configure(sensorType, configuration);
    } catch (IllegalArgumentException ex) {
        Assert.assertEquals("Column name must not be null", ex.getMessage());
      throw ex;
    }
  }

  public static List<LookupKV<EnrichmentKey, EnrichmentValue>> getValues() throws IOException {
    MockHTable table = (MockHTable) MockHBaseTableProvider.getFromCache(TABLE_NAME);
    Assert.assertNotNull(table);
    List<LookupKV<EnrichmentKey, EnrichmentValue>> ret = new ArrayList<>();
    EnrichmentConverter converter = new EnrichmentConverter();
    for(Result r : table.getScanner(Bytes.toBytes(TABLE_CF))) {
      ret.add(converter.fromResult(r, TABLE_CF));
    }
    return ret;
  }
  public static WriterConfiguration createConfig(final int batchSize, final Map<String, Object> sensorConfig)
  {
    return new WriterConfiguration() {
      @Override
      public int getBatchSize(String sensorName) {
        return batchSize;
      }

      @Override
      public int getBatchTimeout(String sensorName) {
        //TODO - enable unit testing
        return 0;
      }

      @Override
      public List<Integer> getAllConfiguredTimeouts() {
        //TODO - enable unit testing
        return new ArrayList<>();
      }

      @Override
      public String getIndex(String sensorName) {
        return SENSOR_TYPE;
      }

      @Override
      public boolean isEnabled(String sensorName) {
        return true;
      }

      @Override
      public Map<String, Object> getSensorConfig(String sensorName) {
        return sensorConfig;
      }

      @Override
      public Map<String, Object> getGlobalConfig() {
        return null;
      }

      @Override
      public boolean isDefault(String sensorName) {
        return false;
      }

      @Override
      public String getFieldNameConverter(String sensorName) {
        return null;
      }
    };
  }
}
