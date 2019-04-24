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

package org.apache.metron.writers.integration;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.integration.components.ConfigUploadComponent;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.parsers.integration.components.ParserTopologyComponent;
import org.junit.Assert;
import org.junit.Test;

public class SimpleHbaseEnrichmentWriterIntegrationTest extends BaseIntegrationTest {

  /**
   * {
   *     "parserClassName": "org.apache.metron.parsers.csv.CSVParser",
   *     "writerClassName": "org.apache.metron.writer.hbase.SimpleHbaseEnrichmentWriter",
   *     "sensorTopic": "dummy",
   *     "outputTopic": "output",
   *     "errorTopic": "error",
   *     "parserConfig": {
   *        "shew.table": "dummy",
   *        "shew.cf": "cf",
   *        "shew.keyColumns": "col2",
   *        "shew.enrichmentType": "et",
   *        "shew.hbaseProvider": "org.apache.metron.hbase.mock.MockHBaseTableProvider",
   *        "columns" : {
   *             "col1": 0,
   *             "col2": 1,
   *             "col3": 2
   *        }
   *     }
   * }
   */
  @Multiline
  public static String parserConfigJSON;

  @Test
  public void test() throws UnableToStartException, IOException {
    final String sensorType = "dummy";

    // the input messages to parse
    final List<byte[]> inputMessages = new ArrayList<byte[]>() {{
      add(Bytes.toBytes("col11,col12,col13"));
      add(Bytes.toBytes("col21,col22,col23"));
      add(Bytes.toBytes("col31,col32,col33"));
    }};

    // setup external components; kafka, zookeeper
    MockHBaseTableProvider.addToCache(sensorType, "cf");
    final Properties topologyProperties = new Properties();
    final ZKServerComponent zkServerComponent = getZKServerComponent(topologyProperties);
    final KafkaComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaComponent.Topic>() {{
      add(new KafkaComponent.Topic(sensorType, 1));
    }});
    topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());

    SensorParserConfig parserConfig = JSONUtils.INSTANCE.load(parserConfigJSON, SensorParserConfig.class);

    System.out.println("Workspace: " + System.getProperty("user.dir"));
    System.out.println("Configs path: ../" + TestConstants.SAMPLE_CONFIG_PATH);
    ConfigUploadComponent configUploadComponent = new ConfigUploadComponent()
            .withTopologyProperties(topologyProperties)
            .withGlobalConfigsPath("../" + TestConstants.SAMPLE_CONFIG_PATH)
            .withParserSensorConfig(sensorType, parserConfig);

    ParserTopologyComponent parserTopologyComponent = new ParserTopologyComponent.Builder()
            .withSensorTypes(Collections.singletonList(sensorType))
            .withTopologyProperties(topologyProperties)
            .withBrokerUrl(kafkaComponent.getBrokerList())
            .withOutputTopic(parserConfig.getOutputTopic())
            .build();

    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("zk", zkServerComponent)
            .withComponent("kafka", kafkaComponent)
            .withComponent("config", configUploadComponent)
            .withComponent("org/apache/storm", parserTopologyComponent)
            .withMillisecondsBetweenAttempts(5000)
            .withCustomShutdownOrder(new String[]{"org/apache/storm","config","kafka","zk"})
            .withNumRetries(10)
            .build();
    try {
      runner.start();
      kafkaComponent.writeMessages(sensorType, inputMessages);
      ProcessorResult<List<LookupKV<EnrichmentKey, EnrichmentValue>>> result =
              runner.process(new Processor<List<LookupKV<EnrichmentKey, EnrichmentValue>>>() {
                List<LookupKV<EnrichmentKey, EnrichmentValue>> messages = null;

                @Override
                public ReadinessState process(ComponentRunner runner) {
                  MockHTable table = (MockHTable) MockHBaseTableProvider.getFromCache(sensorType);
                  if (table != null && table.size() == inputMessages.size()) {
                    EnrichmentConverter converter = new EnrichmentConverter();
                    messages = new ArrayList<>();
                    try {
                      for (Result r : table.getScanner(Bytes.toBytes("cf"))) {
                        messages.add(converter.fromResult(r, "cf"));
                      }
                    } catch (IOException e) {
                    }
                    return ReadinessState.READY;
                  }
                  return ReadinessState.NOT_READY;
                }

                @Override
                public ProcessorResult<List<LookupKV<EnrichmentKey, EnrichmentValue>>> getResult() {
                  ProcessorResult.Builder<List<LookupKV<EnrichmentKey,EnrichmentValue>>> builder = new ProcessorResult.Builder();
                  return builder.withResult(messages).build();
                }
              });
      Set<String> validIndicators = new HashSet<>(ImmutableList.of("col12", "col22", "col32"));
      Map<String, Map<String, String>> validMetadata = new HashMap<String, Map<String, String>>() {{
        put("col12", new HashMap<String, String>() {{
          put("col1", "col11");
          put("col3", "col13");
        }});
        put("col22", new HashMap<String, String>() {{
          put("col1", "col21");
          put("col3", "col23");
        }});
        put("col32", new HashMap<String, String>() {{
          put("col1", "col31");
          put("col3", "col33");
        }});
      }};
      for (LookupKV<EnrichmentKey, EnrichmentValue> kv : result.getResult()) {
        Assert.assertTrue(validIndicators.contains(kv.getKey().indicator));
        Assert.assertEquals(kv.getValue().getMetadata().get("source.type"), "dummy");
        Assert.assertNotNull(kv.getValue().getMetadata().get("timestamp"));
        Assert.assertNotNull(kv.getValue().getMetadata().get("original_string"));
        Map<String, String> metadata = validMetadata.get(kv.getKey().indicator);
        for (Map.Entry<String, String> x : metadata.entrySet()) {
          Assert.assertEquals(kv.getValue().getMetadata().get(x.getKey()), x.getValue());
        }
        Assert.assertEquals(metadata.size() + 4, kv.getValue().getMetadata().size());
      }
    }
    finally {
      if(runner != null) {
        runner.stop();
      }
    }
  }
}
