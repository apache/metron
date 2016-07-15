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
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Iterables;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.field.validation.FieldValidation;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.integration.components.ConfigUploadComponent;
import org.apache.metron.integration.*;
import org.apache.metron.integration.components.KafkaWithZKComponent;
import org.apache.metron.parsers.integration.components.ParserTopologyComponent;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

public class WriterBoltIntegrationTest extends BaseIntegrationTest {
  public static class MockValidator implements FieldValidation{

    @Override
    public boolean isValid(Map<String, Object> input, Map<String, Object> validationConfig, Map<String, Object> globalConfig) {
      if(input.get("action").equals("invalid")) {
        return false;
      }
      return true;
    }

    @Override
    public void initialize(Map<String, Object> validationConfig, Map<String, Object> globalConfig) {
    }
  }
  /**
   {
    "fieldValidations" : [
        {
          "validation" : "org.apache.metron.writers.integration.WriterBoltIntegrationTest$MockValidator"
        }
                         ]
   }
    */
  @Multiline
  public static String globalConfig;

  /**
   {
    "parserClassName" : "org.apache.metron.parsers.csv.CSVParser"
   ,"sensorTopic":"dummy"
   ,"parserConfig":
   {
    "columns" : {
                "action" : 0
               ,"dummy" : 1
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
      add(Bytes.toBytes("valid,foo"));
      add(Bytes.toBytes("invalid,foo"));
      add(Bytes.toBytes("error"));
    }};
    final Properties topologyProperties = new Properties();
    final KafkaWithZKComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaWithZKComponent.Topic>() {{
      add(new KafkaWithZKComponent.Topic(sensorType, 1));
      add(new KafkaWithZKComponent.Topic(Constants.DEFAULT_PARSER_ERROR_TOPIC, 1));
      add(new KafkaWithZKComponent.Topic(Constants.DEFAULT_PARSER_INVALID_TOPIC, 1));
      add(new KafkaWithZKComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
    }});
    topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());

    ConfigUploadComponent configUploadComponent = new ConfigUploadComponent()
            .withTopologyProperties(topologyProperties)
            .withGlobalConfig(globalConfig)
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
      Map<String, List<JSONObject>> outputMessages =
              runner.process(new Processor<Map<String, List<JSONObject>>>() {
                Map<String, List<JSONObject>> messages = null;

                public ReadinessState process(ComponentRunner runner) {
                  KafkaWithZKComponent kafkaWithZKComponent = runner.getComponent("kafka", KafkaWithZKComponent.class);
                  List<byte[]> outputMessages = kafkaWithZKComponent.readMessages(Constants.ENRICHMENT_TOPIC);
                  List<byte[]> invalid = kafkaWithZKComponent.readMessages(Constants.DEFAULT_PARSER_INVALID_TOPIC);
                  List<byte[]> error = kafkaWithZKComponent.readMessages(Constants.DEFAULT_PARSER_ERROR_TOPIC);
                  if(outputMessages.size() == 1 && invalid.size() == 1 && error.size() == 1) {
                    messages = new HashMap<String, List<JSONObject>>() {{
                      put(Constants.ENRICHMENT_TOPIC, loadMessages(outputMessages));
                      put(Constants.DEFAULT_PARSER_ERROR_TOPIC, loadMessages(error));
                      put(Constants.DEFAULT_PARSER_INVALID_TOPIC, loadMessages(invalid));
                    }};
                    return ReadinessState.READY;
                  }
                  return ReadinessState.NOT_READY;
                }

                public Map<String, List<JSONObject>> getResult() {
                  return messages;
                }
              });
      Assert.assertEquals(3, outputMessages.size());
      Assert.assertEquals(1, outputMessages.get(Constants.ENRICHMENT_TOPIC).size());
      Assert.assertEquals("valid", outputMessages.get(Constants.ENRICHMENT_TOPIC).get(0).get("action"));
      Assert.assertEquals(1, outputMessages.get(Constants.DEFAULT_PARSER_ERROR_TOPIC).size());
      Assert.assertEquals("error", outputMessages.get(Constants.DEFAULT_PARSER_ERROR_TOPIC).get(0).get("rawMessage"));
      Assert.assertTrue(Arrays.equals(listToBytes(outputMessages.get(Constants.DEFAULT_PARSER_ERROR_TOPIC).get(0).get("rawMessage_bytes"))
                                     , "error".getBytes()
                                     )
                      );
      Assert.assertEquals(1, outputMessages.get(Constants.DEFAULT_PARSER_INVALID_TOPIC).size());
      Assert.assertEquals("invalid", outputMessages.get(Constants.DEFAULT_PARSER_INVALID_TOPIC).get(0).get("action"));
    }
    finally {
      if(runner != null) {
        runner.stop();
      }
    }
  }
  private static byte[] listToBytes(Object o ){
    List<Byte> l = (List<Byte>)o;
    byte[] ret = new byte[l.size()];
    int i = 0;
    for(Number b : l) {
      ret[i++] = b.byteValue();
    }
    return ret;
  }
  private static List<JSONObject> loadMessages(List<byte[]> outputMessages) {
    List<JSONObject> tmp = new ArrayList<>();
    Iterables.addAll(tmp
            , Iterables.transform(outputMessages
                    , message -> {
                      try {
                        return new JSONObject(JSONUtils.INSTANCE.load(new String(message)
                                             , new TypeReference<Map<String, Object>>() {}
                                             )
                        );
                      } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                      }
                    }
            )
    );
    return tmp;
  }
}
