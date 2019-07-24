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

package org.apache.metron.writer.hbase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.writer.MessageId;
import org.apache.metron.enrichment.converter.DefaultEnrichmentConverterFactory;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentConverterFactory;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.hbase.client.HBaseClientFactory;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.apache.metron.hbase.client.HBaseTableClientFactory;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.writer.AbstractWriter;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Writes streaming enrichment data to HBase.
 */
public class SimpleHbaseEnrichmentWriter extends AbstractWriter implements BulkMessageWriter<JSONObject>, Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(SimpleHbaseEnrichmentWriter.class);

  public enum Configurations {
    HBASE_TABLE("shew.table")
    ,HBASE_CF("shew.cf")
    ,KEY_COLUMNS("shew.keyColumns")
    ,KEY_DELIM("shew.keyDelim")
    ,ENRICHMENT_TYPE("shew.enrichmentType")
    ,VALUE_COLUMNS("shew.valueColumns")
    ,HBASE_CONNECTION_FACTORY("shew.hBaseConnectionFactory")
    ,HBASE_CLIENT_FACTORY("shew.hBaseClientFactory")
    ;
    String key;
    Configurations(String key) {
      this.key = key;
    }
    public String getKey() {
      return key;
    }
    public Object get(Map<String, Object> config) {
      Object o = config.get(key);
      if (o == null) {
        LOG.warn("No config object found for key: '{}'", key);
      }
      return o;
    }
    public <T> T getAndConvert(Map<String, Object> config, Class<T> clazz) {
      Object o = get(config);
      if(o != null) {
        return ConversionUtils.convert(o, clazz);
      }
      LOG.warn("No object of type '{}' found in config", clazz);
      return null;
    }
  }
  public static class KeyTransformer {
    List<String> keys = new ArrayList<>();
    Set<String> keySet;
    private String delim = ":";
    public KeyTransformer(String key) {
      this(key, null);
    }
    public KeyTransformer(String key, String delim) {
      keys.add(key);
      keySet = new HashSet<>(this.keys);
      this.delim = delim == null?this.delim:delim;
    }
    public KeyTransformer(Iterable<String> keys) {
      this(keys, null);
    }
    public KeyTransformer(Iterable<String> keys, String delim) {
      Iterables.addAll(this.keys, keys);
      keySet = new HashSet<>(this.keys);
      this.delim = delim == null?this.delim:delim;
    }

    public String transform(final JSONObject message) {
      String transformedMessage = keys.stream().map(x -> {
        Object o = message.get(x);
        return o == null?"":o.toString();
      }).collect(Collectors.joining(delim));
      LOG.debug("Transformed message: '{}'", transformedMessage);
      return transformedMessage;
    }
  }
  private String tableName;
  private String cf;
  private Map.Entry<Object, KeyTransformer> keyTransformer;
  private HBaseConnectionFactory hBaseConnectionFactory;
  private HBaseClientFactory hBaseClientFactory;
  private transient HBaseClient hBaseClient;

  public SimpleHbaseEnrichmentWriter() {
    // the defaults should no others be defined
    hBaseConnectionFactory = new HBaseConnectionFactory();
    hBaseClientFactory = new HBaseTableClientFactory();
  }

  SimpleHbaseEnrichmentWriter withHBaseClientFactory(HBaseClientFactory hBaseClientFactory) {
    this.hBaseClientFactory = hBaseClientFactory;
    return this;
  }

  @Override
  public void configure(String sensorName, WriterConfiguration configuration) {
    validateEnrichmentType(sensorName, configuration);
    validateKeyColumns(sensorName, configuration);
    Map<String, Object> sensorConfig = configuration.getSensorConfig(sensorName);

    // hbase table
    tableName = Configurations.HBASE_TABLE.getAndConvert(sensorConfig, String.class);
    if(tableName == null) {
      String msg = String.format("Missing configuration: %s", Configurations.HBASE_TABLE.getKey());
      throw new IllegalArgumentException(msg);
    }

    // hbase column family
    cf = Configurations.HBASE_CF.getAndConvert(sensorConfig, String.class);
    if(cf == null) {
      String msg = String.format("Missing configuration: %s", Configurations.HBASE_CF.getKey());
      throw new IllegalArgumentException(msg);
    }

    // hbase connection factory
    String connectionFactoryImpl = Configurations.HBASE_CONNECTION_FACTORY.getAndConvert(sensorConfig, String.class);
    if(connectionFactoryImpl != null) {
      hBaseConnectionFactory = HBaseConnectionFactory.byName(connectionFactoryImpl);
    }

    // hbase client factory
    String clientImpl = Configurations.HBASE_CLIENT_FACTORY.getAndConvert(sensorConfig, String.class);
    if(clientImpl != null) {
      hBaseClientFactory = HBaseClientFactory.byName(clientImpl, () -> new HBaseTableClientFactory());
    }
  }

  @Override
  public void init(Map stormConf, WriterConfiguration configuration) {
    if(hBaseClient == null) {
      // establish a connection to HBase
      hBaseClient = hBaseClientFactory.create(hBaseConnectionFactory, HBaseConfiguration.create(), tableName);
    }
  }

  private void validateEnrichmentType(String sensorName, WriterConfiguration configuration) {
    Map<String, Object> sensorConfig = configuration.getSensorConfig(sensorName);
    Object enrichmentTypeObj = Configurations.ENRICHMENT_TYPE.get(sensorConfig);
    if (enrichmentTypeObj == null) {
      throw new IllegalArgumentException(String.format("%s must be provided", Configurations.ENRICHMENT_TYPE.getKey()));
    }

    if (!(enrichmentTypeObj instanceof String)) {
      throw new IllegalArgumentException(String.format("%s must be a string", Configurations.ENRICHMENT_TYPE.getKey()));
    }

    String enrichmentType = enrichmentTypeObj.toString();
    if (enrichmentType.trim().isEmpty()) {
      throw new IllegalArgumentException(String.format("%s must not be an empty string",
              Configurations.ENRICHMENT_TYPE.getKey()));
    }
  }

  private void validateKeyColumns(String sensorName, WriterConfiguration configuration) {
    Map<String, Object> sensorConfig = configuration.getSensorConfig(sensorName);
    Object keyColumnsObj = Configurations.KEY_COLUMNS.get(sensorConfig);

    try {
      List<String> keyColumns = getColumns(keyColumnsObj, true);
      if (keyColumns == null || keyColumns.isEmpty()) {
        throw new IllegalArgumentException(String.format("%s must be provided", Configurations.KEY_COLUMNS.getKey()));
      }
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }

  private List<String> getColumns(Object keyColumnsObj, boolean allowNull) {
    Object o = keyColumnsObj;
    if(allowNull && keyColumnsObj == null) {
      LOG.debug("No key columns were specified");
      return Collections.emptyList();
    }
    if(o instanceof String) {
      LOG.debug("Key column: '{}'", o);
      return ImmutableList.of(o.toString());
    }
    else if (o instanceof List) {
      List<String> keyCols = new ArrayList<>();
      for(Object key : (List)o) {
        if (key == null) {
          throw new IllegalArgumentException("Column name must not be null");
        }
        String columnName = key.toString();
        if (columnName.trim().isEmpty()) {
          throw new IllegalArgumentException("Column name must not be empty");
        }
        keyCols.add(columnName);
      }
      LOG.debug("Key columns: '{}'", String.join(",", keyCols));
      return keyCols;
    }
    else {
      throw new RuntimeException("Unable to get columns: " + o);
    }
  }

  private KeyTransformer getTransformer(Map<String, Object> config) {
    Object o = Configurations.KEY_COLUMNS.get(config);
    KeyTransformer transformer = null;
    if(keyTransformer != null && keyTransformer.getKey() == o) {
      transformer = keyTransformer.getValue();
      LOG.debug("Transformer found for key '{}': '{}'", o, transformer);
      return transformer;
    }
    else {
      List<String> keys = getColumns(o, false);
      Object delimObj = Configurations.KEY_DELIM.get(config);
      String delim = (delimObj == null || !(delimObj instanceof String))?null:delimObj.toString();
      transformer = new KeyTransformer(keys, delim);
      keyTransformer = new AbstractMap.SimpleEntry<>(o, transformer);
      LOG.debug("Transformer found for keys '{}' and delimiter '{}': '{}'", String.join(",", keys), delim, transformer);
      return transformer;
    }
  }


  private EnrichmentValue getValue( JSONObject message
                                  , Set<String> keyColumns
                                  , Set<String> valueColumns
                                  )
  {
    Map<String, Object> metadata = new HashMap<>();
    if(valueColumns == null || valueColumns.isEmpty()) {
      for (Object kv : message.entrySet()) {
        Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) kv;
        if (!keyColumns.contains(entry.getKey())) {
          addMetadataEntry(metadata, entry);
        }
      }
      return new EnrichmentValue(metadata);
    }
    else {
      for (Object kv : message.entrySet()) {
        Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) kv;
        if (valueColumns.contains(entry.getKey())) {
          addMetadataEntry(metadata, entry);
        }
      }
      return new EnrichmentValue(metadata);
    }
  }

  private void addMetadataEntry(Map<String, Object> metadata, Map.Entry<Object, Object> entry) {
    String key = entry.getKey().toString();
    Object value = entry.getValue();
    LOG.debug("Adding metadata: {Key: '{}', Value: '{}'}", key, value);
    metadata.put(key, value);
  }

  private EnrichmentKey getKey(JSONObject message, KeyTransformer transformer, String enrichmentType) {
    if(enrichmentType != null) {
      return new EnrichmentKey(enrichmentType, transformer.transform(message));
    }
    else {
      return null;
    }
  }

  @Override
  public BulkWriterResponse write(String sensorType
                    , WriterConfiguration configurations
                    , List<BulkMessage<JSONObject>> messages
                    ) throws Exception {

    Map<String, Object> sensorConfig = configurations.getSensorConfig(sensorType);
    KeyTransformer transformer = getTransformer(sensorConfig);
    Object enrichmentTypeObj = Configurations.ENRICHMENT_TYPE.get(sensorConfig);
    String enrichmentType = enrichmentTypeObj == null?null:enrichmentTypeObj.toString();
    Set<String> valueColumns = new HashSet<>(getColumns(Configurations.VALUE_COLUMNS.get(sensorConfig), true));

    for(BulkMessage<JSONObject> bulkWriterMessage : messages) {
      EnrichmentKey key = getKey(bulkWriterMessage.getMessage(), transformer, enrichmentType);
      EnrichmentValue value = getValue(bulkWriterMessage.getMessage(), transformer.keySet, valueColumns);
      if(key != null && value != null) {
        write(key, value);
      }
    }

    BulkWriterResponse response = new BulkWriterResponse();
    Set<MessageId> ids = messages.stream().map(BulkMessage::getId).collect(Collectors.toSet());
    try {
      hBaseClient.mutate();
    } catch (Exception e) {
      response.addAllErrors(e, ids);
      return response;
    }

    // Can return no errors, because put will throw Exception on error.
    response.addAllSuccesses(ids);
    return response;
  }

  private void write(EnrichmentKey key, EnrichmentValue value) {
    final byte[] columnFamily = Bytes.toBytes(cf);
    ColumnList columns = new ColumnList();
    for(Map.Entry<byte[], byte[]> kv : value.toColumns()) {
      columns.addColumn(columnFamily, kv.getKey(), kv.getValue());
    }
    hBaseClient.addMutation(key.toBytes(), columns);
  }

  @Override
  public String getName() {
    return "hbaseEnrichment";
  }

  @Override
  public void close() throws Exception {
    synchronized(this) {
      if(hBaseClient != null) {
        hBaseClient.close();
      }
    }
  }
}
