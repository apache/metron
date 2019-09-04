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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.field.validation.FieldValidation;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.ConfigUploadComponent;
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
      if (input.get("action") != null && input.get("action").equals("invalid")) {
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
   *    "readMetadata": true,
   *    "parserConfig": {
   *        "batchSize" : 1,
   *        "columns" : {
   *            "action" : 0,
   *            "dummy" : 1
   *        }
   *    }
   * }
   */
  @Multiline
  public static String parserConfigJSON;

  /**
   * {
   *    "parserClassName" : "org.apache.metron.parsers.csv.CSVParser",
   *    "sensorTopic": "dummy",
   *    "outputTopic": "output",
   *    "errorTopic": "parser_error",
   *    "parserConfig": {
   *        "batchSize" : 1,
   *        "columns" : {
   *            "name" : 0,
   *            "dummy" : 1
   *        },
   *      "kafka.topicField" : "route_field"
   *    }
   *    ,"fieldTransformations" : [
   *    {
   *      "transformation" : "STELLAR"
   *     ,"input" :  ["name"]
   *     ,"output" :  ["route_field"]
   *     ,"config" : {
   *        "route_field" : "match{ name == 'metron' => 'output', default => NULL}"
   *      }
   *    }
   *    ]
   * }
   */
  @Multiline
  public static String parserConfigJSONKafkaRedirection;

  @Test
  public void test_topic_redirection() throws Exception {
    final String sensorType = "dummy";
    SensorParserConfig parserConfig = JSONUtils.INSTANCE.load(parserConfigJSONKafkaRedirection, SensorParserConfig.class);
    final List<byte[]> inputMessages = new ArrayList<byte[]>() {{
      add(Bytes.toBytes("metron,foo"));
      add(Bytes.toBytes("notmetron,foo"));
      add(Bytes.toBytes("metron,bar"));
      add(Bytes.toBytes("metron,baz"));
    }};

    final Properties topologyProperties = new Properties();
    ComponentRunner runner = setupTopologyComponents(
        topologyProperties,
        Collections.singletonList(sensorType),
        Collections.singletonList(parserConfig),
        globalConfigWithValidation
    );
    try {
      runner.start();
      kafkaComponent.writeMessages(sensorType, inputMessages);
      KafkaProcessor<Map<String, List<JSONObject>>> kafkaProcessor = getKafkaProcessor(
          parserConfig.getOutputTopic(), parserConfig.getErrorTopic(), kafkaMessageSet -> kafkaMessageSet.getMessages().size() == 3 && kafkaMessageSet.getErrors().isEmpty());
      ProcessorResult<Map<String, List<JSONObject>>> result = runner.process(kafkaProcessor);

      // validate the output messages
      Map<String,List<JSONObject>> outputMessages = result.getResult();
      for(JSONObject j : outputMessages.get(Constants.ENRICHMENT_TOPIC)) {
        Assert.assertEquals("metron", j.get("name"));
        Assert.assertEquals("output", j.get("route_field"));
        Assert.assertTrue(ImmutableSet.of("foo", "bar", "baz").contains(j.get("dummy")));
      }
    } finally {
      if(runner != null) {
        runner.stop();
      }
    }
  }

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
    ComponentRunner runner = setupTopologyComponents(topologyProperties, Collections.singletonList(sensorType),
        Collections.singletonList(parserConfig), globalConfigWithValidation);
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
  public ComponentRunner setupTopologyComponents(Properties topologyProperties, List<String> sensorTypes,
      List<SensorParserConfig> parserConfigs, String globalConfig) {
    zkServerComponent = getZKServerComponent(topologyProperties);
    List<KafkaComponent.Topic> topics = new ArrayList<>();
    for(String sensorType : sensorTypes) {
      topics.add(new KafkaComponent.Topic(sensorType, 1));
    }
    topics.add(new KafkaComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
    kafkaComponent = getKafkaComponent(topologyProperties, topics);
    topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());

    configUploadComponent = new ConfigUploadComponent()
        .withTopologyProperties(topologyProperties)
        .withGlobalConfig(globalConfig);

    for (int i = 0; i < sensorTypes.size(); ++i) {
      configUploadComponent.withParserSensorConfig(sensorTypes.get(i), parserConfigs.get(i));
    }

    parserTopologyComponent = new ParserTopologyComponent.Builder()
        .withSensorTypes(sensorTypes)
        .withTopologyProperties(topologyProperties)
        .withBrokerUrl(kafkaComponent.getBrokerList())
        .withErrorTopic(parserConfigs.get(0).getErrorTopic())
        .withOutputTopic(parserConfigs.get(0).getOutputTopic())
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

  private KafkaProcessor<Map<String, List<JSONObject>>> getKafkaProcessor(String outputTopic,
      String errorTopic) {
    return getKafkaProcessor(outputTopic, errorTopic, messageSet -> (messageSet.getMessages().size() == 1) && (messageSet.getErrors().size() == 2));
  }
  @SuppressWarnings("unchecked")
  private KafkaProcessor<Map<String, List<JSONObject>>> getKafkaProcessor(String outputTopic,
      String errorTopic, Predicate<KafkaMessageSet> predicate) {

    return new KafkaProcessor<>()
        .withKafkaComponentName("kafka")
        .withReadTopic(outputTopic)
        .withErrorTopic(errorTopic)
        .withValidateReadMessages(new Function<KafkaMessageSet, Boolean>() {
          @Nullable
          @Override
          public Boolean apply(@Nullable KafkaMessageSet messageSet) {
            return predicate.test(messageSet);
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
                    JSONUtils.INSTANCE.load(new String(message, StandardCharsets.UTF_8), JSONUtils.MAP_SUPPLIER));
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
   *    "errorTopic": "parser_error",
   *    "parserConfig": {
   *        "batchSize" : 1
   *    }
   * }
   */
  @Multiline
  public static String offsetParserConfigJSON;

  /**
   * {
   *    "parserClassName":"org.apache.metron.writers.integration.WriterBoltIntegrationTest$DummyObjectParser",
   *    "sensorTopic":"dummyobjectparser",
   *    "outputTopic": "enrichments",
   *    "errorTopic": "parser_error",
   *    "parserConfig": {
   *        "batchSize" : 1
   *    }
   * }
   */
  @Multiline
  public static String dummyParserConfigJSON;

  @Test
  public void commits_kafka_offsets_for_empty_objects() throws Exception {
    final String sensorType = "emptyobjectparser";
    SensorParserConfig parserConfig = JSONUtils.INSTANCE.load(offsetParserConfigJSON, SensorParserConfig.class);
    final List<byte[]> inputMessages = new ArrayList<byte[]>() {{
      add(Bytes.toBytes("foo"));
      add(Bytes.toBytes("bar"));
      add(Bytes.toBytes("baz"));
    }};
    final Properties topologyProperties = new Properties();
    ComponentRunner runner = setupTopologyComponents(
        topologyProperties,
        Collections.singletonList(sensorType),
        Collections.singletonList(parserConfig),
        globalConfigEmpty);
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

  @Test
  public void test_multiple_sensors() throws Exception {
    // Setup first sensor
    final String emptyObjectSensorType = "emptyobjectparser";
    SensorParserConfig emptyObjectParserConfig = JSONUtils.INSTANCE.load(offsetParserConfigJSON, SensorParserConfig.class);
    final List<byte[]> emptyObjectInputMessages = new ArrayList<byte[]>() {{
      add(Bytes.toBytes("foo"));
      add(Bytes.toBytes("bar"));
      add(Bytes.toBytes("baz"));
    }};

    // Setup second sensor
    final String dummySensorType = "dummyobjectparser";
    SensorParserConfig dummyParserConfig = JSONUtils.INSTANCE.load(dummyParserConfigJSON, SensorParserConfig.class);
    final List<byte[]> dummyInputMessages = new ArrayList<byte[]>() {{
      add(Bytes.toBytes("dummy_foo"));
      add(Bytes.toBytes("dummy_bar"));
      add(Bytes.toBytes("dummy_baz"));
    }};

    final Properties topologyProperties = new Properties();

    List<String> sensorTypes = new ArrayList<>();
    sensorTypes.add(emptyObjectSensorType);
    sensorTypes.add(dummySensorType);

    List<SensorParserConfig> parserConfigs = new ArrayList<>();
    parserConfigs.add(emptyObjectParserConfig);
    parserConfigs.add(dummyParserConfig);

    ComponentRunner runner = setupTopologyComponents(topologyProperties, sensorTypes, parserConfigs, globalConfigEmpty);
    try {
      runner.start();
      kafkaComponent.writeMessages(emptyObjectSensorType, emptyObjectInputMessages);
      kafkaComponent.writeMessages(dummySensorType, dummyInputMessages);

      final List<byte[]> allInputMessages = new ArrayList<>();
      allInputMessages.addAll(emptyObjectInputMessages);
      allInputMessages.addAll(dummyInputMessages);
      Processor allResultsProcessor = new AllResultsProcessor(allInputMessages, Constants.ENRICHMENT_TOPIC);
      @SuppressWarnings("unchecked")
      ProcessorResult<Set<JSONObject>> result = runner.process(allResultsProcessor);

      // validate the output messages
      assertThat(
          "size should match",
          result.getResult().size(),
          equalTo(allInputMessages.size()));
      for (JSONObject record : result.getResult()) {
        assertThat("record should have a guid", record.containsKey("guid"), equalTo(true));
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
   * Goal is to check returning an empty JSONObject in our List returned by parse.
   */
  public static class DummyObjectParser implements MessageParser<JSONObject>, Serializable {

    @Override
    public void init() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<JSONObject> parse(byte[] bytes) {
      JSONObject dummy = new JSONObject();
      dummy.put("dummy_key", "dummy_value");
      return ImmutableList.of(dummy);
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
              JSONUtils.INSTANCE.load(new String(b, StandardCharsets.UTF_8), JSONUtils.MAP_SUPPLIER));
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
