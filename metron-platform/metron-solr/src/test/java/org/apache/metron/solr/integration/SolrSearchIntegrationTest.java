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

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.solr.dao.SolrDao;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SolrSearchIntegrationTest extends SearchIntegrationTest {
  private static SolrComponent solrComponent;
  private static IndexDao dao;

  @BeforeClass
  public static void setupClass() throws Exception {
    indexComponent = startIndex();
    dao = createDao();
    // The data is all static for searches, so we can set it up here, and not do anything between tests.
    broData = SearchIntegrationTest.broData.replace("source:type", "source.type");
    snortData = SearchIntegrationTest.snortData.replace("source:type", "source.type");
    solrComponent.addCollection("bro", "../metron-solr/src/main/config/schema/bro");
    solrComponent.addCollection("snort", "../metron-solr/src/main/config/schema/snort");
    loadTestData();
  }

  @AfterClass
  public static void teardown() {
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
      // Don't test all 256, just test a sample of different fields
      Assert.assertEquals(262, fieldTypes.size());

      // Fields present in both with same type
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("guid"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("source.type"));
      Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));

      // Bro only field
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("username"));

      // A dynamic field present in both with same type
      Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("score"));

      // Dyanamic field present in both with nonstandard types.
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));

      // Field with nonstandard type
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("timestamp"));

      // Bro only field in the dynamic catch all
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("bro_field"));

      // A field is in both bro and snort and they have different types.
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("ttl"));

      // Field only present in Snort
      Assert.assertEquals(null, fieldTypes.get("dgmlen"));

      // Field that doesn't exist
      Assert.assertEquals(null, fieldTypes.get("fake.field"));
    }
    // getColumnMetadata with only snort
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("snort"));
      Assert.assertEquals(32, fieldTypes.size());

      // Fields present in both with same type
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("guid"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("source.type"));
      Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));

      // Snort only field
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("dgmlen"));

      // A dynamic field present in both with same type
      Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("score"));

      // Dyanamic field present in both with nonstandard types.
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));

      // Field with nonstandard type
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("timestamp"));

      // Snort only field in the dynamic catch all
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("snort_field"));

      // A field is in both bro and snort and they have different types.
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ttl"));

      // Field only present in Bro
      Assert.assertEquals(null, fieldTypes.get("username"));

      // Field that doesn't exist
      Assert.assertEquals(null, fieldTypes.get("fake.field"));
    }
  }

  @Override
  @Test
  public void returns_column_data_for_multiple_indices() throws Exception {
    Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Arrays.asList("bro", "snort"));
    // Don't test everything, just test a variety of fields, including fields across collections.

    // Fields present in both with same type
    Assert.assertEquals(FieldType.TEXT, fieldTypes.get("guid"));
    Assert.assertEquals(FieldType.TEXT, fieldTypes.get("source.type"));
    Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
    Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
    Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));

    // Bro only field
    Assert.assertEquals(FieldType.TEXT, fieldTypes.get("username"));

    // Snort only field
    Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("dgmlen"));

    // A dynamic field present in both with same type
    Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("score"));

    // Dyanamic field present in both with nonstandard types.
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));

    // Field present in both with nonstandard type
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("timestamp"));

    // Bro only field in the dynamic catch all
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("bro_field"));

    // Snort only field in the dynamic catch all
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("snort_field"));

    // A field is in both bro and snort and they have different types.
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("ttl"));

    // Field that doesn't exist
    Assert.assertEquals(null, fieldTypes.get("fake.field"));
  }

  @Test
  public void different_type_filter_query() throws Exception {
    thrown.expect(InvalidSearchException.class);
    SearchRequest request = JSONUtils.INSTANCE.load(differentTypeFilterQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
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
