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
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.writer.AckTuplesPolicy;
import org.apache.metron.parsers.DefaultParserRunnerResults;
import org.apache.metron.parsers.ParserRunnerImpl;
import org.apache.metron.parsers.ParserRunnerResults;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.storm.kafka.flux.SimpleStormKafkaBuilder.FieldsConfiguration;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.apache.metron.test.error.MetronErrorJSONMatcher;
import org.apache.storm.Config;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ParserBoltTest extends BaseBoltTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Mock
  private Tuple t1;

  @Mock
  private ParserRunnerImpl parserRunner;

  @Mock
  private WriterHandler writerHandler;

  @Mock
  private MessageGetStrategy messageGetStrategy;

  @Mock
  private Context stellarContext;

  @Mock
  private AckTuplesPolicy bulkWriterResponseHandler;

  private class MockParserRunner extends ParserRunnerImpl {

    private boolean isInvalid = false;
    private RawMessage rawMessage;
    private List<JSONObject> messages;

    public MockParserRunner(HashSet<String> sensorTypes) {
      super(sensorTypes);
    }

    @Override
    public ParserRunnerResults<JSONObject> execute(String sensorType, RawMessage rawMessage, ParserConfigurations parserConfigurations) {
      DefaultParserRunnerResults parserRunnerResults = new DefaultParserRunnerResults();
      this.rawMessage = rawMessage;
      for(JSONObject message: messages) {
        if (!isInvalid) {
          parserRunnerResults.addMessage(message);
        } else {
          MetronError error = new MetronError()
                  .withErrorType(Constants.ErrorType.PARSER_INVALID)
                  .withSensorType(Collections.singleton(sensorType))
                  .addRawMessage(message);
          parserRunnerResults.addError(error);
        }
      }
      return parserRunnerResults;
    }

    protected void setInvalid(boolean isInvalid) {
      this.isInvalid = isInvalid;
    }

    protected void setMessages(List<JSONObject> messages) {
      this.messages = messages;
    }

    protected RawMessage getRawMessage() {
      return rawMessage;
    }
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

    ParserBolt parserBolt = spy(new ParserBolt("zookeeperUrl", parserRunner, new HashMap<String, WriterHandler>() {{
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
    });
    doReturn(stellarContext).when(parserBolt).initializeStellar();

    parserBolt.setCuratorFramework(client);
    parserBolt.setZKCache(cache);

    parserBolt.prepare(stormConf, topologyContext, outputCollector);

    verify(parserRunner, times(1)).init(any(Supplier.class), eq(stellarContext));
    verify(yafConfig, times(1)).init();
    Map<String, String> topicToSensorMap = parserBolt.getTopicToSensorMap();
    Assert.assertEquals(1, topicToSensorMap.size());
    Assert.assertEquals("yaf", topicToSensorMap.get("yafTopic"));
    verify(writerHandler).init(eq(stormConf), eq(topologyContext), eq(outputCollector), eq(parserConfigurations), any(AckTuplesPolicy.class), eq(14));
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

    ParserBolt parserBolt = spy(new ParserBolt("zookeeperUrl", mockParserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}) {

      @Override
      public ParserConfigurations getConfigurations() {
        return parserConfigurations;
      }
    });

    parserBolt.setMessageGetStrategy(messageGetStrategy);
    parserBolt.setOutputCollector(outputCollector);
    parserBolt.setTopicToSensorMap(new HashMap<String, String>() {{
      put("yafTopic", "yaf");
    }});
    parserBolt.setAckTuplesPolicy(bulkWriterResponseHandler);

    JSONObject message = new JSONObject();
    message.put(Constants.GUID, "messageId");
    message.put("field", "value");
    mockParserRunner.setMessages(Collections.singletonList(message));
    RawMessage expectedRawMessage = new RawMessage("originalMessage".getBytes(StandardCharsets.UTF_8), new HashMap<>());

    {
      parserBolt.execute(t1);

      Assert.assertEquals(expectedRawMessage, mockParserRunner.getRawMessage());
      verify(bulkWriterResponseHandler).addTupleMessageIds(t1, Collections.singletonList("messageId"));
      verify(writerHandler, times(1)).write("yaf", new BulkMessage<>("messageId", message), parserConfigurations);
    }
  }

  @Test
  public void shouldExecuteOnSuccessWithMultipleMessages() throws Exception {
    when(messageGetStrategy.get(t1)).thenReturn("originalMessage".getBytes(StandardCharsets.UTF_8));
    when(t1.getStringByField(FieldsConfiguration.TOPIC.getFieldName())).thenReturn("yafTopic");
    MockParserRunner mockParserRunner = new MockParserRunner(new HashSet<String>() {{ add("yaf"); }});
    ParserConfigurations parserConfigurations = new ParserConfigurations();
    parserConfigurations.updateSensorParserConfig("yaf", new SensorParserConfig());

    ParserBolt parserBolt = spy(new ParserBolt("zookeeperUrl", mockParserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}) {

      @Override
      public ParserConfigurations getConfigurations() {
        return parserConfigurations;
      }
    });

    parserBolt.setMessageGetStrategy(messageGetStrategy);
    parserBolt.setOutputCollector(outputCollector);
    parserBolt.setTopicToSensorMap(new HashMap<String, String>() {{
      put("yafTopic", "yaf");
    }});
    parserBolt.setAckTuplesPolicy(bulkWriterResponseHandler);

    List<BulkMessage<JSONObject>> messages = new ArrayList<>();
    for(int i = 0; i < 5; i++) {
      String messageId = String.format("messageId%s", i + 1);
      JSONObject message = new JSONObject();
      message.put(Constants.GUID, messageId);
      message.put("field", String.format("value%s", i + 1));
      messages.add(new BulkMessage<>(messageId, message));
    }

    mockParserRunner.setMessages(messages.stream().map(BulkMessage::getMessage).collect(Collectors.toList()));
    RawMessage expectedRawMessage = new RawMessage("originalMessage".getBytes(StandardCharsets.UTF_8), new HashMap<>());

    {
      // Verify the correct message is written and ack is handled
      parserBolt.execute(t1);

      Assert.assertEquals(expectedRawMessage, mockParserRunner.getRawMessage());

      InOrder inOrder = inOrder(bulkWriterResponseHandler, writerHandler);

      inOrder.verify(bulkWriterResponseHandler).addTupleMessageIds(t1, Arrays.asList("messageId1", "messageId2", "messageId3", "messageId4", "messageId5"));
      inOrder.verify(writerHandler, times(1)).write("yaf", messages.get(0), parserConfigurations);
      inOrder.verify(writerHandler, times(1)).write("yaf", messages.get(1), parserConfigurations);
      inOrder.verify(writerHandler, times(1)).write("yaf", messages.get(2), parserConfigurations);
      inOrder.verify(writerHandler, times(1)).write("yaf", messages.get(3), parserConfigurations);
      inOrder.verify(writerHandler, times(1)).write("yaf", messages.get(4), parserConfigurations);
    }
    verifyNoMoreInteractions(writerHandler, bulkWriterResponseHandler, outputCollector);
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

    parserBolt.setMessageGetStrategy(messageGetStrategy);
    parserBolt.setOutputCollector(outputCollector);
    parserBolt.setTopicToSensorMap(new HashMap<String, String>() {{
      put("yafTopic", "yaf");
    }});
    JSONObject message = new JSONObject();
    message.put("field", "value");
    mockParserRunner.setMessages(Collections.singletonList(message));
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
    doThrow(new IllegalStateException("write failed")).when(writerHandler).write(any(), any(), any());

    ParserBolt parserBolt = spy(new ParserBolt("zookeeperUrl", mockParserRunner, new HashMap<String, WriterHandler>() {{
      put("yaf", writerHandler);
    }}) {

      @Override
      public ParserConfigurations getConfigurations() {
        return parserConfigurations;
      }
    });

    parserBolt.setMessageGetStrategy(messageGetStrategy);
    parserBolt.setOutputCollector(outputCollector);
    parserBolt.setTopicToSensorMap(new HashMap<String, String>() {{
      put("yafTopic", "yaf");
    }});
    parserBolt.setAckTuplesPolicy(bulkWriterResponseHandler);
    JSONObject message = new JSONObject();
    message.put(Constants.GUID, "messageId");
    message.put("field", "value");
    mockParserRunner.setMessages(Collections.singletonList(message));

    MetronError error = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_ERROR)
            .withThrowable(new IllegalStateException("write failed"))
            .withSensorType(Collections.singleton("yaf"))
            .addRawMessage("originalMessage".getBytes(StandardCharsets.UTF_8));

    parserBolt.execute(t1);

    verify(bulkWriterResponseHandler, times(1)).addTupleMessageIds(t1, Collections.singletonList("messageId"));
    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM), argThat(new MetronErrorJSONMatcher(error.getJSONObject())));
    verify(outputCollector, times(1)).reportError(any(IllegalStateException.class));
    verify(outputCollector, times(1)).ack(t1);
  }
}
