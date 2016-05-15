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
import org.apache.metron.integration.EnrichmentIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.apache.metron.integration.utils.SampleUtil;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.utils.JSONUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SolrEnrichmentIntegrationTest extends EnrichmentIntegrationTest {

  private String collection = "metron";
  private String solrZookeeperUrl;

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
                  String testZookeeperUrl = topologyProperties.getProperty("kafka.zk");
                  Configurations configurations = SampleUtil.getSampleConfigs();
                  Map<String, Object> globalConfig = configurations.getGlobalConfig();
                  globalConfig.put("solr.zookeeper", solrComponent.getZookeeperUrl());
                  ConfigurationsUtils.writeGlobalConfigToZookeeper(JSONUtils.INSTANCE.toJSON(globalConfig), testZookeeperUrl);
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
      public ReadinessState process(ComponentRunner runner) {
        SolrComponent solrComponent = runner.getComponent("search", SolrComponent.class);
        if (solrComponent.hasCollection(collection)) {
          List<Map<String, Object>> docsFromDisk;
          try {
            docs = solrComponent.getAllIndexedDocs(collection);
            docsFromDisk = EnrichmentIntegrationTest.readDocsFromDisk(hdfsDir);
            System.out.println(docs.size() + " vs " + inputMessages.size() + " vs " + docsFromDisk.size());
          } catch (IOException e) {
            throw new IllegalStateException("Unable to retrieve indexed documents.", e);
          }
          if (docs.size() < inputMessages.size() || docs.size() != docsFromDisk.size()) {
            return ReadinessState.NOT_READY;
          } else {
            return ReadinessState.READY;
          }
        } else {
          return ReadinessState.NOT_READY;
        }
      }

      public List<Map<String, Object>> getResult() {
        return docs;
      }
    };
  }

  @Override
  public void setAdditionalProperties(Properties topologyProperties) {
    topologyProperties.setProperty("writer.class.name", "org.apache.metron.solr.writer.SolrWriter");
  }

  @Override
  public String cleanField(String field) {
    return field.replaceFirst("_[dfils]$", "");
  }
}
