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

import org.apache.metron.common.csv.CSVConverter;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.enrichment.lookup.LookupKey;

import java.io.IOException;
import java.util.*;


public class CSVExtractor extends CSVConverter implements Extractor {
  public static final String INDICATOR_COLUMN_KEY="indicator_column";
  public static final String TYPE_COLUMN_KEY="type_column";
  public static final String TYPE_KEY="type";
  public static final String LOOKUP_CONVERTER = "lookup_converter";

  private int typeColumn;
  private String type;
  private int indicatorColumn;

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


  public LookupConverter getConverter() {
    return converter;
  }
  @Override
  public Iterable<LookupKV> extract(String line) throws IOException {
    if(ignore(line)) {
      return Collections.emptyList();
    }
    String[] tokens = parser.parseLine(line);

    LookupKey key = converter.toKey(getType(tokens), tokens[indicatorColumn]);
    Map<String, Object> values = new HashMap<>();
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



  @Override
  public void initialize(Map<String, Object> config) {
    super.initialize(config);

    if(config.containsKey(INDICATOR_COLUMN_KEY)) {
      indicatorColumn = columnMap.get(config.get(INDICATOR_COLUMN_KEY).toString());
    }
    if(config.containsKey(TYPE_KEY)) {
      type = config.get(TYPE_KEY).toString();
    }
    else if(config.containsKey(TYPE_COLUMN_KEY)) {
      typeColumn = columnMap.get(config.get(TYPE_COLUMN_KEY).toString());
    }
    if(config.containsKey(LOOKUP_CONVERTER)) {
      converter = LookupConverters.getConverter((String) config.get(LOOKUP_CONVERTER));
    }
  }
}
