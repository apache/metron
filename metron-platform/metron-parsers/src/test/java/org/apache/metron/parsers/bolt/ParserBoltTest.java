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
package org.apache.metron.parsers.bolt;

import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.parsers.ParserResult;
import org.apache.metron.parsers.ParserRunner;
import org.apache.metron.storm.kafka.flux.SimpleStormKafkaBuilder.FieldsConfiguration;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.apache.metron.test.error.MetronErrorJSONMatcher;
import org.apache.storm.Config;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ParserBoltTest extends BaseBoltTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Mock
  private Tuple t1;

  @Mock
  private ParserRunner parserRunner;

  @Mock
  private WriterHandler writerHandler;

  @Mock
  private WriterHandler writerHandlerHandleAck;

  @Mock
  private MessageGetStrategy messageGetStrategy;

  private class MockParserRunner extends ParserRunner {

    private boolean isInvalid = false;
    private RawMessage rawMessage;
    private JSONObject message;

    public MockParserRunner(HashSet<String> sensorTypes) {
      super(sensorTypes);
    }

    @Override
    public void execute(String sensorType, RawMessage rawMessage, ParserConfigurations parserConfigurations) {
      this.rawMessage = rawMessage;
      if (!isInvalid) {
        onSuccess.accept(new ParserResult(sensorType, message, rawMessage.getMessage()));
      } else {
        MetronError error = new MetronError()
                .withErrorType(Constants.ErrorType.PARSER_INVALID)
                .withSensorType(Collections.singleton(sensorType))
                .addRawMessage(message);
        onError.accept(error);
      }
    }

    protected void setInvalid(boolean isInvalid) {
      this.isInvalid = isInvalid;
    }

    protected void setMessage(JSONObject message) {
      this.message = message;
    }

    protected RawMessage getRawMessage() {
      return rawMessage;
    }
  }

  @Before
  public void setup() {
    when(writerHandler.handleAck()).thenReturn(false);
    when(writerHandlerHandleAck.handleAck()).thenReturn(true);

  }

  @Test
  public void shouldThrowExceptionOnDifferentHandleAck() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("All writers must match when calling handleAck()");

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
      put("bro", writerHandlerHandleAck);
    }});
  }

  @Test
  public void withBatchTimeoutDivisorShouldSetBatchTimeoutDivisor() {
    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}).withBatchTimeoutDivisor(5);

    Assert.assertEquals(5, parserBolt.getBatchTimeoutDivisor());
  }

  @Test
  public void shouldThrowExceptionOnInvalidBatchTimeoutDivisor() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("batchTimeoutDivisor must be positive. Value provided was -1");

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}).withBatchTimeoutDivisor(-1);
  }

  @Test
  public void shouldGetComponentConfiguration() {
    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}) {

      @Override
      public ParserConfigurations getConfigurations() {
        ParserConfigurations configurations = new ParserConfigurations();
        SensorParserConfig sensorParserConfig = new SensorParserConfig();
        sensorParserConfig.setParserConfig(new HashMap<String, Object>() {{
            put(IndexingConfigurations.BATCH_SIZE_CONF, 10);
        }});
        configurations.updateSensorParserConfig("yaf", sensorParserConfig);
        return configurations;
      }
    };

    Map<String, Object> componentConfiguration = parserBolt.getComponentConfiguration();
    Assert.assertEquals(1, componentConfiguration.size());
    Assert.assertEquals( 14, componentConfiguration.get(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS));
  }

  @Test
  public void shouldPrepare() {
    Map stormConf = mock(Map.class);
    SensorParserConfig yafConfig = mock(SensorParserConfig.class);
    when(yafConfig.getSensorTopic()).thenReturn("yafTopic");
    when(yafConfig.getParserConfig()).thenReturn(new HashMap<String, Object>() {{
      put(IndexingConfigurations.BATCH_SIZE_CONF, 10);
    }});
    ParserConfigurations parserConfigurations = mock(ParserConfigurations.class);

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}) {

      @Override
      protected SensorParserConfig getSensorParserConfig(String sensorType) {
        if ("yaf".equals(sensorType)) {
          return yafConfig;
        }
        return null;
      }

      @Override
      public ParserConfigurations getConfigurations() {
        return parserConfigurations;
      }
    };

    parserBolt.setCuratorFramework(client);
    parserBolt.setZKCache(cache);

    parserBolt.prepare(stormConf, topologyContext, outputCollector);

    verify(parserRunner, times(1)).setOnError(any(Consumer.class));
    verify(parserRunner, times(1)).init(eq(client), any(Supplier.class));
    verify(yafConfig, times(1)).init();
    Map<String, String> topicToSensorMap = parserBolt.getTopicToSensorMap();
    Assert.assertEquals(1, topicToSensorMap.size());
    Assert.assertEquals("yaf", topicToSensorMap.get("yafTopic"));
    verify(writerHandler).init(stormConf, topologyContext, outputCollector, parserConfigurations);
    verify(writerHandler).setDefaultBatchTimeout(14);
  }

  @Test
  public void shouldThrowExceptionOnMissingConfig() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unable to retrieve a parser config for yaf");

    Map stormConf = mock(Map.class);

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }});

    parserBolt.setCuratorFramework(client);
    parserBolt.setZKCache(cache);

    parserBolt.prepare(stormConf, topologyContext, outputCollector);
  }


  @Test
  public void executeShouldHandleTickTuple() throws Exception {
    when(t1.getSourceComponent()).thenReturn("__system");
    when(t1.getSourceStreamId()).thenReturn("__tick");
    ParserConfigurations parserConfigurations = mock(ParserConfigurations.class);

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}) {

      @Override
      public ParserConfigurations getConfigurations() {
        return parserConfigurations;
      }
    };

    parserBolt.setMessageGetStrategy(messageGetStrategy);
    parserBolt.setOutputCollector(outputCollector);

    parserBolt.execute(t1);

    verify(writerHandler, times(1)).flush(parserConfigurations, messageGetStrategy);
    verify(outputCollector, times(1)).ack(t1);
  }

  @Test
  public void shouldExecuteOnSuccess() throws Exception {
    when(messageGetStrategy.get(t1)).thenReturn("originalMessage".getBytes(StandardCharsets.UTF_8));
    when(t1.getStringByField(FieldsConfiguration.TOPIC.getFieldName())).thenReturn("yafTopic");
    MockParserRunner mockParserRunner = new MockParserRunner(new HashSet<String>() {{ add("yaf"); }});
    ParserConfigurations parserConfigurations = new ParserConfigurations();
    parserConfigurations.updateSensorParserConfig("yaf", new SensorParserConfig());

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", mockParserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}) {

      @Override
      public ParserConfigurations getConfigurations() {
        return parserConfigurations;
      }
    };

    parserBolt.setMessageGetStrategy(messageGetStrategy);
    parserBolt.setOutputCollector(outputCollector);
    parserBolt.setTopicToSensorMap(new HashMap<String, String>() {{
      put("yafTopic", "yaf");
    }});
    JSONObject message = new JSONObject();
    message.put("field", "value");
    mockParserRunner.setMessage(message);
    RawMessage expectedRawMessage = new RawMessage("originalMessage".getBytes(StandardCharsets.UTF_8), new HashMap<>());

    {
      // Verify the correct message is written and ack is handled
      parserBolt.execute(t1);

      Assert.assertEquals(expectedRawMessage, mockParserRunner.getRawMessage());
      verify(writerHandler, times(1)).write("yaf", t1, message, parserConfigurations, messageGetStrategy);
      verify(outputCollector, times(1)).ack(t1);
    }
    {
      // Verify the tuple is not acked when the writer is set to handle ack
      reset(outputCollector);
      parserBolt.setSensorToWriterMap(new HashMap<String, WriterHandler>() {{
        put("yaf", writerHandlerHandleAck);
      }});

      parserBolt.execute(t1);

      verify(writerHandlerHandleAck, times(1)).write("yaf", t1, message, parserConfigurations, messageGetStrategy);
      verify(outputCollector, times(0)).ack(t1);
    }
  }

  @Test
  public void shouldExecuteOnError() throws Exception {
    when(messageGetStrategy.get(t1)).thenReturn("originalMessage".getBytes(StandardCharsets.UTF_8));
    when(t1.getStringByField(FieldsConfiguration.TOPIC.getFieldName())).thenReturn("yafTopic");
    MockParserRunner mockParserRunner = new MockParserRunner(new HashSet<String>() {{
      add("yaf");
    }});
    mockParserRunner.setInvalid(true);
    ParserConfigurations parserConfigurations = new ParserConfigurations();
    parserConfigurations.updateSensorParserConfig("yaf", new SensorParserConfig());

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", mockParserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}) {

      @Override
      public ParserConfigurations getConfigurations() {
        return parserConfigurations;
      }
    };

    mockParserRunner.setOnError(parserBolt::onError);
    parserBolt.setMessageGetStrategy(messageGetStrategy);
    parserBolt.setOutputCollector(outputCollector);
    parserBolt.setTopicToSensorMap(new HashMap<String, String>() {{
      put("yafTopic", "yaf");
    }});
    JSONObject message = new JSONObject();
    message.put("field", "value");
    mockParserRunner.setMessage(message);
    RawMessage expectedRawMessage = new RawMessage("originalMessage".getBytes(StandardCharsets.UTF_8), new HashMap<>());
    MetronError error = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_INVALID)
            .withSensorType(Collections.singleton("yaf"))
            .addRawMessage(message);

    parserBolt.execute(t1);

    Assert.assertEquals(expectedRawMessage, mockParserRunner.getRawMessage());
    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM),
            argThat(new MetronErrorJSONMatcher(error.getJSONObject())));
    verify(outputCollector, times(1)).ack(t1);

  }

  @Test
  public void shouldThrowExceptionOnFailedExecute() {
    when(messageGetStrategy.get(t1)).thenReturn("originalMessage".getBytes(StandardCharsets.UTF_8));
    when(t1.getStringByField(FieldsConfiguration.TOPIC.getFieldName())).thenReturn("yafTopic");

    ParserConfigurations parserConfigurations = new ParserConfigurations();
    parserConfigurations.updateSensorParserConfig("yaf", new SensorParserConfig());
    doThrow(new IllegalStateException("parserRunner.execute failed")).when(parserRunner).execute(eq("yaf"), any(), eq(parserConfigurations));

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}) {

      @Override
      public ParserConfigurations getConfigurations() {
        return parserConfigurations;
      }
    };

    parserBolt.setMessageGetStrategy(messageGetStrategy);
    parserBolt.setOutputCollector(outputCollector);
    parserBolt.setTopicToSensorMap(new HashMap<String, String>() {{
      put("yafTopic", "yaf");
    }});
    MetronError error = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_ERROR)
            .withThrowable(new IllegalStateException("parserRunner.execute failed"))
            .withSensorType(Collections.singleton("yaf"))
            .addRawMessage("originalMessage".getBytes(StandardCharsets.UTF_8));

    parserBolt.execute(t1);

    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM),
            argThat(new MetronErrorJSONMatcher(error.getJSONObject())));
    verify(outputCollector, times(1)).reportError(any(IllegalStateException.class));
    verify(outputCollector, times(1)).ack(t1);
  }

  @Test
  public void shouldThrowExceptionOnFailedWrite() throws Exception {
    when(messageGetStrategy.get(t1)).thenReturn("originalMessage".getBytes(StandardCharsets.UTF_8));
    when(t1.getStringByField(FieldsConfiguration.TOPIC.getFieldName())).thenReturn("yafTopic");
    MockParserRunner mockParserRunner = new MockParserRunner(new HashSet<String>() {{ add("yaf"); }});
    ParserConfigurations parserConfigurations = new ParserConfigurations();
    parserConfigurations.updateSensorParserConfig("yaf", new SensorParserConfig());
    doThrow(new IllegalStateException("write failed")).when(writerHandler).write(any(), any(), any(), any(), any());

    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", mockParserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}) {

      @Override
      public ParserConfigurations getConfigurations() {
        return parserConfigurations;
      }
    };

    parserBolt.setMessageGetStrategy(messageGetStrategy);
    parserBolt.setOutputCollector(outputCollector);
    parserBolt.setTopicToSensorMap(new HashMap<String, String>() {{
      put("yafTopic", "yaf");
    }});
    JSONObject message = new JSONObject();
    message.put("field", "value");
    mockParserRunner.setMessage(message);

    MetronError error = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_ERROR)
            .withThrowable(new IllegalStateException("write failed"))
            .withSensorType(Collections.singleton("yaf"))
            .addRawMessage("originalMessage".getBytes(StandardCharsets.UTF_8));

    parserBolt.execute(t1);

    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM),
            argThat(new MetronErrorJSONMatcher(error.getJSONObject())));
    verify(outputCollector, times(1)).reportError(any(IllegalStateException.class));
    verify(outputCollector, times(1)).ack(t1);
  }


//  @Test
//  public void testEmpty() throws Exception {
//    String sensorType = "yaf";
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//        sensorType,
//       new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, writerMap) {
//      @Override
//      protected ConfigurationsUpdater<ParserConfigurations> createUpdater() {
//        return ParserBoltTest.createUpdater();
//      }
//    };
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    verify(parser, times(1)).init();
//    verify(writer, times(1)).init();
//    byte[] sampleBinary = "some binary message".getBytes();
//
//    when(tuple.getBinary(0)).thenReturn(sampleBinary);
//    when(parser.parseOptional(sampleBinary)).thenReturn(null);
//    parserBolt.execute(tuple);
//    verify(parser, times(0)).validate(any());
//    verify(writer, times(0)).write(eq(sensorType), any(ParserWriterConfiguration.class), eq(tuple), any());
//    verify(outputCollector, times(1)).ack(tuple);
//
//    MetronError error = new MetronError()
//            .withErrorType(Constants.ErrorType.PARSER_ERROR)
//            .withThrowable(new NullPointerException())
//            .withSensorType(Collections.singleton(sensorType))
//            .addRawMessage(sampleBinary);
//    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM), argThat(new MetronErrorJSONMatcher(error.getJSONObject())));
//  }
//
//  @Test
//  public void testInvalid() throws Exception {
//    String sensorType = "yaf";
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//            sensorType,
//            new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, writerMap) {
//      @Override
//      protected ConfigurationsUpdater<ParserConfigurations> createUpdater() {
//        return ParserBoltTest.createUpdater();
//      }
//    };
//
//    buildGlobalConfig(parserBolt);
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    byte[] sampleBinary = "some binary message".getBytes();
//
//    when(tuple.getBinary(0)).thenReturn(sampleBinary);
//    JSONObject parsedMessage = new JSONObject();
//    parsedMessage.put("field", "invalidValue");
//    parsedMessage.put("guid", "this-is-unique-identifier-for-tuple");
//    List<JSONObject> messageList = new ArrayList<>();
//    messageList.add(parsedMessage);
//    when(parser.parseOptional(sampleBinary)).thenReturn(Optional.of(messageList));
//    when(parser.validate(parsedMessage)).thenReturn(true);
//    parserBolt.execute(tuple);
//
//    MetronError error = new MetronError()
//            .withErrorType(Constants.ErrorType.PARSER_INVALID)
//            .withSensorType(Collections.singleton(sensorType))
//            .withErrorFields(new HashSet<String>() {{ add("field"); }})
//            .addRawMessage(new JSONObject(){{
//              put("field", "invalidValue");
//              put("source.type", "yaf");
//              put("guid", "this-is-unique-identifier-for-tuple");
//            }});
//    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM), argThat(new MetronErrorJSONMatcher(error.getJSONObject())));
//  }
//
//  @Test
//  public void test() throws Exception {
//    String sensorType = "yaf";
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//            sensorType,
//            new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, writerMap) {
//      @Override
//      protected ConfigurationsUpdater<ParserConfigurations> createUpdater() {
//        return ParserBoltTest.createUpdater();
//      }
//    };
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    verify(parser, times(1)).init();
//    verify(writer, times(1)).init();
//    byte[] sampleBinary = "some binary message".getBytes();
//    JSONParser jsonParser = new JSONParser();
//    final JSONObject sampleMessage1 = (JSONObject) jsonParser.parse("{ \"field1\":\"value1\", \"guid\": \"this-is-unique-identifier-for-tuple\" }");
//    final JSONObject sampleMessage2 = (JSONObject) jsonParser.parse("{ \"field2\":\"value2\", \"guid\": \"this-is-unique-identifier-for-tuple\" }");
//    List<JSONObject> messages = new ArrayList<JSONObject>() {{
//      add(sampleMessage1);
//      add(sampleMessage2);
//    }};
//    final JSONObject finalMessage1 = (JSONObject) jsonParser.parse("{ \"field1\":\"value1\", \"source.type\":\"" + sensorType + "\", \"guid\": \"this-is-unique-identifier-for-tuple\" }");
//    final JSONObject finalMessage2 = (JSONObject) jsonParser.parse("{ \"field2\":\"value2\", \"source.type\":\"" + sensorType + "\", \"guid\": \"this-is-unique-identifier-for-tuple\" }");
//    when(tuple.getBinary(0)).thenReturn(sampleBinary);
//    when(parser.parseOptional(sampleBinary)).thenReturn(Optional.of(messages));
//    when(parser.validate(eq(messages.get(0)))).thenReturn(true);
//    when(parser.validate(eq(messages.get(1)))).thenReturn(false);
//    parserBolt.execute(tuple);
//    verify(writer, times(1)).write(eq(sensorType), any(ParserWriterConfiguration.class), eq(tuple), eq(finalMessage1));
//    verify(outputCollector, times(1)).ack(tuple);
//    when(parser.validate(eq(messages.get(0)))).thenReturn(true);
//    when(parser.validate(eq(messages.get(1)))).thenReturn(true);
//    when(filter.emit(eq(messages.get(0)), any())).thenReturn(false);
//    when(filter.emit(eq(messages.get(1)), any())).thenReturn(true);
//    parserBolt.execute(tuple);
//    verify(writer, times(1)).write(eq(sensorType), any(ParserWriterConfiguration.class), eq(tuple), eq(finalMessage2));
//    verify(outputCollector, times(2)).ack(tuple);
//    doThrow(new Exception()).when(writer).write(eq(sensorType), any(ParserWriterConfiguration.class), eq(tuple), eq(finalMessage2));
//    parserBolt.execute(tuple);
//    verify(outputCollector, times(1)).reportError(any(Throwable.class));
//  }
//
//  /**
//   {
//    "filterClassName" : "STELLAR"
//   ,"parserConfig" : {
//    "filter.query" : "exists(field1)"
//    }
//   }
//   */
//  @Multiline
//  public static String sensorParserConfig;
//
//  /**
//   * Tests to ensure that a message that is unfiltered results in one write and an ack.
//   * @throws Exception
//   */
//  @Test
//  public void testFilterSuccess() throws Exception {
//
//    String sensorType = "yaf";
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//            sensorType,
//            new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = buildParserBolt(parserRunner, writerMap, sensorParserConfig);
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    verify(parser, times(1)).init();
//    verify(batchWriter, times(1)).init(any(), any(), any());
//    BulkWriterResponse successResponse = mock(BulkWriterResponse.class);
//    when(successResponse.getSuccesses()).thenReturn(ImmutableList.of(t1));
//    when(batchWriter.write(any(), any(), any(), any())).thenReturn(successResponse);
//    when(parser.validate(any())).thenReturn(true);
//    when(parser.parseOptional(any())).thenReturn(Optional.of(ImmutableList.of(new JSONObject(new HashMap<String, Object>() {{
//      put("field1", "blah");
//    }}))));
//    parserBolt.execute(t1);
//    verify(batchWriter, times(1)).write(any(), any(), any(), any());
//    verify(outputCollector, times(1)).ack(t1);
//  }
//
//
//  /**
//   * Tests to ensure that a message filtered out results in no writes, but an ack.
//   * @throws Exception
//   */
//  @Test
//  public void testFilterFailure() throws Exception {
//    String sensorType = "yaf";
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//            sensorType,
//            new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, writerMap) {
//      @Override
//      protected SensorParserConfig getSensorParserConfig(String sensorType) {
//        try {
//          return SensorParserConfig.fromBytes(Bytes.toBytes(sensorParserConfig));
//        } catch (IOException e) {
//          throw new RuntimeException(e);
//        }
//      }
//
//      @Override
//      protected ConfigurationsUpdater<ParserConfigurations> createUpdater() {
//        return ParserBoltTest.createUpdater();
//      }
//    };
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    verify(parser, times(1)).init();
//    verify(batchWriter, times(1)).init(any(), any(), any());
//    when(parser.validate(any())).thenReturn(true);
//    when(parser.parseOptional(any())).thenReturn(Optional.of(ImmutableList.of(new JSONObject(new HashMap<String, Object>() {{
//      put("field2", "blah");
//    }}))));
//    parserBolt.execute(t1);
//    verify(batchWriter, times(0)).write(any(), any(), any(), any());
//    verify(outputCollector, times(1)).ack(t1);
//  }
//  /**
//  {
//     "sensorTopic":"dummy"
//     ,"parserConfig": {
//      "batchSize" : 1
//     }
//      ,"fieldTransformations" : [
//          {
//           "transformation" : "STELLAR"
//          ,"output" : "timestamp"
//          ,"config" : {
//            "timestamp" : "TO_EPOCH_TIMESTAMP(timestampstr, 'yyyy-MM-dd HH:mm:ss', 'UTC')"
//                      }
//          }
//                               ]
//   }
//   */
//  @Multiline
//  public static String csvWithFieldTransformations;
//
//  @Test
//  public void testFieldTransformationPriorToValidation() {
//    String sensorType = "dummy";
//    RecordingWriter recordingWriter = new RecordingWriter();
//    //create a parser which acts like a basic parser but returns no timestamp field.
//    BasicParser dummyParser = new BasicParser() {
//      @Override
//      public void init() {
//
//      }
//
//      @Override
//      public List<JSONObject> parse(byte[] rawMessage) {
//        return ImmutableList.of(new JSONObject() {{
//                put("data", "foo");
//                put("timestampstr", "2016-01-05 17:02:30");
//                put("original_string", "blah");
//              }});
//      }
//
//      @Override
//      public void configure(Map<String, Object> config) {
//
//      }
//    };
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//            sensorType,
//            new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = buildParserBolt(parserRunner, writerMap, csvWithFieldTransformations);
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    when(t1.getBinary(0)).thenReturn(new byte[] {});
//    parserBolt.execute(t1);
//    Assert.assertEquals(1, recordingWriter.getRecords().size());
//    long expected = 1452013350000L;
//    Assert.assertEquals(expected, recordingWriter.getRecords().get(0).get("timestamp"));
//  }
//
//  @Test
//  public void testDefaultBatchSize() throws Exception {
//    String sensorType = "yaf";
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//            sensorType,
//            new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, writerMap) {
//      @Override
//      protected ConfigurationsUpdater<ParserConfigurations> createUpdater() {
//        // this uses default batch size
//        return ParserBoltTest.createUpdater();
//      }
//    };
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    verify(parser, times(1)).init();
//    verify(batchWriter, times(1)).init(any(), any(), any());
//    when(parser.validate(any())).thenReturn(true);
//    when(parser.parseOptional(any())).thenReturn(Optional.of(ImmutableList.of(new JSONObject())));
//    when(filter.emit(any(), any(Context.class))).thenReturn(true);
//    BulkWriterResponse response = new BulkWriterResponse();
//    Tuple[] uniqueTuples = new Tuple[ParserConfigurations.DEFAULT_KAFKA_BATCH_SIZE];
//    for (int i=0; i < uniqueTuples.length; i++) {
//      uniqueTuples[i] = mock(Tuple.class);
//      response.addSuccess(uniqueTuples[i]);
//    }
//    when(batchWriter.write(eq(sensorType), any(WriterConfiguration.class), eq(new HashSet<>(Arrays.asList(uniqueTuples))), any())).thenReturn(response);
//    for (Tuple tuple : uniqueTuples) {
//      parserBolt.execute(tuple);
//    }
//    for (Tuple uniqueTuple : uniqueTuples) {
//      verify(outputCollector, times(1)).ack(uniqueTuple);
//    }
//  }
//
//  @Test
//  public void testLessRecordsThanDefaultBatchSize() throws Exception {
//    String sensorType = "yaf";
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//            sensorType,
//            new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, writerMap) {
//      @Override
//      protected ConfigurationsUpdater<ParserConfigurations> createUpdater() {
//        // this uses default batch size
//        return ParserBoltTest.createUpdater();
//      }
//    };
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    verify(parser, times(1)).init();
//    verify(batchWriter, times(1)).init(any(), any(), any());
//    when(parser.validate(any())).thenReturn(true);
//    when(parser.parseOptional(any())).thenReturn(Optional.of(ImmutableList.of(new JSONObject())));
//    when(filter.emit(any(), any(Context.class))).thenReturn(true);
//    int oneLessThanDefaultBatchSize = ParserConfigurations.DEFAULT_KAFKA_BATCH_SIZE - 1;
//    BulkWriterResponse response = new BulkWriterResponse();
//    Tuple[] uniqueTuples = new Tuple[oneLessThanDefaultBatchSize];
//    for (int i=0; i < uniqueTuples.length; i++) {
//      uniqueTuples[i] = mock(Tuple.class);
//      response.addSuccess(uniqueTuples[i]);
//    }
//    for (Tuple tuple : uniqueTuples) {
//      parserBolt.execute(tuple);
//    }
//    // should have no acking yet - batch size not fulfilled
//    verify(outputCollector, never()).ack(any(Tuple.class));
//    response.addSuccess(t1); // used to achieve count in final verify
//    Iterable<Tuple> tuples = new HashSet(Arrays.asList(uniqueTuples)) {{
//      add(t1);
//    }};
//    when(batchWriter.write(eq(sensorType), any(WriterConfiguration.class), eq(tuples), any())).thenReturn(response);
//    // meet batch size requirement and now it should ack
//    parserBolt.execute(t1);
//    verify(outputCollector, times(ParserConfigurations.DEFAULT_KAFKA_BATCH_SIZE)).ack(any(Tuple.class));
//  }
//
//  @Test
//  public void testBatchOfOne() throws Exception {
//    String sensorType = "yaf";
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//            sensorType,
//            new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, writerMap) {
//      @Override
//      protected ConfigurationsUpdater<ParserConfigurations> createUpdater() {
//        return ParserBoltTest.createUpdater(Optional.of(1));
//      }
//    };
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    verify(parser, times(1)).init();
//    verify(batchWriter, times(1)).init(any(), any(), any());
//    when(parser.validate(any())).thenReturn(true);
//    when(parser.parseOptional(any())).thenReturn(Optional.of(ImmutableList.of(new JSONObject())));
//    when(filter.emit(any(), any(Context.class))).thenReturn(true);
//    BulkWriterResponse response = new BulkWriterResponse();
//    response.addSuccess(t1);
//    when(batchWriter.write(eq(sensorType), any(WriterConfiguration.class), eq(Collections.singleton(t1)), any())).thenReturn(response);
//    parserBolt.execute(t1);
//    verify(outputCollector, times(1)).ack(t1);
//  }
//
//  @Test
//  public void testBatchOfFive() throws Exception {
//    String sensorType = "yaf";
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//            sensorType,
//            new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, writerMap) {
//      @Override
//      protected ConfigurationsUpdater<ParserConfigurations> createUpdater() {
//        return ParserBoltTest.createUpdater(Optional.of(5));
//      }
//    } ;
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    verify(parser, times(1)).init();
//    verify(batchWriter, times(1)).init(any(), any(), any());
//    when(parser.validate(any())).thenReturn(true);
//    when(parser.parseOptional(any())).thenReturn(Optional.of(ImmutableList.of(new JSONObject())));
//    when(filter.emit(any(), any(Context.class))).thenReturn(true);
//    Set<Tuple> tuples = Stream.of(t1, t2, t3, t4, t5).collect(Collectors.toSet());
//    BulkWriterResponse response = new BulkWriterResponse();
//    response.addAllSuccesses(tuples);
//    when(batchWriter.write(eq(sensorType), any(WriterConfiguration.class), eq(tuples), any())).thenReturn(response);
//    writeNonBatch(outputCollector, parserBolt, t1);
//    writeNonBatch(outputCollector, parserBolt, t2);
//    writeNonBatch(outputCollector, parserBolt, t3);
//    writeNonBatch(outputCollector, parserBolt, t4);
//    parserBolt.execute(t5);
//    verify(batchWriter, times(1)).write(eq(sensorType), any(WriterConfiguration.class), eq(tuples), any());
//    verify(outputCollector, times(1)).ack(t1);
//    verify(outputCollector, times(1)).ack(t2);
//    verify(outputCollector, times(1)).ack(t3);
//    verify(outputCollector, times(1)).ack(t4);
//    verify(outputCollector, times(1)).ack(t5);
//
//
//  }
//
//  @Test
//  public void testBatchOfFiveWithError() throws Exception {
//    String sensorType = "yaf";
//    ParserRunner parserRunner = new ParserRunner(Collections.singleton(sensorType));
//    Map<String, WriterHandler> writerMap = Collections.singletonMap(
//            sensorType,
//            new WriterHandler(writer)
//
//    );
//    ParserBolt parserBolt = new ParserBolt("zookeeperUrl", parserRunner, writerMap) {
//      @Override
//      protected ConfigurationsUpdater<ParserConfigurations> createUpdater() {
//        return ParserBoltTest.createUpdater(Optional.of(5));
//      }
//    };
//
//    parserBolt.setCuratorFramework(client);
//    parserBolt.setZKCache(cache);
//    parserBolt.prepare(new HashMap(), topologyContext, outputCollector);
//    verify(parser, times(1)).init();
//    verify(batchWriter, times(1)).init(any(), any(), any());
//
//    doThrow(new Exception()).when(batchWriter).write(any(), any(), any(), any());
//    when(parser.validate(any())).thenReturn(true);
//    when(parser.parseOptional(any())).thenReturn(Optional.of(ImmutableList.of(new JSONObject())));
//    when(filter.emit(any(), any(Context.class))).thenReturn(true);
//    parserBolt.execute(t1);
//    parserBolt.execute(t2);
//    parserBolt.execute(t3);
//    parserBolt.execute(t4);
//    parserBolt.execute(t5);
//    verify(batchWriter, times(1)).write(any(), any(), any(), any());
//    verify(outputCollector, times(1)).ack(t1);
//    verify(outputCollector, times(1)).ack(t2);
//    verify(outputCollector, times(1)).ack(t3);
//    verify(outputCollector, times(1)).ack(t4);
//    verify(outputCollector, times(1)).ack(t5);
//
//  }
//
//  protected void buildGlobalConfig(ParserBolt parserBolt) {
//    HashMap<String, Object> globalConfig = new HashMap<>();
//    Map<String, Object> fieldValidation = new HashMap<>();
//    fieldValidation.put("input", Arrays.asList("field"));
//    fieldValidation.put("validation", "STELLAR");
//    fieldValidation.put("config", new HashMap<String, String>(){{ put("condition", "field != 'invalidValue'"); }});
//    globalConfig.put("fieldValidations", Arrays.asList(fieldValidation));
//    parserBolt.getConfigurations().updateGlobalConfig(globalConfig);
//  }
//
//  private ParserBolt buildParserBolt(ParserRunner parserRunner, Map<String, WriterHandler> writerMap,
//      String csvWithFieldTransformations) {
//    return new ParserBolt("zookeeperUrl", parserRunner, writerMap) {
//      @Override
//      protected SensorParserConfig getSensorParserConfig(String sensorType) {
//        try {
//          return SensorParserConfig.fromBytes(Bytes.toBytes(csvWithFieldTransformations));
//        } catch (IOException e) {
//          throw new RuntimeException(e);
//        }
//      }
//
//      @Override
//      protected ConfigurationsUpdater<ParserConfigurations> createUpdater() {
//        return ParserBoltTest.createUpdater(Optional.of(1));
//      }
//    };
//  }
//
//  private static void writeNonBatch(OutputCollector collector, ParserBolt bolt, Tuple t) {
//    bolt.execute(t);
//  }

}
