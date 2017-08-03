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

package org.apache.metron.elasticsearch.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class ElasticsearchMetaAlertDaoTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testBuildUpdatedMetaAlertSingleAlert() throws IOException, ParseException {
    // Construct the meta alert object
    Map<String, Object> metaSource = new HashMap<>();
    metaSource.put("guid", "m1");
    metaSource.putAll(new MetaScores(new ArrayList<Double>() {{
      add(10d);
    }}).getMetaScores());
    SearchHit metaHit = mock(SearchHit.class);
    when(metaHit.getSource()).thenReturn(metaSource);

    // Construct the inner alert
    SearchHit innerAlertHit = mock(SearchHit.class);
    when(innerAlertHit.getId()).thenReturn("a1");
    SearchHitField field = mock(SearchHitField.class);
    when(field.getValue()).thenReturn(10);
    when(innerAlertHit.field("threat.triage.level")).thenReturn(field);
    SearchHit[] innerHitArray = new SearchHit[1];
    innerHitArray[0] = innerAlertHit;

    // Construct the inner hits that contains the alert
    SearchHits searchHits = mock(SearchHits.class);
    when(searchHits.getHits()).thenReturn(innerHitArray);
    Map<String, SearchHits> innerHits = new HashMap<>();
    innerHits.put("alert", searchHits);
    when(metaHit.getInnerHits()).thenReturn(innerHits);

    // Construct  the updated Document
    Map<String, Object> updateMap = new HashMap<>();
    updateMap.put("threat.triage.level", 5);
    updateMap.put("fakekey", "fakevalue");
    Document update = new Document(updateMap, "a1", "bro_doc", 0L);

    ElasticsearchDao esDao = new ElasticsearchDao();
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao(esDao, "metaalerts");
    XContentBuilder builder = emaDao.buildUpdatedMetaAlert(update, metaHit);

    JSONParser parser = new JSONParser();
    Object obj = parser.parse(builder.string());
    JSONObject actual = (JSONObject) obj;

    JSONObject expected = new JSONObject();
    expected.put("average", 5.0);
    expected.put("min", 5.0);
    expected.put("median", 5.0);
    expected.put("max", 5.0);
    expected.put("count", 1L);
    expected.put("guid", "m1");
    expected.put("sum", 5.0);
    JSONArray expectedAlerts = new JSONArray();
    JSONObject expectedAlert = new JSONObject();
    expectedAlert.put("threat.triage.level", 5L);
    expectedAlert.put("fakekey", "fakevalue");
    expectedAlerts.add(expectedAlert);
    expected.put("alert", expectedAlerts);

    assertEquals(expected, actual);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBuildUpdatedMetaAlertMultipleAlerts() throws IOException, ParseException {
    // Construct the meta alert object
    Map<String, Object> metaSource = new HashMap<>();
    metaSource.put("guid", "m1");
    metaSource.putAll(new MetaScores(new ArrayList<Double>() {{
      add(10d);
      add(10d);
    }}).getMetaScores());
    SearchHit metaHit = mock(SearchHit.class);
    when(metaHit.getSource()).thenReturn(metaSource);

    // Construct the inner alerts
    SearchHit innerAlertHitOne = mock(SearchHit.class);
    when(innerAlertHitOne.getId()).thenReturn("a1");
    SearchHitField triageOne = mock(SearchHitField.class);
    when(triageOne.getValue()).thenReturn(10);
    when(innerAlertHitOne.field("threat.triage.level")).thenReturn(triageOne);

    SearchHit innerAlertHitTwo = mock(SearchHit.class);
    when(innerAlertHitTwo.getId()).thenReturn("a2");
    SearchHitField triageTwo = mock(SearchHitField.class);
    when(triageTwo.getValue()).thenReturn(10);
    Map<String, Object> innerAlertHitTwoSource = new HashMap<>();
    innerAlertHitTwoSource.put("threat.triage.level", 10);
    innerAlertHitTwoSource.put("guid", "a2");
    when(innerAlertHitTwo.getSource()).thenReturn(innerAlertHitTwoSource);
    when(innerAlertHitTwo.field("threat.triage.level")).thenReturn(triageTwo);

    SearchHit[] innerHitArray = new SearchHit[2];
    innerHitArray[0] = innerAlertHitOne;
    innerHitArray[1] = innerAlertHitTwo;

    // Construct the inner hits that contains the alert
    SearchHits searchHits = mock(SearchHits.class);
    when(searchHits.getHits()).thenReturn(innerHitArray);
    Map<String, SearchHits> innerHits = new HashMap<>();
    innerHits.put("alert", searchHits);
    when(metaHit.getInnerHits()).thenReturn(innerHits);

    // Construct  the updated Document
    Map<String, Object> updateMap = new HashMap<>();
    updateMap.put("threat.triage.level", 5);
    updateMap.put("fakekey", "fakevalue");
    Document update = new Document(updateMap, "a1", "bro_doc", 0L);

    ElasticsearchDao esDao = new ElasticsearchDao();
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao(esDao, "metaalerts");
    XContentBuilder builder = emaDao.buildUpdatedMetaAlert(update, metaHit);

    JSONParser parser = new JSONParser();
    Object obj = parser.parse(builder.string());
    JSONObject actual = (JSONObject) obj;

    JSONObject expected = new JSONObject();
    expected.put("average", 7.5);
    expected.put("min", 5.0);
    expected.put("median", 7.5);
    expected.put("max", 10.0);
    expected.put("count", 2L);
    expected.put("guid", "m1");
    expected.put("sum", 15.0);
    JSONArray expectedAlerts = new JSONArray();
    JSONObject expectedAlertOne = new JSONObject();
    expectedAlertOne.put("threat.triage.level", 5L);
    expectedAlertOne.put("fakekey", "fakevalue");
    expectedAlerts.add(expectedAlertOne);
    JSONObject expectedAlertTwo = new JSONObject();
    expectedAlertTwo.put("threat.triage.level", 10L);
    expectedAlertTwo.put("guid", "a2");
    expectedAlerts.add(expectedAlertTwo);
    expected.put("alert", expectedAlerts);

   assertEquals(expected, actual);
  }
}
