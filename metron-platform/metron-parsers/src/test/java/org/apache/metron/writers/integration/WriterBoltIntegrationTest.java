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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nullable;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.field.validation.FieldValidation;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.integration.components.ConfigUploadComponent;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.integration.processors.KafkaMessageSet;
import org.apache.metron.integration.processors.KafkaProcessor;
import org.apache.metron.parsers.integration.components.ParserTopologyComponent;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class WriterBoltIntegrationTest extends BaseIntegrationTest {
  private ZKServerComponent zkServerComponent;
  private KafkaComponent kafkaComponent;
  private ConfigUploadComponent configUploadComponent;
  private ParserTopologyComponent parserTopologyComponent;

  public static class MockValidator implements FieldValidation {

    @Override
    public boolean isValid(Map<String, Object> input, Map<String, Object> validationConfig, Map<String, Object> globalConfig, Context context) {
      if (input.get("action").equals("invalid")) {
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
  public static String globalConfigWithValidation;

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
  public void parser_with_global_validations_writes_bad_records_to_error_topic() throws Exception {
    final String sensorType = "dummy";
    SensorParserConfig parserConfig = JSONUtils.INSTANCE.load(parserConfigJSON, SensorParserConfig.class);
    final List<byte[]> inputMessages = new ArrayList<byte[]>() {{
      add(Bytes.toBytes("valid,foo"));
      add(Bytes.toBytes("invalid,foo"));
      add(Bytes.toBytes("error"));
    }};

    final Properties topologyProperties = new Properties();
    ComponentRunner runner = setupTopologyComponents(topologyProperties, sensorType, parserConfig, globalConfigWithValidation);
    try {
      runner.start();
      kafkaComponent.writeMessages(sensorType, inputMessages);
      KafkaProcessor<Map<String, List<JSONObject>>> kafkaProcessor = getKafkaProcessor(
          parserConfig.getOutputTopic(), parserConfig.getErrorTopic());
      ProcessorResult<Map<String, List<JSONObject>>> result = runner.process(kafkaProcessor);

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

  /**
   * Setup external components (as side effects of invoking this method):
   * zookeeper, kafka, config upload, parser topology, main runner.
   *
   * Modifies topology properties with relevant component properties, e.g. kafka.broker.
   *
   * @return runner
   */
  public ComponentRunner setupTopologyComponents(Properties topologyProperties, String sensorType,
      SensorParserConfig parserConfig, String globalConfig) {
    zkServerComponent = getZKServerComponent(topologyProperties);
    kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaComponent.Topic>() {{
      add(new KafkaComponent.Topic(sensorType, 1));
      add(new KafkaComponent.Topic(parserConfig.getErrorTopic(), 1));
      add(new KafkaComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
    }});
    topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());

    configUploadComponent = new ConfigUploadComponent()
        .withTopologyProperties(topologyProperties)
        .withGlobalConfig(globalConfig)
        .withParserSensorConfig(sensorType, parserConfig);

    parserTopologyComponent = new ParserTopologyComponent.Builder()
        .withSensorType(sensorType)
        .withTopologyProperties(topologyProperties)
        .withBrokerUrl(kafkaComponent.getBrokerList())
        .withErrorTopic(parserConfig.getErrorTopic())
        .withOutputTopic(parserConfig.getOutputTopic())
        .build();

    return new ComponentRunner.Builder()
        .withComponent("zk", zkServerComponent)
        .withComponent("kafka", kafkaComponent)
        .withComponent("config", configUploadComponent)
        .withComponent("org/apache/storm", parserTopologyComponent)
        .withMillisecondsBetweenAttempts(5000)
        .withNumRetries(10)
        .withCustomShutdownOrder(new String[]{"org/apache/storm","config","kafka","zk"})
        .build();
  }

  @SuppressWarnings("unchecked")
  private KafkaProcessor<Map<String, List<JSONObject>>> getKafkaProcessor(String outputTopic,
      String errorTopic) {

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
        .withProvideResult(new Function<KafkaMessageSet, Map<String, List<JSONObject>>>() {
          @Nullable
          @Override
          public Map<String, List<JSONObject>> apply(@Nullable KafkaMessageSet messageSet) {
            return new HashMap<String, List<JSONObject>>() {{
              put(Constants.ENRICHMENT_TOPIC, loadMessages(messageSet.getMessages()));
              put(errorTopic, loadMessages(messageSet.getErrors()));
            }};
          }
        });
  }

  private static List<JSONObject> loadMessages(List<byte[]> outputMessages) {
    List<JSONObject> tmp = new ArrayList<>();
    Iterables.addAll(tmp,
        Iterables.transform(outputMessages,
            message -> {
              try {
                return new JSONObject(
                    JSONUtils.INSTANCE.load(new String(message), JSONUtils.MAP_SUPPLIER));
              } catch (Exception ex) {
                throw new IllegalStateException(ex);
              }
            }
        )
    );
    return tmp;
  }

  /**
   * { }
   */
  @Multiline
  public static String globalConfigEmpty;

  /**
   * {
   *    "parserClassName":"org.apache.metron.writers.integration.WriterBoltIntegrationTest$EmptyObjectParser",
   *    "sensorTopic":"emptyobjectparser",
   *    "outputTopic": "enrichments",
   *    "errorTopic": "parser_error"
   * }
   */
  @Multiline
  public static String offsetParserConfigJSON;

  @Test
  public void commits_kafka_offsets_for_emtpy_objects() throws Exception {
    final String sensorType = "emptyobjectparser";
    SensorParserConfig parserConfig = JSONUtils.INSTANCE.load(offsetParserConfigJSON, SensorParserConfig.class);
    final List<byte[]> inputMessages = new ArrayList<byte[]>() {{
      add(Bytes.toBytes("foo"));
      add(Bytes.toBytes("bar"));
      add(Bytes.toBytes("baz"));
    }};
    final Properties topologyProperties = new Properties();
    ComponentRunner runner = setupTopologyComponents(topologyProperties, sensorType, parserConfig, globalConfigEmpty);
    try {
      runner.start();
      kafkaComponent.writeMessages(sensorType, inputMessages);
      Processor allResultsProcessor = new AllResultsProcessor(inputMessages, Constants.ENRICHMENT_TOPIC);
      ProcessorResult<Set<JSONObject>> result = runner.process(allResultsProcessor);

      // validate the output messages
      assertThat("size should match", result.getResult().size(), equalTo(inputMessages.size()));
      for (JSONObject record : result.getResult()) {
        assertThat("record should have a guid", record.containsKey("guid"), equalTo(true));
        assertThat("record should have correct source.type", record.get("source.type"),
            equalTo(sensorType));
      }
    } finally {
      if (runner != null) {
        runner.stop();
      }
    }
  }

  /**
   * Goal is to check returning an empty JSONObject in our List returned by parse.
   */
  public static class EmptyObjectParser implements MessageParser<JSONObject>, Serializable {

    @Override
    public void init() {
    }

    @Override
    public List<JSONObject> parse(byte[] bytes) {
      return ImmutableList.of(new JSONObject());
    }

    @Override
    public boolean validate(JSONObject message) {
      return true;
    }

    @Override
    public void configure(Map<String, Object> map) {
    }
  }

  /**
   * Verifies all messages in the provided List of input messages appears in the specified
   * Kafka output topic
   */
  private class AllResultsProcessor implements  Processor<Set<JSONObject>> {

    private final List<byte[]> inputMessages;
    private String outputKafkaTopic;
    // used for calculating readiness and returning result set
    private final Set<JSONObject> outputMessages = new HashSet<>();

    public AllResultsProcessor(List<byte[]> inputMessages, String outputKafkaTopic) {
      this.inputMessages = inputMessages;
      this.outputKafkaTopic = outputKafkaTopic;
    }

    @Override
    public ReadinessState process(ComponentRunner runner) {
      KafkaComponent kc = runner.getComponent("kafka", KafkaComponent.class);
      outputMessages.addAll(readMessagesFromKafka(kc, outputKafkaTopic));
      return calcReadiness(inputMessages.size(), outputMessages.size());
    }

    private Set<JSONObject> readMessagesFromKafka(KafkaComponent kc, String topic) {
      Set<JSONObject> out = new HashSet<>();
      for (byte[] b : kc.readMessages(topic)) {
        try {
          JSONObject m = new JSONObject(
              JSONUtils.INSTANCE.load(new String(b), JSONUtils.MAP_SUPPLIER));
          out.add(m);
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
      return out;
    }

    private ReadinessState calcReadiness(int in, int out) {
      return in == out ? ReadinessState.READY : ReadinessState.NOT_READY;
    }

    @Override
    public ProcessorResult<Set<JSONObject>> getResult() {
      return new ProcessorResult<>(outputMessages, null);
    }
  }

}
