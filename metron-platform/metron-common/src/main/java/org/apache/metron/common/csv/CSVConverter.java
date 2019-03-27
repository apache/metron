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

/**
 * Based on the parser config field `COLUMNS_KEY` the log line is
 * transformed into a intermediate result using the CSV parser.
 *
 * <p>All keys in `COLUMNS_KEY` and values extracted from the log line
 * will be trimmed before adding to `values`.
 */
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

  /**
   * Converts a CSV line to a map of column name -> value.
   *
   * @param line The line to be turned into a map
   * @return A map from column name -> value in the line
   * @throws IOException If there's an issue parsing the line
   */
  public Map<String, String> toMap(String line) throws IOException {
    if(ignore(line)) {
      return null;
    }
    String[] tokens = parser.parseLine(line);
    Map<String, String> values = new HashMap<>();
    for(Map.Entry<String, Integer> kv : columnMap.entrySet()) {
      values.put(kv.getKey().trim(), tokens[kv.getValue()].trim());
    }
    return values;
  }

  /**
   * Initializes the CSVConverter based on the provided config. The config should contain
   * an entry for {@code columns}, and can optionally contain a {@code separator}.
   *
   * @param config The configuration used for setup
   */
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

  /**
   * Given a column determines the column -> position. If the column contains data in the format
   * {@code columnName:position}, use that. Otherwise, use the provided value of i.
   *
   * @param column The data in the column
   * @param i The default position to use
   * @return A map entry of column value -> position
   */
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

  /**
   * Retrieves the column map based on the columns key in the config map.
   *
   * <p>If the data is a string, the default is to split on ',' as a list of column names.
   *
   * <p>If the data is a list, the default is to used as the list of column names.
   *
   * <p>If the data is a map, it must be in the form columnName -> position.
   *
   * <p>For string and list, the data can be in the form columnName:position, and extracted
   * appropriately.
   *
   * @param config Uses the columns key if defined to retrieve columns.
   * @return A map from column -> position
   */
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
