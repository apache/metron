
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

package org.apache.metron.elasticsearch.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.dao.ElasticsearchMetaAlertDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertIntegrationTest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SortField;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.metron.elasticsearch.dao.ElasticsearchMetaAlertDao.METAALERTS_INDEX;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ElasticsearchMetaAlertIntegrationTest extends MetaAlertIntegrationTest {

  private static IndexDao esDao;
  private static ElasticSearchComponent es;
  private static AccessConfig accessConfig;

  protected static final String INDEX_DIR = "target/elasticsearch_meta";
  private static String POSTFIX= new SimpleDateFormat(DATE_FORMAT).format(new Date());
  private static final String INDEX_RAW = SENSOR_NAME + POSTFIX;
  protected static final String INDEX = INDEX_RAW + "_index";

  public static List<Object[]> data() {
    Function<List<String>, List<String>> asteriskTransform = x -> ImmutableList.of("*");
    Function<List<String>, List<String>> explicitTransform =
            allIndices -> allIndices.stream().map(x -> x.replace("_index", ""))
                       .collect(Collectors.toCollection(ArrayList::new));
    return Arrays.asList(new Object[][]{
            {asteriskTransform},
            {explicitTransform}
            }
    );
  }

  /**
   {
     "properties": {
       "metron_alert": { "type": "nested" }
     }
   }
   */
  @Multiline
  public static String nestedAlertMapping;

  /**
   * {
       "%MAPPING_NAME%_doc" : {
         "properties" : {
           "guid" : { "type" : "keyword" },
           "ip_src_addr" : { "type" : "keyword" },
           "score" : { "type" : "integer" },
           "metron_alert" : { "type" : "nested" },
           "source:type" : { "type" : "keyword"},
           "alert_status": { "type": "keyword" }
         }
     }
   }
   */
  @Multiline
  public static String template;

  @BeforeAll
  public static void setupBefore() throws Exception {
    // Ensure ES can retry as needed.
    MAX_RETRIES = 10;

    Map<String, Object> globalConfig = new HashMap<String, Object>() {
      {
        put("es.clustername", "metron");
        put("es.port", "9200");
        put("es.ip", "localhost");
        put("es.date.format", DATE_FORMAT);
      }
    };

    accessConfig = new AccessConfig();
    accessConfig.setMaxSearchResults(1000);
    accessConfig.setMaxSearchGroups(100);
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);

    // start elasticsearch
    es = new ElasticSearchComponent.Builder()
            .withHttpPort(9211)
            .withIndexDir(new File(INDEX_DIR))
            .withAccessConfig(accessConfig)
            .build();
    es.start();
  }

  @BeforeEach
  public void setup() throws IOException {
    es.createIndexWithMapping(METAALERTS_INDEX, METAALERT_DOC, template.replace("%MAPPING_NAME%", METAALERT_TYPE));
    es.createIndexWithMapping(INDEX, "test_doc", template.replace("%MAPPING_NAME%", "test"));

    esDao = new ElasticsearchDao();
    esDao.init(accessConfig);

    ElasticsearchMetaAlertDao elasticsearchMetaDao = new ElasticsearchMetaAlertDao(esDao);
    elasticsearchMetaDao.setPageSize(5);
    metaDao = elasticsearchMetaDao;
  }

  @AfterAll
  public static void teardown() {
    if (es != null) {
      es.stop();
    }
  }

  @AfterEach
  public void reset() {
    es.reset();
  }

  public void shouldSearchByNestedAlert() {
     // Do nothing. ES has multiple index patterns, so this will be ignored in favor of a parameterized test.
  };

  @ParameterizedTest
  @MethodSource("data")
  public void shouldSearchByNestedAlert(Function<List<String>, List<String>> indexTransform) throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(4);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(0).put("ip_src_addr", "192.168.1.1");
    alerts.get(0).put("ip_src_port", 8010);
    alerts.get(1).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(1).put("ip_src_addr", "192.168.1.2");
    alerts.get(1).put("ip_src_port", 8009);
    alerts.get(2).put("ip_src_addr", "192.168.1.3");
    alerts.get(2).put("ip_src_port", 8008);
    alerts.get(3).put("ip_src_addr", "192.168.1.4");
    alerts.get(3).put("ip_src_port", 8007);
    addRecords(alerts, INDEX, SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    setupTypings();

    // Load metaAlerts
    Map<String, Object> activeMetaAlert = buildMetaAlert("meta_active", MetaAlertStatus.ACTIVE,
            Optional.of(Arrays.asList(alerts.get(0), alerts.get(1))));
    Map<String, Object> inactiveMetaAlert = buildMetaAlert("meta_inactive",
            MetaAlertStatus.INACTIVE,
            Optional.of(Arrays.asList(alerts.get(2), alerts.get(3))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(Arrays.asList(activeMetaAlert, inactiveMetaAlert), METAALERTS_INDEX,
            METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
            new GetRequest("message_0", SENSOR_NAME),
            new GetRequest("message_1", SENSOR_NAME),
            new GetRequest("message_2", SENSOR_NAME),
            new GetRequest("message_3", SENSOR_NAME),
            new GetRequest("meta_active", METAALERT_TYPE),
            new GetRequest("meta_inactive", METAALERT_TYPE)));

    SearchResponse searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
                "(ip_src_addr:192.168.1.1 AND ip_src_port:8009) OR (metron_alert.ip_src_addr:192.168.1.1 AND metron_alert.ip_src_port:8009)");
        setIndices(Collections.singletonList(METAALERT_TYPE));
        setFrom(0);
        setSize(5);
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });
    // Should not have results because nested alerts shouldn't be flattened
    assertEquals(0, searchResponse.getTotal());

    // Query against all indices. Only the single active meta alert should be returned.
    // The child alerts should be hidden.
    searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
                "(ip_src_addr:192.168.1.1 AND ip_src_port:8010)"
                        + " OR (metron_alert.ip_src_addr:192.168.1.1 AND metron_alert.ip_src_port:8010)");
        setIndices(indexTransform.apply(allIndices));
        setFrom(0);
        setSize(5);
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });

    // Nested query should match a nested alert
    assertEquals(1, searchResponse.getTotal());
    assertEquals("meta_active", searchResponse.getResults().get(0).getSource().get("guid"));

    // Query against all indices. The child alert has no actual attached meta alerts, and should
    // be returned on its own.
    searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
                "(ip_src_addr:192.168.1.3 AND ip_src_port:8008)"
                        + " OR (metron_alert.ip_src_addr:192.168.1.3 AND metron_alert.ip_src_port:8008)");
        setIndices(Collections.singletonList("*"));
        setFrom(0);
        setSize(1);
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });

    // Nested query should match a plain alert
    assertEquals(1, searchResponse.getTotal());
    assertEquals("message_2", searchResponse.getResults().get(0).getSource().get("guid"));
  }

  @Override
  protected long getMatchingAlertCount(String fieldName, Object fieldValue)
          throws IOException, InterruptedException {
    long cnt = 0;
    for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
      List<Map<String, Object>> docs = es
              .getAllIndexedDocs(INDEX, SENSOR_NAME + "_doc");
      cnt = docs
              .stream()
              .filter(d -> {
                Object newfield = d.get(fieldName);
                return newfield != null && newfield.equals(fieldValue);
              }).count();
    }
    return cnt;
  }

  @Override
  protected long getMatchingMetaAlertCount(String fieldName, String fieldValue)
          throws IOException, InterruptedException {
    long cnt = 0;
    for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
      List<Map<String, Object>> docs = es
              .getAllIndexedDocs(METAALERTS_INDEX, METAALERT_DOC);
      cnt = docs
              .stream()
              .filter(d -> {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> alerts = (List<Map<String, Object>>) d
                        .get(ALERT_FIELD);

                for (Map<String, Object> alert : alerts) {
                  Object newField = alert.get(fieldName);
                  if (newField != null && newField.equals(fieldValue)) {
                    return true;
                  }
                }

                return false;
              }).count();
    }
    return cnt;
  }

  @Override
  protected void addRecords(List<Map<String, Object>> inputData, String index, String docType)
          throws IOException, ParseException {
    es.add(index, docType, inputData.stream().map(m -> {
              try {
                return JSONUtils.INSTANCE.toJSON(m, true);
              } catch (JsonProcessingException e) {
                throw new IllegalStateException(e.getMessage(), e);
              }
            }
            ).collect(Collectors.toList())
    );
  }

  @Override
  protected void setupTypings() throws IOException {
    ((ElasticsearchDao) esDao).getClient().putMapping(INDEX, "test_doc", nestedAlertMapping);
  }

  @Override
  protected String getTestIndexName() {
    return INDEX_RAW;
  }

  @Override
  protected String getTestIndexFullName() {
    return INDEX;
  }

  @Override
  protected String getMetaAlertIndex() {
    return METAALERTS_INDEX;
  }

  @Override
  protected String getSourceTypeField() {
    return ElasticsearchMetaAlertDao.SOURCE_TYPE_FIELD;
  }

  @Override
  protected void setEmptiedMetaAlertField(Map<String, Object> docMap) {
    docMap.put(METAALERT_FIELD, new ArrayList<>());
  }

  @Override
  protected boolean isFiniteDoubleOnly() {
    return true;
  }

  @Override
  protected boolean isEmptyMetaAlertList() {
    return true;
  }
}
