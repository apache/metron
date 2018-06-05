/*
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

package org.apache.metron.solr.integration;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.solr.dao.SolrDao;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SolrRetrieveLatestIntegrationTest {

  private static SolrComponent solrComponent;

  protected static final String TEST_COLLECTION = "test";
  protected static final String TEST_SENSOR = "test_sensor";
  protected static final String BRO_SENSOR = "bro";

  private static IndexDao dao;

  @BeforeClass
  public static void setupBeforeClass() throws Exception {
    solrComponent = new SolrComponent.Builder().build();
    solrComponent.start();
  }

  @Before
  public void setup() throws Exception {
    solrComponent
        .addCollection(TEST_COLLECTION, "../metron-solr/src/test/resources/config/test/conf");
    solrComponent.addCollection(BRO_SENSOR, "../metron-solr/src/main/config/schema/bro");

    AccessConfig accessConfig = new AccessConfig();
    Map<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(SOLR_ZOOKEEPER, solrComponent.getZookeeperUrl());
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);
    // Map the sensor name to the collection name for test.
    accessConfig.setIndexSupplier(s -> s.equals(TEST_SENSOR) ? TEST_COLLECTION : s);

    dao = new SolrDao();
    dao.init(accessConfig);
    addData(BRO_SENSOR, BRO_SENSOR);
    addData(TEST_COLLECTION, TEST_SENSOR);
  }

  @After
  public void reset() {
    solrComponent.reset();
  }

  @AfterClass
  public static void teardown() {
    solrComponent.stop();
  }

  @Test
  public void testGetLatest() throws IOException {
    Document actual = dao.getLatest("message_1_bro", BRO_SENSOR);
    assertEquals(buildExpectedDocument(BRO_SENSOR, 1), actual);
  }

  @Test
  public void testGetLatestCollectionSensorDiffer() throws IOException {
    Document actual = dao.getLatest("message_1_test_sensor", TEST_SENSOR);
    assertEquals(buildExpectedDocument(TEST_SENSOR, 1), actual);
  }

  @Test
  public void testGetAllLatest() throws IOException {
    List<GetRequest> requests = new ArrayList<>();
    requests.add(buildGetRequest(BRO_SENSOR, 1));
    requests.add(buildGetRequest(BRO_SENSOR, 2));

    Iterable<Document> actual = dao.getAllLatest(requests);
    for (Document doc : actual) {
      System.out.println("DOC: " + doc);
    }
    assertTrue(Iterables.contains(actual, buildExpectedDocument(BRO_SENSOR, 1)));
    assertTrue(Iterables.contains(actual, buildExpectedDocument(BRO_SENSOR, 2)));
  }

  @Test
  public void testGetAllLatestCollectionSensorDiffer() throws IOException {
    List<GetRequest> requests = new ArrayList<>();
    requests.add(buildGetRequest(TEST_SENSOR, 1));
    requests.add(buildGetRequest(TEST_SENSOR, 2));

    Iterable<Document> actual = dao.getAllLatest(requests);
    assertTrue(Iterables.contains(actual, buildExpectedDocument(TEST_SENSOR, 1)));
    assertTrue(Iterables.contains(actual, buildExpectedDocument(TEST_SENSOR, 2)));
  }

  @Test
  public void testGetAllLatestCollectionSensorMixed() throws IOException {
    List<GetRequest> requests = new ArrayList<>();
    requests.add(buildGetRequest(TEST_SENSOR, 1));
    requests.add(buildGetRequest(BRO_SENSOR, 2));

    Iterable<Document> actual = dao.getAllLatest(requests);
    assertTrue(Iterables.contains(actual, buildExpectedDocument(TEST_SENSOR, 1)));
    assertTrue(Iterables.contains(actual, buildExpectedDocument(BRO_SENSOR, 2)));
  }

  protected Document buildExpectedDocument(String sensor, int i) {
    Map<String, Object> expectedMapOne = new HashMap<>();
    expectedMapOne.put("source.type", sensor);
    expectedMapOne.put(Constants.GUID, buildGuid(sensor, i));
    return new Document(expectedMapOne, buildGuid(sensor, i), sensor, 0L);
  }

  protected GetRequest buildGetRequest(String sensor, int i) {
    GetRequest requestOne = new GetRequest();
    requestOne.setGuid(buildGuid(sensor, i));
    requestOne.setSensorType(sensor);
    return requestOne;
  }

  protected static void addData(String collection, String sensorName)
      throws IOException, SolrServerException {
    List<Map<String, Object>> inputData = new ArrayList<>();
    for (int i = 0; i < 3; ++i) {
      final String name = buildGuid(sensorName, i);
      HashMap<String, Object> inputMap = new HashMap<>();
      inputMap.put("source.type", sensorName);
      inputMap.put(Constants.GUID, name);
      inputData.add(inputMap);
    }
    solrComponent.addDocs(collection, inputData);
  }

  protected static String buildGuid(String sensorName, int i) {
    return "message_" + i + "_" + sensorName;
  }
}
