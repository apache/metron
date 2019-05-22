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
package org.apache.metron.elasticsearch.integration;


import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.elasticsearch.client.ElasticsearchClientFactory;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.integration.InMemoryComponent;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ElasticsearchSearchIntegrationTest extends SearchIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static String indexDir = "target/elasticsearch_search";
  private static String dateFormat = "yyyy.MM.dd.HH";
  private static String broTemplatePath = "../../metron-deployment/packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/files/bro_index.template";
  private static String snortTemplatePath = "../../metron-deployment/packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/files/snort_index.template";
  protected static final String BRO_INDEX = "bro_index_2017.01.01.01";
  protected static final String SNORT_INDEX = "snort_index_2017.01.01.02";
  protected static Map<String, Object> globalConfig;
  protected static AccessConfig accessConfig;
  protected static RestClient lowLevelClient;
  protected static RestHighLevelClient highLevelClient;
  protected static IndexDao dao;

  @BeforeClass
  public static void setup() throws Exception {
    globalConfig =  new HashMap<String, Object>() {{
      put("es.clustername", "metron");
      put("es.port", "9200");
      put("es.ip", "localhost");
      put("es.date.format", dateFormat);
    }};

    accessConfig = new AccessConfig();
    accessConfig.setMaxSearchResults(100);
    accessConfig.setMaxSearchGroups(100);
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);

    indexComponent = startIndex();

    ElasticsearchClient esClient = ElasticsearchClientFactory.create(globalConfig);
    lowLevelClient = esClient.getLowLevelClient();
    highLevelClient = esClient.getHighLevelClient();

    dao = new ElasticsearchDao();
    dao.init(accessConfig);

    // The data is all static for searches, so we can set it up beforehand, and it's faster
    loadTestData();
  }

  protected static InMemoryComponent startIndex() throws Exception {
    InMemoryComponent es = new ElasticSearchComponent.Builder()
            .withHttpPort(9211)
            .withIndexDir(new File(indexDir))
            .withAccessConfig(accessConfig)
            .build();
    es.start();
    return es;
  }

  protected static void loadTestData() throws Exception {
    ElasticSearchComponent es = (ElasticSearchComponent) indexComponent;

    // add bro template
    JSONObject broTemplate = JSONUtils.INSTANCE.load(new File(broTemplatePath), JSONObject.class);
    addTestFieldMappings(broTemplate, "bro_doc");
    String broTemplateJson = JSONUtils.INSTANCE.toJSON(broTemplate, true);
    HttpEntity broEntity = new NStringEntity(broTemplateJson, ContentType.APPLICATION_JSON);
    Response response = lowLevelClient.performRequest("PUT", "/_template/bro_template", Collections.emptyMap(), broEntity);
    assertThat(response.getStatusLine().getStatusCode(), equalTo(200));

    // add snort template
    JSONObject snortTemplate = JSONUtils.INSTANCE.load(new File(snortTemplatePath), JSONObject.class);
    addTestFieldMappings(snortTemplate, "snort_doc");
    String snortTemplateJson = JSONUtils.INSTANCE.toJSON(snortTemplate, true);
    HttpEntity snortEntity = new NStringEntity(snortTemplateJson, ContentType.APPLICATION_JSON);
    response = lowLevelClient.performRequest("PUT", "/_template/snort_template", Collections.emptyMap(), snortEntity);
    assertThat(response.getStatusLine().getStatusCode(), equalTo(200));

    // create bro index
    response = lowLevelClient.performRequest("PUT", BRO_INDEX);
    assertThat(response.getStatusLine().getStatusCode(), equalTo(200));

    // create snort index
    response = lowLevelClient.performRequest("PUT", SNORT_INDEX);
    assertThat(response.getStatusLine().getStatusCode(), equalTo(200));

    // write the test documents for Bro
    List<String> broDocuments = new ArrayList<>();
    for (Object broObject: (JSONArray) new JSONParser().parse(broData)) {
      broDocuments.add(((JSONObject) broObject).toJSONString());
    }
    // add documents using Metron GUID
    es.add(BRO_INDEX, "bro", broDocuments.subList(0, 4), true);

    // add a document to the same index but with an Elasticsearch id
    es.add(BRO_INDEX, "bro", broDocuments.subList(4, 5), false);

    // write the test documents for Snort
    List<String> snortDocuments = new ArrayList<>();
    for (Object snortObject: (JSONArray) new JSONParser().parse(snortData)) {
      snortDocuments.add(((JSONObject) snortObject).toJSONString());
    }
    es.add(SNORT_INDEX, "snort", snortDocuments);
  }

  /**
   * Add test fields to a template with defined types in case they are not defined in the sensor template shipped with Metron.
   * This is useful for testing certain cases, for example faceting on fields of various types.
   * Template follows this pattern:
   * { "mappings" : { "xxx_doc" : { "properties" : { ... }}}}
   * @param template - this method has side effects - template is modified with field mappings.
   * @param docType
   */
  private static void addTestFieldMappings(JSONObject template, String docType) {
    Map mappings = (Map) template.get("mappings");
    Map docTypeJSON = (Map) mappings.get(docType);
    Map properties = (Map) docTypeJSON.get("properties");
    Map<String, String> longType = new HashMap<>();
    longType.put("type", "long");
    properties.put("long_field", longType);
    Map<String, String> floatType = new HashMap<>();
    floatType.put("type", "float");
    properties.put("latitude", floatType);
    Map<String, String> doubleType = new HashMap<>();
    doubleType.put("type", "double");
    properties.put("score", doubleType);
  }

  @Test
  public void bad_facet_query_throws_exception() throws Exception {
    thrown.expect(InvalidSearchException.class);
    thrown.expectMessage("Failed to execute search");
    SearchRequest request = JSONUtils.INSTANCE.load(badFacetQuery, SearchRequest.class);
    dao.search(request);
  }

  @Override
  public void returns_column_metadata_for_specified_indices() throws Exception {
    // getColumnMetadata with only bro
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("bro"));
      Assert.assertEquals(262, fieldTypes.size());
      Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("method"));
      Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("ttl"));
      Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("guid"));
      Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("source:type"));
      Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
      Assert.assertEquals(FieldType.DATE, fieldTypes.get("timestamp"));
      Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
      Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("score"));
      Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("location_point"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("metron_alert"));
    }
    // getColumnMetadata with only snort
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("snort"));
      Assert.assertEquals(32, fieldTypes.size());
      Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("sig_generator"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ttl"));
      Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("guid"));
      Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("source:type"));
      Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
      Assert.assertEquals(FieldType.DATE, fieldTypes.get("timestamp"));
      Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
      Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("score"));
      Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("location_point"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ttl"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("metron_alert"));
    }
  }

  @Override
  public void returns_column_data_for_multiple_indices() throws Exception {
    Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Arrays.asList("bro", "snort"));
    Assert.assertEquals(277, fieldTypes.size());

    // Ensure internal Metron fields are properly defined
    Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("guid"));
    Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("source:type"));
    Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("threat:triage:score"));
    Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("alert_status"));
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("metron_alert"));

    Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
    Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
    Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
    Assert.assertEquals(FieldType.DATE, fieldTypes.get("timestamp"));
    Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
    Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("score"));
    Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("suppress_for"));
    Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));

    // Ensure a field defined only in bro is included
    Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("method"));
    // Ensure a field defined only in snort is included
    Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("sig_generator"));
    // Ensure fields in both bro and snort have type OTHER because they have different types
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("ttl"));
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("msg"));
  }

  @Test
  public void throws_exception_on_aggregation_queries_on_non_string_non_numeric_fields()
      throws Exception {
    thrown.expect(InvalidSearchException.class);
    thrown.expectMessage("Failed to execute search");
    GroupRequest request = JSONUtils.INSTANCE.load(badGroupQuery, GroupRequest.class);
    dao.group(request);
  }

  @Test
  public void different_type_filter_query() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(differentTypeFilterQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
    Assert.assertEquals(1, response.getTotal());
    List<SearchResult> results = response.getResults();
    Assert.assertEquals("bro", results.get(0).getSource().get("source:type"));
    Assert.assertEquals("data 1", results.get(0).getSource().get("ttl"));
  }

  @Override
  protected String getSourceTypeField() {
    return Constants.SENSOR_TYPE.replace('.', ':');
  }

  @Override
  protected IndexDao getIndexDao() {
    return dao;
  }

  @Override
  protected String getIndexName(String sensorType) {
    if ("bro".equals(sensorType)) {
      return BRO_INDEX;
    } else {
      return SNORT_INDEX;
    }
  }
}
