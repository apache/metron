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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.interfaces.FieldNameConverter;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.elasticsearch.writer.ElasticsearchFieldNameConverter;
import org.apache.metron.indexing.integration.IndexingIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.KafkaComponent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class ElasticsearchIndexingIntegrationTest extends IndexingIntegrationTest {

  private String indexDir = "target/elasticsearch";
  private String dateFormat = "yyyy.MM.dd.HH";
  private String index = "yaf_index_" + new SimpleDateFormat(dateFormat).format(new Date());
  private FieldNameConverter fieldNameConverter = new ElasticsearchFieldNameConverter();
  /**
   * {
   * "yaf_doc": {
   *   "properties": {
   *     "source:type": { "type": "keyword" },
   *     "guid": { "type": "keyword" },
   *     "isn": { "type": "text" }
   *   }
   * }
   * }
   */
  @Multiline
  private static String mapping;


  @Override
  public FieldNameConverter getFieldNameConverter() {
    return fieldNameConverter;
  }

  @Override
  public InMemoryComponent getSearchComponent(final Properties topologyProperties) {
    return new ElasticSearchComponent.Builder()
            .withHttpPort(9211)
            .withIndexDir(new File(indexDir))
            .withMapping(index, "yaf_doc", mapping)
            .build();
  }

  @Override
  public Processor<List<Map<String, Object>>> getProcessor(final List<byte[]> inputMessages) {
    return new Processor<List<Map<String, Object>>>() {
      List<Map<String, Object>> docs = null;
      List<byte[]> errors = null;
      final AtomicInteger missCount = new AtomicInteger(0);
      @Override
      public ReadinessState process(ComponentRunner runner) {
        ElasticSearchComponent elasticSearchComponent = runner.getComponent("search", ElasticSearchComponent.class);
        KafkaComponent kafkaComponent = runner.getComponent("kafka", KafkaComponent.class);
        if (elasticSearchComponent.hasIndex(index)) {
          try {
            docs = elasticSearchComponent.getAllIndexedDocs(index, testSensorType + "_doc");
          } catch (IOException e) {
            throw new IllegalStateException("Unable to retrieve indexed documents.", e);
          }
          if (docs.size() < inputMessages.size() ) {
            errors = kafkaComponent.readMessages(ERROR_TOPIC);
            if(errors.size() > 0 && errors.size() + docs.size() == inputMessages.size()){
              return ReadinessState.READY;
            }
            return ReadinessState.NOT_READY;
          } else {
            return ReadinessState.READY;
          }
        } else {
          return ReadinessState.NOT_READY;
        }
      }

      @Override
      public ProcessorResult<List<Map<String, Object>>> getResult()  {
        ProcessorResult.Builder<List<Map<String,Object>>> builder = new ProcessorResult.Builder();
        return builder.withResult(docs).withProcessErrors(errors).build();
      }
    };
  }

  @Override
  public void setAdditionalProperties(Properties topologyProperties) {
    topologyProperties.setProperty("es.clustername", "metron");
    topologyProperties.setProperty("es.port", "9300");
    topologyProperties.setProperty("es.ip", "localhost");
    topologyProperties.setProperty("ra_indexing_writer_class_name", "org.apache.metron.elasticsearch.writer.ElasticsearchWriter");
    topologyProperties.setProperty("ra_indexing_kafka_start", "UNCOMMITTED_EARLIEST");
    topologyProperties.setProperty("ra_indexing_workers", "1");
    topologyProperties.setProperty("ra_indexing_acker_executors", "0");
    topologyProperties.setProperty("ra_indexing_topology_max_spout_pending", "");
    topologyProperties.setProperty("ra_indexing_kafka_spout_parallelism", "1");
    topologyProperties.setProperty("ra_indexing_writer_parallelism", "1");
  }

  @Override
  public String cleanField(String field) {
    return field;
  }

  @Override
  public String getTemplatePath() {
    return "../metron-elasticsearch/src/main/config/elasticsearch.properties.j2";
  }

  @Override
  public String getFluxPath() {
    return "../metron-indexing/src/main/flux/indexing/random_access/remote.yaml";
  }
}
