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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;

public class SolrSearchIntegrationTest extends SearchIntegrationTest {

  private SolrComponent solrComponent;

  @Override
  protected IndexDao createDao() throws Exception {
    AccessConfig config = new AccessConfig();
    config.setMaxSearchResults(100);
    config.setMaxSearchGroups(100);
    config.setGlobalConfigSupplier( () ->
        new HashMap<String, Object>() {{
          put("solr.zookeeper", solrComponent.getZookeeperUrl());
        }}
    );

    IndexDao dao = new SolrDao();
    dao.init(config);
    return dao;
  }

  @Override
  protected InMemoryComponent startIndex() throws Exception {
    solrComponent = new SolrComponent.Builder()
        .addCollection("bro", "../metron-solr/src/test/resources/config/bro/conf")
        .addCollection("snort", "../metron-solr/src/test/resources/config/snort/conf")
        .build();
    solrComponent.start();
    return solrComponent;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadTestData() throws Exception {
    CloudSolrClient solrClient = solrComponent.getSolrClient();
    JSONArray broArray = (JSONArray) new JSONParser().parse(broData);
    solrComponent.addDocs("bro", broArray);
    solrClient.setDefaultCollection("bro");
    solrClient.commit();
    JSONArray snortArray = (JSONArray) new JSONParser().parse(snortData);
    solrComponent.addDocs("snort", snortArray);
    solrClient.setDefaultCollection("snort");
    solrClient.commit();
  }

  @Override
  public void returns_column_metadata_for_specified_indices() throws Exception {
    // getColumnMetadata with only bro
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("bro"));
      Assert.assertEquals(12, fieldTypes.size());
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("bro_field"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("duplicate_name_field"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("guid"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("source:type"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
      Assert.assertEquals(FieldType.LONG, fieldTypes.get("timestamp"));
      Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
      Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("score"));
      Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("bro_field"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("duplicate_name_field"));
    }
    // getColumnMetadata with only snort
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("snort"));
      Assert.assertEquals(13, fieldTypes.size());
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("snort_field"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("duplicate_name_field"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("guid"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("source:type"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
      Assert.assertEquals(FieldType.LONG, fieldTypes.get("timestamp"));
      Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
      Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("score"));
      Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("duplicate_name_field"));
    }
  }

  @Override
  public void returns_column_data_for_multiple_indices() throws Exception {
    Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Arrays.asList("bro", "snort"));
    Assert.assertEquals(14, fieldTypes.size());
    Assert.assertEquals(FieldType.TEXT, fieldTypes.get("guid"));
    Assert.assertEquals(FieldType.TEXT, fieldTypes.get("source:type"));
    Assert.assertEquals(FieldType.TEXT, fieldTypes.get("ip_src_addr"));
    Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
    Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
    Assert.assertEquals(FieldType.LONG, fieldTypes.get("timestamp"));
    Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
    Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("score"));
    Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));
    Assert.assertEquals(FieldType.TEXT, fieldTypes.get("bro_field"));
    Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("snort_field"));
    //NOTE: This is because the field is in both bro and snort and they have different types.
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("duplicate_name_field"));
    Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("threat:triage:score"));
  }

  @Test
  public void different_type_filter_query() throws Exception {
    thrown.expect(InvalidSearchException.class);
    SearchRequest request = JSONUtils.INSTANCE.load(differentTypeFilterQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
  }
}
