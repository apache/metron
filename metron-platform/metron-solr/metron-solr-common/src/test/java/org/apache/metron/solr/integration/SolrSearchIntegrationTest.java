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
package org.apache.metron.solr.integration;

import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.solr.client.SolrClientFactory;
import org.apache.metron.solr.dao.SolrDao;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.junit.jupiter.api.Assertions.*;

public class SolrSearchIntegrationTest extends SearchIntegrationTest {
  private static SolrComponent solrComponent;
  private static IndexDao dao;

  @BeforeAll
  public static void setupClass() throws Exception {
    indexComponent = startIndex();
    dao = createDao();
    // The data is all static for searches, so we can set it up here, and not do anything between tests.
    broData = SearchIntegrationTest.broData.replace("source:type", "source.type");
    snortData = SearchIntegrationTest.snortData.replace("source:type", "source.type");
    solrComponent.addCollection("bro", "./src/main/config/schema/bro");
    solrComponent.addCollection("snort", "./src/main/config/schema/snort");
    loadTestData();
  }

  @AfterAll
  public static void teardown() {
    SolrClientFactory.close();
    if (solrComponent != null) {
      solrComponent.stop();
    }
  }

  @Override
  public IndexDao getIndexDao() {
    return dao;
  }

  protected static IndexDao createDao() {
    AccessConfig config = new AccessConfig();
    config.setMaxSearchResults(100);
    config.setMaxSearchGroups(100);
    config.setGlobalConfigSupplier( () ->
        new HashMap<String, Object>() {{
          put(SOLR_ZOOKEEPER, solrComponent.getZookeeperUrl());
        }}
    );

    config.setIndexSupplier( sensorType -> sensorType);
    IndexDao dao = new SolrDao();
    dao.init(config);
    return dao;
  }

  protected static InMemoryComponent startIndex() throws Exception {
    solrComponent = new SolrComponent.Builder().build();
    solrComponent.start();
    return solrComponent;
  }

  @SuppressWarnings("unchecked")
  protected static void loadTestData() throws ParseException, IOException, SolrServerException {
    JSONArray broArray = (JSONArray) new JSONParser().parse(broData);
    solrComponent.addDocs("bro", broArray);
    JSONArray snortArray = (JSONArray) new JSONParser().parse(snortData);
    solrComponent.addDocs("snort", snortArray);
  }

  @Override
  @Test
  public void returns_column_metadata_for_specified_indices() throws Exception {
    // getColumnMetadata with only bro
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("bro"));
      // Don't test all fields, just test a sample of different fields
      assertEquals(263, fieldTypes.size());

      // Fields present in both with same type
      assertEquals(FieldType.TEXT, fieldTypes.get("guid"));
      assertEquals(FieldType.TEXT, fieldTypes.get("source.type"));
      assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
      assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));

      // Bro only field
      assertEquals(FieldType.TEXT, fieldTypes.get("username"));

      // A dynamic field present in both with same type
      assertEquals(FieldType.FLOAT, fieldTypes.get("score"));

      // Dyanamic field present in both with nonstandard types.
      assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));

      // Field with nonstandard type
      assertEquals(FieldType.OTHER, fieldTypes.get("timestamp"));

      // Bro only field in the dynamic catch all
      assertEquals(FieldType.TEXT, fieldTypes.get("method"));

      // A field is in both bro and snort and they have different types.
      assertEquals(FieldType.TEXT, fieldTypes.get("ttl"));

      // Field only present in Snort
      assertNull(fieldTypes.get("dgmlen"));

      // Field that doesn't exist
      assertNull(fieldTypes.get("fake.field"));
    }
    // getColumnMetadata with only snort
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("snort"));
      assertEquals(33, fieldTypes.size());

      // Fields present in both with same type
      assertEquals(FieldType.TEXT, fieldTypes.get("guid"));
      assertEquals(FieldType.TEXT, fieldTypes.get("source.type"));
      assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
      assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));

      // Snort only field
      assertEquals(FieldType.INTEGER, fieldTypes.get("dgmlen"));

      // A dynamic field present in both with same type
      assertEquals(FieldType.FLOAT, fieldTypes.get("score"));

      // Dyanamic field present in both with nonstandard types.
      assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));

      // Field with nonstandard type
      assertEquals(FieldType.OTHER, fieldTypes.get("timestamp"));

      // Snort only field in the dynamic catch all
      assertEquals(FieldType.TEXT, fieldTypes.get("sig_generator"));

      // A field is in both bro and snort and they have different types.
      assertEquals(FieldType.INTEGER, fieldTypes.get("ttl"));

      // Field only present in Bro
      assertNull(fieldTypes.get("username"));

      // Field that doesn't exist
      assertNull(fieldTypes.get("fake.field"));
    }
  }

  @Override
  @Test
  public void returns_column_data_for_multiple_indices() throws Exception {
    Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Arrays.asList("bro", "snort"));
    // Don't test everything, just test a variety of fields, including fields across collections.

    // Fields present in both with same type
    assertEquals(FieldType.TEXT, fieldTypes.get("guid"));
    assertEquals(FieldType.TEXT, fieldTypes.get("source.type"));
    assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
    assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
    assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));

    // Bro only field
    assertEquals(FieldType.TEXT, fieldTypes.get("username"));

    // Snort only field
    assertEquals(FieldType.INTEGER, fieldTypes.get("dgmlen"));

    // A dynamic field present in both with same type
    assertEquals(FieldType.FLOAT, fieldTypes.get("score"));

    // Dyanamic field present in both with nonstandard types.
    assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));

    // Field present in both with nonstandard type
    assertEquals(FieldType.OTHER, fieldTypes.get("timestamp"));

    // Bro only field in the dynamic catch all
    assertEquals(FieldType.TEXT, fieldTypes.get("method"));

    // Snort only field in the dynamic catch all
    assertEquals(FieldType.TEXT, fieldTypes.get("sig_generator"));

    // A field is in both bro and snort and they have different types.
    assertEquals(FieldType.OTHER, fieldTypes.get("ttl"));

    // Field that doesn't exist
    assertNull(fieldTypes.get("fake.field"));
  }

  @Test
  @Override
  public void different_type_filter_query() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(differentTypeFilterQuery, SearchRequest.class);
    assertThrows(InvalidSearchException.class, () -> dao.search(request));
  }

  @Override
  protected String getSourceTypeField() {
    return Constants.SENSOR_TYPE;
  }

  @Override
  protected String getIndexName(String sensorType) {
    return sensorType;
  }
}
