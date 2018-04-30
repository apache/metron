/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.metron.indexing.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.junit.Assert;
import org.junit.Test;

public abstract class UpdateIntegrationTest {

  private static final int MAX_RETRIES = 10;
  private static final int SLEEP_MS = 500;
  protected static final String SENSOR_NAME = "test";
  private static final String CF = "p";

  protected static MultiIndexDao dao;

  @Test
  public void test() throws Exception {
    List<Map<String, Object>> inputData = new ArrayList<>();
    for(int i = 0; i < 10;++i) {
      final String name = "message" + i;
      inputData.add(
          new HashMap<String, Object>() {{
            put("source.type", SENSOR_NAME);
            put("name" , name);
            put("timestamp", System.currentTimeMillis());
            put(Constants.GUID, name);
          }}
      );
    }
    addTestData(getIndexName(), SENSOR_NAME, inputData);
    List<Map<String,Object>> docs = null;
    for(int t = 0;t < MAX_RETRIES;++t, Thread.sleep(SLEEP_MS)) {
      docs = getIndexedTestData(getIndexName(), SENSOR_NAME);
      if(docs.size() >= 10) {
        break;
      }
    }
    Assert.assertEquals(10, docs.size());
    //modify the first message and add a new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {{
        put("new-field", "metron");
      }};
      String guid = "" + message0.get(Constants.GUID);
      dao.replace(new ReplaceRequest(){{
        setReplacement(message0);
        setGuid(guid);
        setSensorType(SENSOR_NAME);
        setIndex(getIndexName());
      }}, Optional.empty());

      Assert.assertEquals(1, getMockHTable().size());
      Document doc = dao.getLatest(guid, SENSOR_NAME);
      Assert.assertEquals(message0, doc.getDocument());
      {
        //ensure hbase is up to date
        Get g = new Get(HBaseDao.Key.toBytes(new HBaseDao.Key(guid, SENSOR_NAME)));
        Result r = getMockHTable().get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(1, columns.size());
        Assert.assertEquals(message0
            , JSONUtils.INSTANCE.load(new String(columns.lastEntry().getValue())
                , JSONUtils.MAP_SUPPLIER)
        );
      }
      {
        //ensure ES is up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = getIndexedTestData(getIndexName(), SENSOR_NAME);
          cnt = docs
              .stream()
              .filter(d -> message0.get("new-field").equals(d.get("new-field")))
              .count();
        }
        Assert.assertNotEquals("Elasticsearch is not updated!", cnt, 0);
      }
    }
    //modify the same message and modify the new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {{
        put("new-field", "metron2");
      }};
      String guid = "" + message0.get(Constants.GUID);
      dao.replace(new ReplaceRequest(){{
        setReplacement(message0);
        setGuid(guid);
        setSensorType(SENSOR_NAME);
        setIndex(getIndexName());
      }}, Optional.empty());
      Assert.assertEquals(1, getMockHTable().size());
      Document doc = dao.getLatest(guid, SENSOR_NAME);
      Assert.assertEquals(message0, doc.getDocument());
      {
        //ensure hbase is up to date
        Get g = new Get(HBaseDao.Key.toBytes(new HBaseDao.Key(guid, SENSOR_NAME)));
        Result r = getMockHTable().get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(2, columns.size());
        Assert.assertEquals(message0, JSONUtils.INSTANCE.load(new String(columns.lastEntry().getValue())
            , JSONUtils.MAP_SUPPLIER)
        );
        Assert.assertNotEquals(message0, JSONUtils.INSTANCE.load(new String(columns.firstEntry().getValue())
            , JSONUtils.MAP_SUPPLIER)
        );
      }
      {
        //ensure ES is up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t,Thread.sleep(SLEEP_MS)) {
          docs = getIndexedTestData(getIndexName(), SENSOR_NAME);
          cnt = docs
              .stream()
              .filter(d -> message0.get("new-field").equals(d.get("new-field")))
              .count();
        }

        Assert.assertNotEquals("Index is not updated!", cnt, 0);
      }
    }
  }

  protected abstract String getIndexName();
  protected abstract MockHTable getMockHTable();
  protected abstract void addTestData(String indexName, String sensorType, List<Map<String,Object>> docs) throws Exception;
  protected abstract List<Map<String,Object>> getIndexedTestData(String indexName, String sensorType) throws Exception;
}
