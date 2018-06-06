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
package org.apache.metron.enrichment.bolt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.log4j.Level;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.message.MessageGetters;
import org.apache.metron.common.system.FakeClock;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.test.bolt.BaseEnrichmentBoltTest;
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.metron.writer.BulkWriterComponent;
import org.apache.metron.writer.bolt.BulkMessageWriterBolt;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.hamcrest.Description;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;

public class BulkMessageWriterBoltTest extends BaseEnrichmentBoltTest {

  protected class MessageListMatcher extends ArgumentMatcher<List<JSONObject>> {

    private List<JSONObject> expectedMessageList;

    public MessageListMatcher(List<JSONObject> expectedMessageList) {
      this.expectedMessageList = expectedMessageList;
    }

    @Override
    public boolean matches(Object o) {
      List<JSONObject> actualMessageList = (List<JSONObject>) o;
      for(JSONObject message: actualMessageList) removeTimingFields(message);
      return expectedMessageList.equals(actualMessageList);
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(String.format("[%s]", expectedMessageList));
    }

  }

  /**
   * {
   * "field": "value",
   * "source.type": "test"
   * }
   */
  @Multiline
  private String sampleMessageString;

  private JSONObject sampleMessage;
  private List<JSONObject> messageList;
  private List<JSONObject> fullMessageList;
  private List<Tuple> tupleList;

  @Before
  public void parseMessages() throws ParseException {
    JSONParser parser = new JSONParser();
    fullMessageList = new ArrayList<>();
    sampleMessage = (JSONObject) parser.parse(sampleMessageString);
    sampleMessage.put("field", "value1");
    fullMessageList.add(((JSONObject) sampleMessage.clone()));
    sampleMessage.put("field", "value2");
    fullMessageList.add(((JSONObject) sampleMessage.clone()));
    sampleMessage.put("field", "value3");
    fullMessageList.add(((JSONObject) sampleMessage.clone()));
    sampleMessage.put("field", "value4");
    fullMessageList.add(((JSONObject) sampleMessage.clone()));
    sampleMessage.put("field", "value5");
    fullMessageList.add(((JSONObject) sampleMessage.clone()));
  }

  @Mock
  private BulkMessageWriter<JSONObject> bulkMessageWriter;

  @Mock
  private MessageGetStrategy messageGetStrategy;

  @Test
  public void testSourceTypeMissing() throws Exception {
    // setup the bolt
    BulkMessageWriterBolt<IndexingConfigurations> bulkMessageWriterBolt = new BulkMessageWriterBolt<IndexingConfigurations>(
          "zookeeperUrl", "INDEXING")
            .withBulkMessageWriter(bulkMessageWriter)
            .withMessageGetter(MessageGetters.JSON_FROM_FIELD.name())
            .withMessageGetterField("message");
    bulkMessageWriterBolt.setCuratorFramework(client);
    bulkMessageWriterBolt.setZKCache(cache);
    bulkMessageWriterBolt.getConfigurations().updateSensorIndexingConfig(sensorType,
            new FileInputStream(sampleSensorIndexingConfigPath));

    // initialize the bolt
    bulkMessageWriterBolt.declareOutputFields(declarer);
    Map stormConf = new HashMap();
    bulkMessageWriterBolt.prepare(stormConf, topologyContext, outputCollector);

    // create a message with no source type
    JSONObject message = (JSONObject) new JSONParser().parse(sampleMessageString);
    message.remove("source.type");
    when(tuple.getValueByField("message")).thenReturn(message);

    // the tuple should be handled as an error and ack'd
    bulkMessageWriterBolt.execute(tuple);
    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM), any());
    verify(outputCollector, times(1)).ack(tuple);
  }

  @Test
  public void testFlushOnBatchSize() throws Exception {
    BulkMessageWriterBolt<IndexingConfigurations> bulkMessageWriterBolt = new BulkMessageWriterBolt<IndexingConfigurations>(
        "zookeeperUrl", "INDEXING")
        .withBulkMessageWriter(bulkMessageWriter)
        .withMessageGetter(MessageGetters.JSON_FROM_FIELD.name())
        .withMessageGetterField("message");
    bulkMessageWriterBolt.setCuratorFramework(client);
    bulkMessageWriterBolt.setZKCache(cache);
    bulkMessageWriterBolt.getConfigurations().updateSensorIndexingConfig(sensorType,
            new FileInputStream(sampleSensorIndexingConfigPath));
    bulkMessageWriterBolt.declareOutputFields(declarer);
    verify(declarer, times(1)).declareStream(eq("error"), argThat(
            new FieldsMatcher("message")));
    Map stormConf = new HashMap();
    doThrow(new Exception()).when(bulkMessageWriter).init(eq(stormConf),any(TopologyContext.class), any(WriterConfiguration.class));
    try {
      bulkMessageWriterBolt.prepare(stormConf, topologyContext, outputCollector);
      fail("A runtime exception should be thrown when bulkMessageWriter.init throws an exception");
    } catch(RuntimeException e) {}
    reset(bulkMessageWriter);
    when(bulkMessageWriter.getName()).thenReturn("hdfs");
    bulkMessageWriterBolt.prepare(stormConf, topologyContext, outputCollector);
    verify(bulkMessageWriter, times(1)).init(eq(stormConf),any(TopologyContext.class), any(WriterConfiguration.class));
    tupleList = new ArrayList<>();
    messageList = new ArrayList<>();
    for(int i = 0; i < 4; i++) {
      when(tuple.getValueByField("message")).thenReturn(fullMessageList.get(i));
      tupleList.add(tuple);
      messageList.add(fullMessageList.get(i));
      bulkMessageWriterBolt.execute(tuple);
      verify(bulkMessageWriter, times(0)).write(eq(sensorType)
              , any(WriterConfiguration.class), eq(tupleList), eq(messageList));
    }
    when(tuple.getValueByField("message")).thenReturn(fullMessageList.get(4));
    tupleList.add(tuple);
    messageList.add(fullMessageList.get(4));
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllSuccesses(tupleList);
    when(bulkMessageWriter.write(eq(sensorType), any(WriterConfiguration.class), eq(tupleList)
            , argThat(new MessageListMatcher(messageList)))).thenReturn(response);
    bulkMessageWriterBolt.execute(tuple);
    verify(bulkMessageWriter, times(1)).write(eq(sensorType)
            , any(WriterConfiguration.class), eq(tupleList)
            , argThat(new MessageListMatcher(messageList)));
    verify(outputCollector, times(5)).ack(tuple);
    reset(outputCollector);
    doThrow(new Exception()).when(bulkMessageWriter).write(eq(sensorType), any(WriterConfiguration.class)
            , Matchers.anyListOf(Tuple.class), Matchers.anyListOf(JSONObject.class));
    when(tuple.getValueByField("message")).thenReturn(fullMessageList.get(0));
    UnitTestHelper.setLog4jLevel(BulkWriterComponent.class, Level.FATAL);
    for(int i = 0; i < 5; i++) {
      bulkMessageWriterBolt.execute(tuple);
    }
    UnitTestHelper.setLog4jLevel(BulkWriterComponent.class, Level.ERROR);
    verify(outputCollector, times(5)).ack(tuple);
    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM), any(Values.class));
    verify(outputCollector, times(1)).reportError(any(Throwable.class));
  }

  @Test
  public void testFlushOnBatchTimeout() throws Exception {
    FakeClock clock = new FakeClock();
    BulkMessageWriterBolt<IndexingConfigurations> bulkMessageWriterBolt = new BulkMessageWriterBolt<IndexingConfigurations>(
        "zookeeperUrl", "INDEXING")
        .withBulkMessageWriter(bulkMessageWriter)
        .withMessageGetter(MessageGetters.JSON_FROM_FIELD.name())
        .withMessageGetterField("message")
        .withBatchTimeoutDivisor(3);
    bulkMessageWriterBolt.setCuratorFramework(client);
    bulkMessageWriterBolt.setZKCache(cache);
    bulkMessageWriterBolt.getConfigurations().updateSensorIndexingConfig(sensorType,
            new FileInputStream(sampleSensorIndexingConfigPath));
    bulkMessageWriterBolt.declareOutputFields(declarer);
    verify(declarer, times(1)).declareStream(eq("error")
            , argThat(new FieldsMatcher("message")));
    Map stormConf = new HashMap();
    when(bulkMessageWriter.getName()).thenReturn("elasticsearch");
    bulkMessageWriterBolt.prepare(stormConf, topologyContext, outputCollector, clock);
    verify(bulkMessageWriter, times(1)).init(eq(stormConf),any(TopologyContext.class), any(WriterConfiguration.class));
    int batchTimeout = bulkMessageWriterBolt.getDefaultBatchTimeout();
    assertEquals(4, batchTimeout);
    tupleList = new ArrayList<>();
    messageList = new ArrayList<>();
    for(int i = 0; i < 3; i++) {
      when(tuple.getValueByField("message")).thenReturn(fullMessageList.get(i));
      tupleList.add(tuple);
      messageList.add(fullMessageList.get(i));
      bulkMessageWriterBolt.execute(tuple);
      verify(bulkMessageWriter, times(0)).write(eq(sensorType)
              , any(WriterConfiguration.class), eq(tupleList), eq(messageList));
    }
    clock.elapseSeconds(5);
    when(tuple.getValueByField("message")).thenReturn(fullMessageList.get(3));
    tupleList.add(tuple);
    messageList.add(fullMessageList.get(3));
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllSuccesses(tupleList);
    when(bulkMessageWriter.write(eq(sensorType), any(WriterConfiguration.class), eq(tupleList)
            , argThat(new MessageListMatcher(messageList)))).thenReturn(response);
    bulkMessageWriterBolt.execute(tuple);
    verify(bulkMessageWriter, times(1)).write(eq(sensorType)
            , any(WriterConfiguration.class)
            , eq(tupleList), argThat(new MessageListMatcher(messageList)));
    verify(outputCollector, times(4)).ack(tuple);
  }

  @Test
  public void testFlushOnTickTuple() throws Exception {
    FakeClock clock = new FakeClock();
    BulkMessageWriterBolt<IndexingConfigurations> bulkMessageWriterBolt = new BulkMessageWriterBolt<IndexingConfigurations>(
        "zookeeperUrl", "INDEXING")
        .withBulkMessageWriter(bulkMessageWriter)
        .withMessageGetter(MessageGetters.JSON_FROM_FIELD.name())
        .withMessageGetterField("message");
    bulkMessageWriterBolt.setCuratorFramework(client);
    bulkMessageWriterBolt.setZKCache(cache);
    bulkMessageWriterBolt.getConfigurations().updateSensorIndexingConfig(sensorType
            , new FileInputStream(sampleSensorIndexingConfigPath));
    bulkMessageWriterBolt.declareOutputFields(declarer);
    verify(declarer, times(1)).declareStream(eq("error")
            , argThat(new FieldsMatcher("message")));
    Map stormConf = new HashMap();
    when(bulkMessageWriter.getName()).thenReturn("elasticsearch");
    bulkMessageWriterBolt.prepare(stormConf, topologyContext, outputCollector, clock);
    verify(bulkMessageWriter, times(1)).init(eq(stormConf),any(TopologyContext.class)
            , any(WriterConfiguration.class));
    int batchTimeout = bulkMessageWriterBolt.getDefaultBatchTimeout();
    assertEquals(14, batchTimeout);
    tupleList = new ArrayList<>();
    messageList = new ArrayList<>();
    for(int i = 0; i < 3; i++) {
      when(tuple.getValueByField("message")).thenReturn(fullMessageList.get(i));
      tupleList.add(tuple);
      messageList.add(fullMessageList.get(i));
      bulkMessageWriterBolt.execute(tuple);
      verify(bulkMessageWriter, times(0)).write(eq(sensorType)
              , any(WriterConfiguration.class), eq(tupleList), eq(messageList));
    }
    when(tuple.getValueByField("message")).thenReturn(null);
    when(tuple.getSourceComponent()).thenReturn("__system"); //mark the tuple as a TickTuple, part 1 of 2
    when(tuple.getSourceStreamId()).thenReturn("__tick");    //mark the tuple as a TickTuple, part 2 of 2
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllSuccesses(tupleList);
    when(bulkMessageWriter.write(eq(sensorType), any(WriterConfiguration.class), eq(tupleList)
            , argThat(new MessageListMatcher(messageList)))).thenReturn(response);
    clock.advanceToSeconds(2);
    bulkMessageWriterBolt.execute(tuple);
    verify(bulkMessageWriter, times(0)).write(eq(sensorType)
            , any(WriterConfiguration.class)
            , eq(tupleList), argThat(new MessageListMatcher(messageList)));
    verify(outputCollector, times(1)).ack(tuple);  // 1 tick
    clock.advanceToSeconds(9);
    bulkMessageWriterBolt.execute(tuple);
    verify(bulkMessageWriter, times(1)).write(eq(sensorType)
            , any(WriterConfiguration.class)
            , eq(tupleList), argThat(new MessageListMatcher(messageList)));
    assertEquals(3, tupleList.size());
    verify(outputCollector, times(5)).ack(tuple);  // 3 messages + 2nd tick
  }

  /**
   * If an invalid message is sent to indexing, the message should be handled as an error
   * and the topology should continue processing.
   */
  @Test
  public void testMessageInvalid() throws Exception {
    FakeClock clock = new FakeClock();

    // setup the bolt
    BulkMessageWriterBolt<IndexingConfigurations> bolt = new BulkMessageWriterBolt<IndexingConfigurations>(
        "zookeeperUrl", "INDEXING")
            .withBulkMessageWriter(bulkMessageWriter)
            .withMessageGetter(MessageGetters.JSON_FROM_POSITION.name())
            .withMessageGetterField("message");
    bolt.setCuratorFramework(client);
    bolt.setZKCache(cache);
    bolt.getConfigurations().updateSensorIndexingConfig(sensorType, new FileInputStream(sampleSensorIndexingConfigPath));

    // initialize the bolt
    bolt.declareOutputFields(declarer);
    Map stormConf = new HashMap();
    bolt.prepare(stormConf, topologyContext, outputCollector, clock);

    // execute a tuple that contains an invalid message
    byte[] invalidJSON = "this is not valid JSON".getBytes();
    when(tuple.getBinary(0)).thenReturn(invalidJSON);
    bolt.execute(tuple);

    // the tuple should be handled as an error and ack'd
    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM), any());
    verify(outputCollector, times(1)).ack(tuple);
  }

}
