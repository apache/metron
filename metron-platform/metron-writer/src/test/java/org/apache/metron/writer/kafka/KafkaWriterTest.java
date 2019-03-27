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

package org.apache.metron.writer.kafka;

import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.writer.ParserWriterConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.writer.MessageId;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class KafkaWriterTest {

  @Mock
  private KafkaProducer kafkaProducer;

  public static final String SENSOR_TYPE = "test";
  public WriterConfiguration createConfiguration(final Map<String, Object> parserConfig) {
    ParserConfigurations configurations = new ParserConfigurations();
    configurations.updateSensorParserConfig( SENSOR_TYPE
                                           , new SensorParserConfig() {{
                                              setParserConfig(parserConfig);
                                                                      }}
                                           );
    return new ParserWriterConfiguration(configurations);
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testHappyPathGlobalConfig() throws Exception {
    KafkaWriter writer = new KafkaWriter();
    WriterConfiguration configuration = createConfiguration(
            new HashMap<String, Object>() {{
              put("kafka.brokerUrl" , "localhost:6667");
              put("kafka.topic" , SENSOR_TYPE);
              put("kafka.producerConfigs" , ImmutableMap.of("key1", 1, "key2", "value2"));
            }}
    );

    writer.configure(SENSOR_TYPE, configuration);
    Map<String, Object> producerConfigs = writer.createProducerConfigs();
    assertEquals(producerConfigs.get("bootstrap.servers"), "localhost:6667");
    assertEquals(producerConfigs.get("key.serializer"), "org.apache.kafka.common.serialization.StringSerializer");
    assertEquals(producerConfigs.get("value.serializer"), "org.apache.kafka.common.serialization.StringSerializer");
    assertEquals(producerConfigs.get("request.required.acks"), 1);
    assertEquals(producerConfigs.get("key1"), 1);
    assertEquals(producerConfigs.get("key2"), "value2");
  }

  @Test
  public void testHappyPathGlobalConfigWithPrefix() throws Exception {
    KafkaWriter writer = new KafkaWriter();
    writer.withConfigPrefix("prefix");
    WriterConfiguration configuration = createConfiguration(
            new HashMap<String, Object>() {{
              put("prefix.kafka.brokerUrl" , "localhost:6667");
              put("prefix.kafka.topic" , SENSOR_TYPE);
              put("prefix.kafka.producerConfigs" , ImmutableMap.of("key1", 1, "key2", "value2"));
            }}
    );

    writer.configure(SENSOR_TYPE, configuration);
    Map<String, Object> producerConfigs = writer.createProducerConfigs();
    assertEquals(producerConfigs.get("bootstrap.servers"), "localhost:6667");
    assertEquals(producerConfigs.get("key.serializer"), "org.apache.kafka.common.serialization.StringSerializer");
    assertEquals(producerConfigs.get("value.serializer"), "org.apache.kafka.common.serialization.StringSerializer");
    assertEquals(producerConfigs.get("request.required.acks"), 1);
    assertEquals(producerConfigs.get("key1"), 1);
    assertEquals(producerConfigs.get("key2"), "value2");
  }

  @Test
  public void testTopicField_bothTopicAndFieldSpecified() throws Exception {
    KafkaWriter writer = new KafkaWriter();
    WriterConfiguration configuration = createConfiguration(
            new HashMap<String, Object>() {{
              put("kafka.brokerUrl" , "localhost:6667");
              put("kafka.topic" , SENSOR_TYPE);
              put("kafka.topicField" , "kafka_topic");
              put("kafka.producerConfigs" , ImmutableMap.of("key1", 1, "key2", "value2"));
            }}
    );

    writer.configure(SENSOR_TYPE, configuration);
    assertEquals( "metron"
                       , writer.getKafkaTopic(new JSONObject() {{
                          put("kafka_topic", "metron");
                         }}).get()
                       );
    Assert.assertFalse( writer.getKafkaTopic(new JSONObject()).isPresent() );

  }

  @Test
  public void testTopicField_onlyFieldSpecified() throws Exception {
    KafkaWriter writer = new KafkaWriter();
    WriterConfiguration configuration = createConfiguration(
            new HashMap<String, Object>() {{
              put("kafka.brokerUrl" , "localhost:6667");
              put("kafka.topicField" , "kafka_topic");
              put("kafka.producerConfigs" , ImmutableMap.of("key1", 1, "key2", "value2"));
            }}
    );

    writer.configure(SENSOR_TYPE, configuration);
    assertEquals( "metron"
                       , writer.getKafkaTopic(new JSONObject() {{
                          put("kafka_topic", "metron");
                         }}).get()
                       );
    Assert.assertFalse( writer.getKafkaTopic(new JSONObject()).isPresent() );
  }

  @Test
  public void testTopicField_neitherSpecified() throws Exception {
    KafkaWriter writer = new KafkaWriter();
    WriterConfiguration configuration = createConfiguration(
            new HashMap<String, Object>() {{
              put("kafka.brokerUrl" , "localhost:6667");
              put("kafka.producerConfigs" , ImmutableMap.of("key1", 1, "key2", "value2"));
            }}
    );

    writer.configure(SENSOR_TYPE, configuration);
    assertEquals(Constants.ENRICHMENT_TOPIC
                       , writer.getKafkaTopic(new JSONObject() {{
                          put("kafka_topic", "metron");
                         }}).get()
                       );
    Assert.assertTrue( writer.getKafkaTopic(new JSONObject()).isPresent() );
  }

  @Test
  public void testWriterShouldReturnResponse() throws Exception {
    KafkaWriter writer = spy(new KafkaWriter());
    writer.setKafkaProducer(kafkaProducer);

    List<BulkMessage<JSONObject>> messages = new ArrayList<>();
    JSONObject successMessage = new JSONObject();
    successMessage.put("value", "success");
    JSONObject errorMessage = new JSONObject();
    errorMessage.put("value", "error");
    JSONObject droppedMessage = new JSONObject();
    droppedMessage.put("value", "dropped");
    messages.add(new BulkMessage<>("successId", successMessage));
    messages.add(new BulkMessage<>("errorId", errorMessage));
    messages.add(new BulkMessage<>("droppedId", droppedMessage));

    doReturn(Optional.of("successTopic")).when(writer).getKafkaTopic(successMessage);
    doReturn(Optional.of("errorTopic")).when(writer).getKafkaTopic(errorMessage);
    doReturn(Optional.empty()).when(writer).getKafkaTopic(droppedMessage);

    Future successFuture = mock(Future.class);
    Future errorFuture = mock(Future.class);
    ExecutionException throwable = new ExecutionException(new Exception("kafka error"));
    when(kafkaProducer.send(new ProducerRecord<String, String>("errorTopic", "{\"value\":\"error\"}"))).thenReturn(errorFuture);
    when(kafkaProducer.send(new ProducerRecord<String, String>("successTopic", "{\"value\":\"success\"}"))).thenReturn(successFuture);

    when(errorFuture.get()).thenThrow(throwable);

    BulkWriterResponse response = new BulkWriterResponse();
    response.addSuccess(new MessageId("successId"));
    response.addError(throwable, new MessageId("errorId"));

    assertEquals(response, writer.write(SENSOR_TYPE, createConfiguration(new HashMap<>()), messages));
    verify(kafkaProducer, times(1)).flush();
    verify(kafkaProducer, times(1)).send(new ProducerRecord<String, String>("successTopic", "{\"value\":\"success\"}"));
    verify(kafkaProducer, times(1)).send(new ProducerRecord<String, String>("errorTopic", "{\"value\":\"error\"}"));
    verifyNoMoreInteractions(kafkaProducer);
  }

  @Test
  public void testWriteShouldReturnErrorsOnFailedFlush() throws Exception {
    KafkaWriter writer = spy(new KafkaWriter());
    writer.setKafkaProducer(kafkaProducer);

    List<BulkMessage<JSONObject>> messages = new ArrayList<>();
    JSONObject message1 = new JSONObject();
    message1.put("value", "message1");
    JSONObject message2 = new JSONObject();
    message2.put("value", "message2");
    messages.add(new BulkMessage<>("messageId1", message1));
    messages.add(new BulkMessage<>("messageId2", message2));

    doReturn(Optional.of("topic1")).when(writer).getKafkaTopic(message1);
    doReturn(Optional.of("topic2")).when(writer).getKafkaTopic(message2);

    Future future1 = mock(Future.class);
    Future future2 = mock(Future.class);
    when(kafkaProducer.send(new ProducerRecord<String, String>("topic1", "{\"value\":\"message1\"}"))).thenReturn(future1);
    when(kafkaProducer.send(new ProducerRecord<String, String>("topic2", "{\"value\":\"message2\"}"))).thenReturn(future2);
    InterruptException throwable = new InterruptException("kafka flush exception");
    doThrow(throwable).when(kafkaProducer).flush();

    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllErrors(throwable, Arrays.asList(new MessageId("messageId1"), new MessageId("messageId2")));

    assertEquals(response, writer.write(SENSOR_TYPE, createConfiguration(new HashMap<>()), messages));
    verify(kafkaProducer, times(1)).flush();
    verify(kafkaProducer, times(1)).send(new ProducerRecord<String, String>("topic1", "{\"value\":\"message1\"}"));
    verify(kafkaProducer, times(1)).send(new ProducerRecord<String, String>("topic2", "{\"value\":\"message2\"}"));
    verifyNoMoreInteractions(kafkaProducer);
  }
}
