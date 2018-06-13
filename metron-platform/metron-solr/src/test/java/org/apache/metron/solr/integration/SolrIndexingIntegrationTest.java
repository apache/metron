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

import com.google.common.base.Function;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.field.FieldNameConverter;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.integration.utils.SampleUtil;
import org.apache.metron.indexing.integration.IndexingIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.solr.integration.components.SolrComponent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SolrIndexingIntegrationTest extends IndexingIntegrationTest {

  private String collection = "metron";
  private FieldNameConverter fieldNameConverter = fieldName -> fieldName;
  @Override
  public FieldNameConverter getFieldNameConverter() {
    return fieldNameConverter;
  }

  @Override
  public InMemoryComponent getSearchComponent(final Properties topologyProperties) throws Exception {
    SolrComponent solrComponent = new SolrComponent.Builder()
            .addCollection(collection, "../metron-solr/src/test/resources/solr/conf")
            .withPostStartCallback(new Function<SolrComponent, Void>() {
              @Nullable
              @Override
              public Void apply(@Nullable SolrComponent solrComponent) {
                topologyProperties.setProperty("solr.zk", solrComponent.getZookeeperUrl());
                try {
                  String testZookeeperUrl = topologyProperties.getProperty(ZKServerComponent.ZOOKEEPER_PROPERTY);
                  Configurations configurations = SampleUtil.getSampleConfigs();
                  Map<String, Object> globalConfig = configurations.getGlobalConfig();
                  globalConfig.put("solr.zookeeper", solrComponent.getZookeeperUrl());
                  ConfigurationsUtils.writeGlobalConfigToZookeeper(JSONUtils.INSTANCE.toJSONPretty(globalConfig), testZookeeperUrl);
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }
            })
            .build();
    return solrComponent;
  }

  @Override
  public Processor<List<Map<String, Object>>> getProcessor(final List<byte[]> inputMessages) {
    return new Processor<List<Map<String, Object>>>() {
      List<Map<String, Object>> docs = null;
      List<byte[]> errors = null;
      @Override
      public ReadinessState process(ComponentRunner runner) {
        SolrComponent solrComponent = runner.getComponent("search", SolrComponent.class);
        KafkaComponent kafkaComponent = runner.getComponent("kafka", KafkaComponent.class);
        if (solrComponent.hasCollection(collection)) {
          docs = solrComponent.getAllIndexedDocs(collection);
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
      public ProcessorResult<List<Map<String, Object>>> getResult() {
        ProcessorResult.Builder<List<Map<String,Object>>> builder = new ProcessorResult.Builder();
        return builder.withResult(docs).withProcessErrors(errors).build();
      }
    };
  }

  @Override
  public void setAdditionalProperties(Properties topologyProperties) {
    topologyProperties.setProperty("ra_indexing_writer_class_name", "org.apache.metron.solr.writer.SolrWriter");
    topologyProperties.setProperty("ra_indexing_kafka_start", "UNCOMMITTED_EARLIEST");
    topologyProperties.setProperty("ra_indexing_workers", "1");
    topologyProperties.setProperty("ra_indexing_acker_executors", "0");
    topologyProperties.setProperty("ra_indexing_topology_max_spout_pending", "");
    topologyProperties.setProperty("ra_indexing_kafka_spout_parallelism", "1");
    topologyProperties.setProperty("ra_indexing_writer_parallelism", "1");
  }

  @Override
  public String cleanField(String field) {
    return field.replaceFirst("_[dfils]$", "");
  }

  @Override
  public String getTemplatePath() {
    return "../metron-solr/src/main/config/solr.properties.j2";
  }

  @Override
  public String getFluxPath() {
    return "../metron-indexing/src/main/flux/indexing/random_access/remote.yaml";
  }
}
