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

package org.apache.metron.enrichment.writer;

import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.MessageId;
import org.apache.storm.task.TopologyContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.writer.AbstractWriter;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleHbaseEnrichmentWriter extends AbstractWriter implements BulkMessageWriter<JSONObject>, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(SimpleHbaseEnrichmentWriter.class);

  public enum Configurations {
    HBASE_TABLE("shew.table")
    ,HBASE_CF("shew.cf")
    ,KEY_COLUMNS("shew.keyColumns")
    ,KEY_DELIM("shew.keyDelim")
    ,ENRICHMENT_TYPE("shew.enrichmentType")
    ,VALUE_COLUMNS("shew.valueColumns")
    ,HBASE_PROVIDER("shew.hbaseProvider")
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
  private transient EnrichmentConverter converter;
  private String tableName;
  private String cf;
  private HTableInterface table;
  private TableProvider provider;
  private Map.Entry<Object, KeyTransformer> keyTransformer;

  public SimpleHbaseEnrichmentWriter() {
  }

  @Override
  public void configure(String sensorName, WriterConfiguration configuration) {
    validateEnrichmentType(sensorName, configuration);
    validateKeyColumns(sensorName, configuration);
    String hbaseProviderImpl = Configurations.HBASE_PROVIDER.getAndConvert(configuration.getSensorConfig(sensorName),String.class);
    if(hbaseProviderImpl != null) {
      provider = ReflectionUtils.createInstance(hbaseProviderImpl);
    }
    if(converter == null) {
      converter = new EnrichmentConverter();
    }
    LOG.debug("Sensor: '{}': {Provider: '{}', Converter: '{}'}", sensorName, getClassName(provider), getClassName(converter));
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

  private String getClassName(Object object) {
    return object == null ? "" : object.getClass().getName();
  }

  @Override
  public void init(Map stormConf, TopologyContext topologyContext, WriterConfiguration configuration) throws Exception {
    if(converter == null) {
      converter = new EnrichmentConverter();
    }
  }

  protected synchronized TableProvider getProvider() {
    if(provider == null) {

      provider = new HTableProvider();
    }
    return provider;
  }

  public HTableInterface getTable(String tableName, String cf) throws IOException {
    synchronized(this) {
      boolean isInitial = this.tableName == null || this.cf == null;
      boolean isValid = tableName != null && cf != null;

      if( isInitial || (isValid && (!this.tableName.equals(tableName) || !this.cf.equals(cf)) )
        )
      {
        Configuration conf = HBaseConfiguration.create();
        //new table connection
        if(table != null) {
          table.close();
        }
        LOG.debug("Fetching table '{}', column family: '{}'", tableName, cf);
        table = getProvider().getTable(conf, tableName);
        this.tableName = tableName;
        this.cf = cf;
      }
      return table;
    }
  }

  public HTableInterface getTable(Map<String, Object> config) throws IOException {
    return getTable(Configurations.HBASE_TABLE.getAndConvert(config, String.class)
                   ,Configurations.HBASE_CF.getAndConvert(config, String.class)
                   );

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
                    ) throws Exception
  {
    Map<String, Object> sensorConfig = configurations.getSensorConfig(sensorType);
    HTableInterface table = getTable(sensorConfig);
    KeyTransformer transformer = getTransformer(sensorConfig);
    Object enrichmentTypeObj = Configurations.ENRICHMENT_TYPE.get(sensorConfig);
    String enrichmentType = enrichmentTypeObj == null?null:enrichmentTypeObj.toString();
    Set<String> valueColumns = new HashSet<>(getColumns(Configurations.VALUE_COLUMNS.get(sensorConfig), true));
    List<Put> puts = new ArrayList<>();
    for(BulkMessage<JSONObject> bulkWriterMessage : messages) {
      EnrichmentKey key = getKey(bulkWriterMessage.getMessage(), transformer, enrichmentType);
      EnrichmentValue value = getValue(bulkWriterMessage.getMessage(), transformer.keySet, valueColumns);
      if(key == null || value == null) {
        continue;
      }
      Put put = converter.toPut(this.cf, key, value);
      if(put != null) {
        LOG.debug("Put: {Column Family: '{}', Key: '{}', Value: '{}'}", this.cf, key, value);
        puts.add(put);
      }
    }
    Set<MessageId> ids = messages.stream().map(BulkMessage::getId).collect(Collectors.toSet());
    BulkWriterResponse response = new BulkWriterResponse();
    try {
      table.put(puts);
    } catch (Exception e) {
      response.addAllErrors(e, ids);
      return response;
    }

    // Can return no errors, because put will throw Exception on error.
    response.addAllSuccesses(ids);
    return response;
  }

  @Override
  public String getName() {
    return "hbaseEnrichment";
  }

  @Override
  public void close() throws Exception {
    synchronized(this) {
      if(table != null) {
        table.close();
      }
    }
  }
}
