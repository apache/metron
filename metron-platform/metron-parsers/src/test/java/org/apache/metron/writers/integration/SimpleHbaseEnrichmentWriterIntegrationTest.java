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
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.integration.components.ConfigUploadComponent;
import org.apache.metron.enrichment.integration.mock.MockTableProvider;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.integration.*;
import org.apache.metron.integration.components.KafkaWithZKComponent;
import org.apache.metron.parsers.integration.components.ParserTopologyComponent;
import org.apache.metron.test.mock.MockHTable;
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

public class SimpleHbaseEnrichmentWriterIntegrationTest extends BaseIntegrationTest {

  /**
   {
    "parserClassName" : "org.apache.metron.parsers.csv.CSVParser"
   ,"writerClassName" : "org.apache.metron.enrichment.writer.SimpleHbaseEnrichmentWriter"
   ,"sensorTopic":"dummy"
   ,"parserConfig":
   {
     "shew.table" : "dummy"
    ,"shew.cf" : "cf"
    ,"shew.keyColumns" : "col2"
    ,"shew.enrichmentType" : "et"
    ,"shew.hbaseProvider" : "org.apache.metron.enrichment.integration.mock.MockTableProvider"
    ,"columns" : {
                "col1" : 0
               ,"col2" : 1
               ,"col3" : 2
                 }
   }
   }
   */
  @Multiline
  public static String parserConfig;

  @Test
  public void test() throws UnableToStartException, IOException {
    final String sensorType = "dummy";
    final List<byte[]> inputMessages = new ArrayList<byte[]>() {{
      add(Bytes.toBytes("col11,col12,col13"));
      add(Bytes.toBytes("col21,col22,col23"));
      add(Bytes.toBytes("col31,col32,col33"));
    }};
    MockTableProvider.addTable(sensorType, "cf");
    final Properties topologyProperties = new Properties();
    final KafkaWithZKComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaWithZKComponent.Topic>() {{
      add(new KafkaWithZKComponent.Topic(sensorType, 1));
    }});
    topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());

    ConfigUploadComponent configUploadComponent = new ConfigUploadComponent()
            .withTopologyProperties(topologyProperties)
            .withGlobalConfigsPath(TestConstants.SAMPLE_CONFIG_PATH)
            .withParserSensorConfig(sensorType, JSONUtils.INSTANCE.load(parserConfig, SensorParserConfig.class));

    ParserTopologyComponent parserTopologyComponent = new ParserTopologyComponent.Builder()
            .withSensorType(sensorType)
            .withTopologyProperties(topologyProperties)
            .withBrokerUrl(kafkaComponent.getBrokerList()).build();

    UnitTestHelper.verboseLogging();
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("kafka", kafkaComponent)
            .withComponent("config", configUploadComponent)
            .withComponent("storm", parserTopologyComponent)
            .withMillisecondsBetweenAttempts(5000)
            .withNumRetries(10)
            .build();
    try {
      runner.start();
      kafkaComponent.writeMessages(sensorType, inputMessages);
      List<LookupKV<EnrichmentKey, EnrichmentValue>> outputMessages =
              runner.process(new Processor<List<LookupKV<EnrichmentKey, EnrichmentValue>>>() {
                List<LookupKV<EnrichmentKey, EnrichmentValue>> messages = null;

                public ReadinessState process(ComponentRunner runner) {
                  MockHTable table = MockTableProvider.getTable(sensorType);
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

                public List<LookupKV<EnrichmentKey, EnrichmentValue>> getResult() {
                  return messages;
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
      for (LookupKV<EnrichmentKey, EnrichmentValue> kv : outputMessages) {
        Assert.assertTrue(validIndicators.contains(kv.getKey().indicator));
        Assert.assertEquals(kv.getValue().getMetadata().get("source.type"), "dummy");
        Assert.assertNotNull(kv.getValue().getMetadata().get("timestamp"));
        Assert.assertNotNull(kv.getValue().getMetadata().get("original_string"));
        Map<String, String> metadata = validMetadata.get(kv.getKey().indicator);
        for (Map.Entry<String, String> x : metadata.entrySet()) {
          Assert.assertEquals(kv.getValue().getMetadata().get(x.getKey()), x.getValue());
        }
        Assert.assertEquals(metadata.size() + 3, kv.getValue().getMetadata().size());
      }
    }
    finally {
      if(runner != null) {
        runner.stop();
      }
    }
  }
}
