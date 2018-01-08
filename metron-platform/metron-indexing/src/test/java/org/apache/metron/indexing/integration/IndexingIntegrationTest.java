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

package org.apache.metron.indexing.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.TestConstants;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.interfaces.FieldNameConverter;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.integration.components.ConfigUploadComponent;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.zookeeper.KeeperException;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.metron.common.configuration.ConfigurationsUtils.getClient;

public abstract class IndexingIntegrationTest extends BaseIntegrationTest {
  protected static final String ERROR_TOPIC = "indexing_error";
  protected String sampleParsedPath = TestConstants.SAMPLE_DATA_PARSED_PATH + "TestExampleParsed";
  protected String testSensorType = "test";
  protected final int NUM_RETRIES = 100;
  protected final long TOTAL_TIME_MS = 150000L;

  protected void preTest() {}


  @Test
  public void test() throws Exception {
    preTest();
    final List<byte[]> inputMessages = TestUtils.readSampleData(sampleParsedPath);
    final Properties topologyProperties = new Properties() {{
      setProperty("indexing_kafka_start", "UNCOMMITTED_EARLIEST");
      setProperty("kafka_security_protocol", "PLAINTEXT");
      setProperty("topology_auto_credentials", "[]");
      setProperty("indexing_workers", "1");
      setProperty("indexing_acker_executors", "0");
      setProperty("indexing_topology_worker_childopts", "");
      setProperty("indexing_topology_max_spout_pending", "");
      setProperty("indexing_input_topic", Constants.INDEXING_TOPIC);
      setProperty("indexing_error_topic", ERROR_TOPIC);
      setProperty("indexing_kafka_spout_parallelism", "1");
      setProperty("indexing_writer_parallelism", "1");
    }};
    setAdditionalProperties(topologyProperties);
    final ZKServerComponent zkServerComponent = getZKServerComponent(topologyProperties);
    final KafkaComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaComponent.Topic>() {{
      add(new KafkaComponent.Topic(Constants.INDEXING_TOPIC, 1));
      add(new KafkaComponent.Topic(ERROR_TOPIC, 1));
    }});
    List<Map<String, Object>> inputDocs = new ArrayList<>();
    for(byte[] b : inputMessages) {
      Map<String, Object> m = JSONUtils.INSTANCE.load(new String(b), new TypeReference<Map<String, Object>>() {});
      inputDocs.add(m);

    }
    final AtomicBoolean isLoaded = new AtomicBoolean(false);
    ConfigUploadComponent configUploadComponent = new ConfigUploadComponent()
            .withTopologyProperties(topologyProperties)
            .withGlobalConfigsPath(TestConstants.SAMPLE_CONFIG_PATH)
            .withEnrichmentConfigsPath(TestConstants.SAMPLE_CONFIG_PATH)
            .withIndexingConfigsPath(TestConstants.SAMPLE_CONFIG_PATH)
            .withPostStartCallback(component -> {
              try {
                waitForIndex(component.getTopologyProperties().getProperty(ZKServerComponent.ZOOKEEPER_PROPERTY));
              } catch (Exception e) {
                e.printStackTrace();
              }
              isLoaded.set(true);
              }
            );

    FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(getFluxPath()))
            .withTopologyName("test")
            .withTemplateLocation(new File(getTemplatePath()))
            .withTopologyProperties(topologyProperties)
            .build();


    ComponentRunner runner = null;
    InMemoryComponent searchComponent = getSearchComponent(topologyProperties);
    ComponentRunner.Builder componentBuilder = new ComponentRunner.Builder();
    componentBuilder = componentBuilder.withComponent("zk", zkServerComponent)
                                       .withComponent("kafka", kafkaComponent)
                                       .withComponent("config", configUploadComponent)
                                       .withComponent("storm", fluxComponent)
                                       .withMillisecondsBetweenAttempts(1500)
                                       .withNumRetries(NUM_RETRIES)
                                       .withMaxTimeMS(TOTAL_TIME_MS);

    if(searchComponent != null) {
     componentBuilder = componentBuilder.withComponent("search", getSearchComponent(topologyProperties))
                                        .withCustomShutdownOrder(new String[]{"search", "storm", "config", "kafka", "zk"})
                      ;
    }
    else {
      componentBuilder = componentBuilder.withCustomShutdownOrder(new String[]{ "storm", "config", "kafka", "zk"})
                      ;
    }
    runner = componentBuilder.build();

    try {
      runner.start();
      while(!isLoaded.get()) {
        Thread.sleep(100);
      }
      fluxComponent.submitTopology();
      kafkaComponent.writeMessages(Constants.INDEXING_TOPIC, inputMessages);
      List<Map<String, Object>> docs = cleanDocs(runner.process(getProcessor(inputMessages)));
      Assert.assertEquals(docs.size(), inputMessages.size());
      //assert that our input docs are equivalent to the output docs, converting the input docs keys based
      // on the field name converter
      assertInputDocsMatchOutputs(inputDocs, docs, getFieldNameConverter());
    }
    finally {
      if(runner != null) {
        runner.stop();
      }
    }
  }

  private void waitForIndex(String zookeeperQuorum) throws Exception {
    try(CuratorFramework client = getClient(zookeeperQuorum)) {
      client.start();
      System.out.println("Waiting for zookeeper...");
      byte[] bytes = null;
      do {
        try {
          bytes = ConfigurationsUtils.readSensorIndexingConfigBytesFromZookeeper(testSensorType, client);
          Thread.sleep(1000);
        }
        catch(KeeperException.NoNodeException nne) {
          //kindly ignore because the path might not exist just yet.
        }
      }
      while(bytes == null || bytes.length == 0);
      System.out.println("Found index config in zookeeper...");
    }
  }

  public List<Map<String, Object>> cleanDocs(ProcessorResult<List<Map<String, Object>>> result) {
    List<Map<String,Object>> docs = result.getResult();
    StringBuffer buffer = new StringBuffer();
    boolean failed = false;
    List<Map<String, Object>> ret = new ArrayList<>();
    if(result.failed()) {
      failed = true;
      result.getBadResults(buffer);
      buffer.append(String.format("%d Valid messages processed", docs.size())).append("\n");
      for (Map<String, Object> doc : docs) {
        Map<String, Object> msg = new HashMap<>();
        for (Map.Entry<String, Object> kv : doc.entrySet()) {
          //for writers like solr who modify the keys, we want to undo that if we can
            buffer.append(cleanField(kv.getKey())).append(kv.getValue().toString()).append("\n");
          }
        }
      Assert.fail(buffer.toString());
    }else {
      for (Map<String, Object> doc : docs) {
        Map<String, Object> msg = new HashMap<>();
        for (Map.Entry<String, Object> kv : doc.entrySet()) {
          //for writers like solr who modify the keys, we want to undo that if we can
          msg.put(cleanField(kv.getKey()), kv.getValue());
        }
        ret.add(msg);
      }
    }
    return ret;
  }

  public void assertInputDocsMatchOutputs( List<Map<String, Object>> inputDocs
                                         , List<Map<String, Object>> indexDocs
                                         , FieldNameConverter converter
                                         )
  {
    for(Map<String, Object> indexDoc : indexDocs) {
      boolean foundMatch = false;
      for(Map<String, Object> doc : inputDocs) {
        if(docMatches(indexDoc, doc, converter)) {
          foundMatch = true;
          break;
        }
      }
      if(!foundMatch) {
        System.err.println("Unable to find: ");
        printMessage(indexDoc);
        dumpMessages("INPUT DOCS:", inputDocs);
      }
      Assert.assertTrue(foundMatch);
    }
  }

  private void printMessage(Map<String, Object> doc) {
    TreeMap<String, Object> d = new TreeMap<>(doc);
      for(Map.Entry<String, Object> kv : d.entrySet()) {
        System.err.println("  " + kv.getKey() + " -> " + kv.getValue());
      }
  }

  private void dumpMessages(String title, List<Map<String, Object>> docs) {
    System.err.println(title);
    int cnt = 0;
    for(Map<String, Object> doc : docs) {
      System.err.println("MESSAGE " + cnt++);
      printMessage(doc);
    }
  }

  boolean docMatches(Map<String, Object> indexedDoc, Map<String, Object> inputDoc, FieldNameConverter converter) {
    String key = "original_string";
    String indexKey = converter.convert(key);
    String originalString = inputDoc.get(key).toString();
    return originalString.equals(indexedDoc.get(indexKey).toString());
  }
  public abstract Processor<List<Map<String, Object>>> getProcessor (List <byte[]>inputMessages);
  public abstract FieldNameConverter getFieldNameConverter();
  public abstract InMemoryComponent getSearchComponent(final Properties topologyProperties) throws Exception;
  public abstract void setAdditionalProperties(Properties topologyProperties);
  public abstract String cleanField(String field);
  public abstract String getTemplatePath();
  public abstract String getFluxPath();
}
