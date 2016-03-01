/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.integration;

import com.google.common.base.Function;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.Constants;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.integration.util.TestUtils;
import org.apache.metron.integration.util.UnitTestHelper;
import org.apache.metron.integration.util.integration.ComponentRunner;
import org.apache.metron.integration.util.integration.Processor;
import org.apache.metron.integration.util.integration.ReadinessState;
import org.apache.metron.integration.util.integration.components.ElasticSearchComponent;
import org.apache.metron.integration.util.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.util.integration.components.KafkaWithZKComponent;
import org.apache.metron.integration.util.mock.MockHTable;
import org.apache.metron.integration.util.threatintel.ThreatIntelHelper;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.utils.SourceConfigUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

public class EnrichmentIntegrationTest {

  private String fluxPath = "src/main/resources/Metron_Configs/topologies/enrichment/test.yaml";
  private String indexDir = "target/elasticsearch";
  private String sampleParsedPath = "src/main/resources/SampleParsed/YafExampleParsed";
  private String sampleIndexedPath = "src/main/resources/SampleIndexed/YafIndexed";
  private Map<String, String> sourceConfigs = new HashMap<>();

  public static class Provider implements TableProvider, Serializable {
    MockHTable.Provider  provider = new MockHTable.Provider();
    @Override
    public HTableInterface getTable(Configuration config, String tableName) throws IOException {
      return provider.getTable(config, tableName);
    }
  }


  @Test
  public void test() throws Exception {
    final String dateFormat = "yyyy.MM.dd.hh";
    final String index = "yaf_" + new SimpleDateFormat(dateFormat).format(new Date());
    String yafConfig = "{\n" +
            "  \"index\": \"yaf\",\n" +
            "  \"batchSize\": 5,\n" +
            "  \"enrichmentFieldMap\":\n" +
            "  {\n" +
            "    \"geo\": [\"sip\", \"dip\"],\n" +
            "    \"host\": [\"sip\", \"dip\"]\n" +
            "  },\n" +
            "  \"threatIntelFieldMap\":\n" +
            "  {\n" +
            "    \"ip\": [\"sip\", \"dip\"]\n" +
            "  }\n" +
            "}";
    sourceConfigs.put("yaf", yafConfig);
    final List<byte[]> inputMessages = TestUtils.readSampleData(sampleParsedPath);
    final String cf = "cf";
    final String trackerHBaseTable = "tracker";
    final String ipThreatIntelTable = "ip_threat_intel";
    final Properties topologyProperties = new Properties() {{
      setProperty("org.apache.metron.enrichment.host.known_hosts", "[{\"ip\":\"10.1.128.236\", \"local\":\"YES\", \"type\":\"webserver\", \"asset_value\" : \"important\"},\n" +
              "{\"ip\":\"10.1.128.237\", \"local\":\"UNKNOWN\", \"type\":\"unknown\", \"asset_value\" : \"important\"},\n" +
              "{\"ip\":\"10.60.10.254\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"},\n" +
              "{\"ip\":\"10.0.2.15\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"}]");
      setProperty("hbase.provider.impl","" + Provider.class.getName());
      setProperty("threat.intel.tracker.table", trackerHBaseTable);
      setProperty("threat.intel.tracker.cf", cf);
      setProperty("threat.intel.ip.table", ipThreatIntelTable);
      setProperty("threat.intel.ip.cf", cf);
      setProperty("es.clustername", "metron");
      setProperty("es.port", "9300");
      setProperty("es.ip", "localhost");
      setProperty("index.date.format", dateFormat);
    }};
    final KafkaWithZKComponent kafkaComponent = new KafkaWithZKComponent().withTopics(new ArrayList<KafkaWithZKComponent.Topic>() {{
      add(new KafkaWithZKComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
    }})
            .withPostStartCallback(new Function<KafkaWithZKComponent, Void>() {
              @Nullable
              @Override
              public Void apply(@Nullable KafkaWithZKComponent kafkaWithZKComponent) {
                topologyProperties.setProperty("kafka.zk", kafkaWithZKComponent.getZookeeperConnect());
                try {
                  for(String sourceType: sourceConfigs.keySet()) {
                    SourceConfigUtils.writeToZookeeper(sourceType, sourceConfigs.get(sourceType).getBytes(), kafkaWithZKComponent.getZookeeperConnect());
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }
            });

    ElasticSearchComponent esComponent = new ElasticSearchComponent.Builder()
            .withHttpPort(9211)
            .withIndexDir(new File(indexDir))
            .build();

    //create MockHBaseTables
    final MockHTable trackerTable = (MockHTable)MockHTable.Provider.addToCache(trackerHBaseTable, cf);
    final MockHTable ipTable = (MockHTable)MockHTable.Provider.addToCache(ipThreatIntelTable, cf);
    ThreatIntelHelper.INSTANCE.load(ipTable, cf, new ArrayList<LookupKV<ThreatIntelKey, ThreatIntelValue>>(){{
      add(new LookupKV<>(new ThreatIntelKey("10.0.2.3"), new ThreatIntelValue(new HashMap<String, String>())));
    }});

    FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(fluxPath))
            .withTopologyName("test")
            .withTopologyProperties(topologyProperties)
            .build();

    UnitTestHelper.verboseLogging();
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("kafka", kafkaComponent)
            .withComponent("elasticsearch", esComponent)
            .withComponent("storm", fluxComponent)
            .withTimeBetweenAttempts(10000)
            .build();
    runner.start();
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(Constants.ENRICHMENT_TOPIC, inputMessages);
    List<Map<String, Object>> docs =
            runner.process(new Processor<List<Map<String, Object>>> () {
              List<Map<String, Object>> docs = null;
              public ReadinessState process(ComponentRunner runner){
                ElasticSearchComponent elasticSearchComponent = runner.getComponent("elasticsearch", ElasticSearchComponent.class);
                if(elasticSearchComponent.hasIndex(index)) {
                  try {
                    docs = elasticSearchComponent.getAllIndexedDocs(index, "yaf");
                  } catch (IOException e) {
                    throw new IllegalStateException("Unable to retrieve indexed documents.", e);
                  }
                  if(docs.size() < inputMessages.size()) {
                    return ReadinessState.NOT_READY;
                  }
                  else {
                    return ReadinessState.READY;
                  }
                }
                else {
                  return ReadinessState.NOT_READY;
                }
              }

              public List<Map<String, Object>> getResult() {
                return docs;
              }
            });

    List<byte[]> sampleIndexedMessages = TestUtils.readSampleData(sampleIndexedPath);
    Assert.assertEquals(sampleIndexedMessages.size(), docs.size());
    for (int i = 0; i < docs.size(); i++) {
      String doc = docs.get(i).toString();
      String sampleIndexedMessage = new String(sampleIndexedMessages.get(i));
      assertEqual(sampleIndexedMessage, doc);
    }
    runner.stop();
  }
  public static void assertEqual(String doc1, String doc2) {
    Assert.assertEquals(doc1.length(), doc2.length());
    char[] c1 = doc1.toCharArray();
    Arrays.sort(c1);
    char[] c2 = doc2.toCharArray();
    Arrays.sort(c2);
    Assert.assertArrayEquals(c1, c2);
  }
}
