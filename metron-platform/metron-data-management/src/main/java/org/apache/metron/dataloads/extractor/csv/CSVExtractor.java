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
package org.apache.metron.dataloads.extractor.csv;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.enrichment.lookup.LookupKey;

import java.io.IOException;
import java.util.*;

public class CSVExtractor implements Extractor {
  public static final String COLUMNS_KEY="columns";
  public static final String INDICATOR_COLUMN_KEY="indicator_column";
  public static final String TYPE_COLUMN_KEY="type_column";
  public static final String TYPE_KEY="type";
  public static final String SEPARATOR_KEY="separator";
  public static final String LOOKUP_CONVERTER = "lookup_converter";

  private int typeColumn;
  private String type;
  private int indicatorColumn;
  private Map<String, Integer> columnMap = new HashMap<>();
  private CSVParser parser;
  private LookupConverter converter = LookupConverters.ENRICHMENT.getConverter();

  public int getTypeColumn() {
    return typeColumn;
  }

  public String getType() {
    return type;
  }

  public int getIndicatorColumn() {
    return indicatorColumn;
  }

  public Map<String, Integer> getColumnMap() {
    return columnMap;
  }

  public CSVParser getParser() {
    return parser;
  }

  public LookupConverter getConverter() {
    return converter;
  }
  @Override
  public Iterable<LookupKV> extract(String line) throws IOException {
    if(line.trim().startsWith("#")) {
      //comment
      return Collections.emptyList();
    }
    String[] tokens = parser.parseLine(line);

    LookupKey key = converter.toKey(getType(tokens), tokens[indicatorColumn]);
    Map<String, String> values = new HashMap<>();
    for(Map.Entry<String, Integer> kv : columnMap.entrySet()) {
      values.put(kv.getKey(), tokens[kv.getValue()]);
    }
    return Arrays.asList(new LookupKV(key, converter.toValue(values)));
  }

  private String getType(String[] tokens) {
    if(type == null) {
      return tokens[typeColumn];
    }
    else {
      return type;
    }
  }

  private static Map.Entry<String, Integer> getColumnMapEntry(String column, int i) {
    if(column.contains(":")) {
      Iterable<String> tokens = Splitter.on(':').split(column);
      String col = Iterables.getFirst(tokens, null);
      Integer pos = Integer.parseInt(Iterables.getLast(tokens));
      return new AbstractMap.SimpleEntry<>(col, pos);
    }
    else {
      return new AbstractMap.SimpleEntry<>(column, i);
    }

  }
  private static Map<String, Integer> getColumnMap(Map<String, Object> config) {
    Map<String, Integer> columnMap = new HashMap<>();
    if(config.containsKey(COLUMNS_KEY)) {
      Object columnsObj = config.get(COLUMNS_KEY);
      if(columnsObj instanceof String) {
        String columns = (String)columnsObj;
        int i = 0;
        for (String column : Splitter.on(',').split(columns)) {
          Map.Entry<String, Integer> e = getColumnMapEntry(column, i++);
          columnMap.put(e.getKey(), e.getValue());
        }
      }
      else if(columnsObj instanceof List) {
        List columns = (List)columnsObj;
        int i = 0;
        for(Object column : columns) {
          Map.Entry<String, Integer> e = getColumnMapEntry(column.toString(), i++);
          columnMap.put(e.getKey(), e.getValue());
        }
      }
      else if(columnsObj instanceof Map) {
        Map<Object, Object> map = (Map<Object, Object>)columnsObj;
        for(Map.Entry<Object, Object> e : map.entrySet()) {
          columnMap.put(e.getKey().toString(), Integer.parseInt(e.getValue().toString()));
        }
      }
    }
    return columnMap;
  }

  @Override
  public void initialize(Map<String, Object> config) {
    if(config.containsKey(COLUMNS_KEY)) {
      columnMap = getColumnMap(config);
    }
    else {
      throw new IllegalStateException("CSVExtractor requires " + COLUMNS_KEY + " configuration");
    }
    if(config.containsKey(INDICATOR_COLUMN_KEY)) {
      indicatorColumn = columnMap.get(config.get(INDICATOR_COLUMN_KEY).toString());
    }
    if(config.containsKey(TYPE_KEY)) {
      type = config.get(TYPE_KEY).toString();
    }
    else if(config.containsKey(TYPE_COLUMN_KEY)) {
      typeColumn = columnMap.get(config.get(TYPE_COLUMN_KEY).toString());
    }
    if(config.containsKey(SEPARATOR_KEY)) {
      char separator = config.get(SEPARATOR_KEY).toString().charAt(0);
      parser = new CSVParserBuilder().withSeparator(separator)
              .build();
    }
    if(config.containsKey(LOOKUP_CONVERTER)) {
      converter = LookupConverters.getConverter((String) config.get(LOOKUP_CONVERTER));
    }
  }
}
