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
package org.apache.metron.rest.controller;

import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.InMemoryDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaoControllerTest {
  public static final String TABLE = "updates";
  public static final String CF = "t";
  public void loadTestData() throws ParseException {
    Map<String, List<String>> backingStore = new HashMap<>();
    for(Map.Entry<String, String> indices :
            ImmutableMap.of(
                    "bro_index_2017.01.01.01", SearchIntegrationTest.broData,
                    "snort_index_2017.01.01.01", SearchIntegrationTest.snortData
            ).entrySet()
       )
    {
      List<String> results = new ArrayList<>();
      backingStore.put(indices.getKey(), results);
      JSONArray broArray = (JSONArray) new JSONParser().parse(indices.getValue());
      int i = 0;
      for(Object o: broArray) {
        JSONObject jsonObject = (JSONObject) o;
        jsonObject.put(Constants.GUID, indices.getKey() + ":" + i++);
        results.add(jsonObject.toJSONString());
      }
    }
    InMemoryDao.load(backingStore);
  }
}
