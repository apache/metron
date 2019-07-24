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
package org.apache.metron.enrichment.converter;

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.enrichment.lookup.LookupValue;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EnrichmentValue implements LookupValue {
  private static final ThreadLocal<ObjectMapper> _mapper = new ThreadLocal().withInitial(() -> new ObjectMapper());
  public static final String VALUE_COLUMN_NAME = "v";
  public static final byte[] VALUE_COLUMN_NAME_B = Bytes.toBytes(VALUE_COLUMN_NAME);
  private Map<String, Object> metadata;

  public EnrichmentValue() {
    this.metadata = new HashMap<>();
  }

  public EnrichmentValue(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public EnrichmentValue withValue(String key, Object value) {
    metadata.put(key, value);
    return this;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @Override
  public Iterable<Map.Entry<byte[], byte[]>> toColumns() {
    return EnrichmentConverter.toEntries( VALUE_COLUMN_NAME_B, Bytes.toBytes(valueToString(metadata)));
  }

  @Override
  public void fromColumns(Iterable<Map.Entry<byte[], byte[]>> values) {
    for(Map.Entry<byte[], byte[]> cell : values) {
      fromColumn(cell.getKey(), cell.getValue());
    }
  }

  public void fromColumn(byte[] columnQualifier, byte[] value) {
    String columnValue = Bytes.toString(value);
    if(Bytes.equals(columnQualifier, VALUE_COLUMN_NAME_B)) {
      metadata = stringToValue(columnValue);
    }
  }

  public Map<String, Object> stringToValue(String s){
    try {
      return _mapper.get().readValue(s, new TypeReference<Map<String, Object>>(){});
    } catch (IOException e) {
      throw new RuntimeException("Unable to convert string to metadata: " + s);
    }
  }
  public String valueToString(Map<String, Object> value) {
    try {
      return _mapper.get().writeValueAsString(value);
    } catch (IOException e) {
      throw new RuntimeException("Unable to convert metadata to string: " + value);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EnrichmentValue)) return false;
    EnrichmentValue that = (EnrichmentValue) o;
    return Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata);
  }

  @Override
  public String toString() {
    return "EnrichmentValue{" +
            "metadata=" + metadata +
            '}';
  }
}
