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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.common.field.validation.FieldValidation;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.integration.components.ConfigUploadComponent;
import org.apache.metron.integration.*;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.processors.KafkaMessageSet;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.integration.processors.KafkaProcessor;
import org.apache.metron.parsers.csv.CSVParser;
import org.apache.metron.parsers.integration.components.ParserTopologyComponent;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class WriterBoltIntegrationTest extends BaseIntegrationTest {

  public static class MockValidator implements FieldValidation{

    @Override
    public boolean isValid(Map<String, Object> input, Map<String, Object> validationConfig, Map<String, Object> globalConfig, Context context) {
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
   * {
   *   "fieldValidations" : [
   *      { "validation" : "org.apache.metron.writers.integration.WriterBoltIntegrationTest$MockValidator" }
   *   ]
   * }
   */
  @Multiline
  public static String globalConfig;

  /**
   * {
   *    "parserClassName" : "org.apache.metron.parsers.csv.CSVParser",
   *    "sensorTopic": "dummy",
   *    "outputTopic": "output",
   *    "errorTopic": "parser_error",
   *    "parserConfig": {
   *        "columns" : {
   *            "action" : 0,
   *            "dummy" : 1
   *        }
   *    }
   * }
   */
  @Multiline
  public static String parserConfigJSON;

  @Test
  public void test() throws UnableToStartException, IOException, ParseException {

    UnitTestHelper.setLog4jLevel(CSVParser.class, org.apache.log4j.Level.FATAL);
    final String sensorType = "dummy";

    SensorParserConfig parserConfig = JSONUtils.INSTANCE.load(parserConfigJSON, SensorParserConfig.class);

    // the input messages to parser
    final List<byte[]> inputMessages = new ArrayList<byte[]>() {{
      add(Bytes.toBytes("valid,foo"));
      add(Bytes.toBytes("invalid,foo"));
      add(Bytes.toBytes("error"));
    }};

    // setup external components; zookeeper, kafka
    final Properties topologyProperties = new Properties();
    final ZKServerComponent zkServerComponent = getZKServerComponent(topologyProperties);
    final KafkaComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaComponent.Topic>() {{
      add(new KafkaComponent.Topic(sensorType, 1));
      add(new KafkaComponent.Topic(parserConfig.getErrorTopic(), 1));
      add(new KafkaComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
    }});
    topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());

    ConfigUploadComponent configUploadComponent = new ConfigUploadComponent()
            .withTopologyProperties(topologyProperties)
            .withGlobalConfig(globalConfig)
            .withParserSensorConfig(sensorType, parserConfig);

    ParserTopologyComponent parserTopologyComponent = new ParserTopologyComponent.Builder()
            .withSensorType(sensorType)
            .withTopologyProperties(topologyProperties)
            .withBrokerUrl(kafkaComponent.getBrokerList())
            .withErrorTopic(parserConfig.getErrorTopic())
            .withOutputTopic(parserConfig.getOutputTopic())
            .build();

    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("zk", zkServerComponent)
            .withComponent("kafka", kafkaComponent)
            .withComponent("config", configUploadComponent)
            .withComponent("org/apache/storm", parserTopologyComponent)
            .withMillisecondsBetweenAttempts(5000)
            .withNumRetries(10)
            .withCustomShutdownOrder(new String[]{"org/apache/storm","config","kafka","zk"})
            .build();
    try {
      runner.start();
      kafkaComponent.writeMessages(sensorType, inputMessages);
      ProcessorResult<Map<String, List<JSONObject>>> result = runner.process(
              getProcessor(parserConfig.getOutputTopic(), parserConfig.getErrorTopic()));

      // validate the output messages
      Map<String,List<JSONObject>> outputMessages = result.getResult();
      Assert.assertEquals(2, outputMessages.size());
      Assert.assertEquals(1, outputMessages.get(Constants.ENRICHMENT_TOPIC).size());
      Assert.assertEquals("valid", outputMessages.get(Constants.ENRICHMENT_TOPIC).get(0).get("action"));
      Assert.assertEquals(2, outputMessages.get(parserConfig.getErrorTopic()).size());

      // validate an error message
      JSONObject invalidMessage = outputMessages.get(parserConfig.getErrorTopic()).get(0);
      Assert.assertEquals(Constants.ErrorType.PARSER_INVALID.getType(), invalidMessage.get(Constants.ErrorFields.ERROR_TYPE.getName()));
      JSONObject rawMessage = JSONUtils.INSTANCE.load((String) invalidMessage.get(Constants.ErrorFields.RAW_MESSAGE.getName()), JSONObject.class);
      Assert.assertEquals("foo", rawMessage.get("dummy"));
      Assert.assertEquals("invalid", rawMessage.get("action"));

      // validate the next error message
      JSONObject errorMessage = outputMessages.get(parserConfig.getErrorTopic()).get(1);
      Assert.assertEquals(Constants.ErrorType.PARSER_ERROR.getType(), errorMessage.get(Constants.ErrorFields.ERROR_TYPE.getName()));
      Assert.assertEquals("error", errorMessage.get(Constants.ErrorFields.RAW_MESSAGE.getName()));

    } finally {
      if(runner != null) {
        runner.stop();
      }
    }
  }

  private static List<JSONObject> loadMessages(List<byte[]> outputMessages) {
    List<JSONObject> tmp = new ArrayList<>();
    Iterables.addAll(tmp,
            Iterables.transform(outputMessages,
                    message -> {
                      try {
                        return new JSONObject(JSONUtils.INSTANCE.load(new String(message), JSONUtils.MAP_SUPPLIER));
                      } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                      }
                    }
            )
    );
    return tmp;
  }

  @SuppressWarnings("unchecked")
  private KafkaProcessor<Map<String,List<JSONObject>>> getProcessor(String outputTopic, String errorTopic){

    return new KafkaProcessor<>()
            .withKafkaComponentName("kafka")
            .withReadTopic(outputTopic)
            .withErrorTopic(errorTopic)
            .withValidateReadMessages(new Function<KafkaMessageSet, Boolean>() {
              @Nullable
              @Override
              public Boolean apply(@Nullable KafkaMessageSet messageSet) {
                return (messageSet.getMessages().size() == 1) && (messageSet.getErrors().size() == 2);
              }
            })
            .withProvideResult(new Function<KafkaMessageSet,Map<String,List<JSONObject>>>(){
              @Nullable
              @Override
              public Map<String,List<JSONObject>> apply(@Nullable KafkaMessageSet messageSet) {
                return new HashMap<String, List<JSONObject>>() {{
                  put(Constants.ENRICHMENT_TOPIC, loadMessages(messageSet.getMessages()));
                  put(errorTopic, loadMessages(messageSet.getErrors()));
                }};
              }
            });
    }
}
