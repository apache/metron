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

package org.apache.metron.common.csv;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class CSVConverter implements Serializable {
  public static final String COLUMNS_KEY="columns";
  public static final String SEPARATOR_KEY="separator";
  protected Map<String, Integer> columnMap = new HashMap<>();
  protected CSVParser parser;

  public Map<String, Integer> getColumnMap() {
    return columnMap;
  }

  public CSVParser getParser() {
    return parser;
  }

  public Map<String, String> toMap(String line) throws IOException {
    if(ignore(line)) {
      return null;
    }
    String[] tokens = parser.parseLine(line);
    Map<String, String> values = new HashMap<>();
    for(Map.Entry<String, Integer> kv : columnMap.entrySet()) {
      values.put(kv.getKey(), tokens[kv.getValue()]);
    }
    return values;
  }

  public void initialize(Map<String, Object> config) {
    if(config.containsKey(COLUMNS_KEY)) {
      columnMap = getColumnMap(config);
    }
    else {
      throw new IllegalStateException("CSVExtractor requires " + COLUMNS_KEY + " configuration");
    }
    char separator = ',';
    if(config.containsKey(SEPARATOR_KEY)) {
      separator = config.get(SEPARATOR_KEY).toString().charAt(0);

    }
    parser = new CSVParserBuilder().withSeparator(separator)
              .build();
  }
  protected boolean ignore(String line) {
    if(null == line) {
      return true;
    }
    String trimmedLine = line.trim();
    return trimmedLine.startsWith("#") || isEmpty(trimmedLine);
  }
  public static Map.Entry<String, Integer> getColumnMapEntry(String column, int i) {
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
  public static Map<String, Integer> getColumnMap(Map<String, Object> config) {
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
}
