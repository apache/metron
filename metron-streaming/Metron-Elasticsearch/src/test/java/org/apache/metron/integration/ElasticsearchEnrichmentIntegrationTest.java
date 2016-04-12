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
package org.apache.metron.integration;

import org.apache.metron.integration.util.integration.ComponentRunner;
import org.apache.metron.integration.util.integration.InMemoryComponent;
import org.apache.metron.integration.util.integration.Processor;
import org.apache.metron.integration.util.integration.ReadinessState;
import org.apache.metron.integration.util.integration.components.ElasticSearchComponent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ElasticsearchEnrichmentIntegrationTest extends EnrichmentIntegrationTest {

  private String indexDir = "target/elasticsearch";
  private String dateFormat = "yyyy.MM.dd.HH";
  private String index = "yaf_index_" + new SimpleDateFormat(dateFormat).format(new Date());

  @Override
  InMemoryComponent getSearchComponent(final Properties topologyProperties) {
    return new ElasticSearchComponent.Builder()
            .withHttpPort(9211)
            .withIndexDir(new File(indexDir))
            .build();
  }

  @Override
  Processor<List<Map<String, Object>>> getProcessor(final List<byte[]> inputMessages) {
    return new Processor<List<Map<String, Object>>>() {
      List<Map<String, Object>> docs = null;
      public ReadinessState process(ComponentRunner runner) {
        ElasticSearchComponent elasticSearchComponent = runner.getComponent("search", ElasticSearchComponent.class);
        if (elasticSearchComponent.hasIndex(index)) {
          List<Map<String, Object>> docsFromDisk;
          try {
            docs = elasticSearchComponent.getAllIndexedDocs(index, "yaf_doc");
            docsFromDisk = readDocsFromDisk(hdfsDir);
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
  void setAdditionalProperties(Properties topologyProperties) {
    topologyProperties.setProperty("writer.class.name", "org.apache.metron.writer.ElasticsearchWriter");
  }

  @Override
  public String cleanField(String field) {
    return field;
  }
}
