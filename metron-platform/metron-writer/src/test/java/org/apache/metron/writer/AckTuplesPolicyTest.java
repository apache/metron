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
package org.apache.metron.writer;

import org.apache.metron.common.Constants;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.writer.MessageId;
import org.apache.metron.test.error.MetronErrorJSONMatcher;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;

public class AckTuplesPolicyTest {


  @Mock
  private OutputCollector collector;

  @Mock
  private MessageGetStrategy messageGetStrategy;

  @Mock
  private Tuple tuple1;

  @Mock
  private Tuple tuple2;

  private String sensorType = "testSensor";

  private AckTuplesPolicy ackTuplesPolicy;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    ackTuplesPolicy = new AckTuplesPolicy(collector, messageGetStrategy);
  }

  @Test
  public void shouldProperlyHandleSuccessAndErrors() throws Exception {
    String messageId1 = "messageId1";
    String messageId2 = "messageId2";
    String messageId3 = "messageId3";
    JSONObject message1 = new JSONObject();
    JSONObject message2 = new JSONObject();
    JSONObject message3 = new JSONObject();
    message1.put("value", "message1");
    message2.put("value", "message2");
    message3.put("value", "message3");
    Tuple tuple3 = mock(Tuple.class);
    Throwable e = new Exception("test exception");
    MetronError expectedError1 = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR).withThrowable(e).withRawMessages(Collections.singletonList(message1));
    MetronError expectedError2 = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR).withThrowable(e).withRawMessages(Collections.singletonList(message2));
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllErrors(e, Arrays.asList(new MessageId(messageId1), new MessageId(messageId2)));
    response.addSuccess(new MessageId(messageId3));

    when(messageGetStrategy.get(tuple1)).thenReturn(message1);
    when(messageGetStrategy.get(tuple2)).thenReturn(message2);

    ackTuplesPolicy.addTupleMessageIds(tuple1, Collections.singleton(messageId1));
    ackTuplesPolicy.addTupleMessageIds(tuple2, Collections.singleton(messageId2));
    ackTuplesPolicy.addTupleMessageIds(tuple3, Collections.singleton(messageId3));

    ackTuplesPolicy.onFlush(sensorType, response);

    assertEquals(0, ackTuplesPolicy.getTupleMessageMap().size());
    assertEquals(0, ackTuplesPolicy.getTupleErrorMap().size());
    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM),
            new Values(argThat(new MetronErrorJSONMatcher(expectedError1.getJSONObject()))));
    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM),
            new Values(argThat(new MetronErrorJSONMatcher(expectedError2.getJSONObject()))));
    verify(collector, times(1)).ack(tuple1);
    verify(collector, times(1)).ack(tuple2);
    verify(collector, times(1)).ack(tuple3);
    verify(collector, times(1)).reportError(e);
    verifyNoMoreInteractions(collector);
  }

  @Test
  public void shouldOnlyReportErrorsOncePerBatch() {
    AckTuplesPolicy ackTuplesPolicy = new AckTuplesPolicy(collector, messageGetStrategy);
    JSONObject rawMessage1 = new JSONObject();
    JSONObject rawMessage2 = new JSONObject();
    rawMessage1.put("value", "rawMessage1");
    rawMessage2.put("value", "rawMessage2");
    String messageId1 = "messageId1";
    String messageId2 = "messageId2";
    String messageId3 = "messageId3";
    JSONObject message1 = new JSONObject();
    JSONObject message2 = new JSONObject();
    JSONObject message3 = new JSONObject();
    message1.put("value", "message1");
    message2.put("value", "message2");
    message3.put("value", "message3");

    Throwable e1 = new Exception("test exception 1");
    Throwable e2 = new Exception("test exception 2");
    MetronError expectedError1 = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR).withThrowable(e1).withRawMessages(Collections.singletonList(rawMessage1));
    MetronError expectedError2 = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR).withThrowable(e2).withRawMessages(Collections.singletonList(rawMessage1));
    MetronError expectedError3 = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR).withThrowable(e1).withRawMessages(Collections.singletonList(rawMessage2));

    when(messageGetStrategy.get(tuple1)).thenReturn(rawMessage1);
    when(messageGetStrategy.get(tuple2)).thenReturn(rawMessage2);

    ackTuplesPolicy.addTupleMessageIds(tuple1, Arrays.asList(messageId1, messageId2));
    ackTuplesPolicy.addTupleMessageIds(tuple2, Collections.singletonList(messageId3));

    BulkWriterResponse response = new BulkWriterResponse();
    response.addError(e1, new MessageId(messageId1));

    ackTuplesPolicy.onFlush(sensorType, response);

    assertEquals(2, ackTuplesPolicy.getTupleMessageMap().size());
    assertEquals(1, ackTuplesPolicy.getTupleErrorMap().size());
    verify(collector, times(0)).ack(any());
    verify(collector, times(0)).reportError(any());
    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM), new Values(argThat(new MetronErrorJSONMatcher(expectedError1.getJSONObject()))));

    response = new BulkWriterResponse();
    response.addError(e2, new MessageId(messageId2));
    response.addError(e1, new MessageId(messageId3));

    ackTuplesPolicy.onFlush(sensorType, response);

    assertEquals(0, ackTuplesPolicy.getTupleMessageMap().size());
    assertEquals(0, ackTuplesPolicy.getTupleErrorMap().size());
    verify(collector, times(1)).ack(tuple1);
    verify(collector, times(1)).ack(tuple2);
    verify(collector, times(1)).reportError(e1);
    verify(collector, times(1)).reportError(e2);
    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM), new Values(argThat(new MetronErrorJSONMatcher(expectedError2.getJSONObject()))));
    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM), new Values(argThat(new MetronErrorJSONMatcher(expectedError3.getJSONObject()))));
    verifyNoMoreInteractions(collector);
  }

  @Test
  public void shouldProperlyAckTuples() {
    ackTuplesPolicy.addTupleMessageIds(tuple1, Collections.singletonList("message1"));
    ackTuplesPolicy.addTupleMessageIds(tuple2, Collections.singletonList("message2"));

    BulkWriterResponse response = new BulkWriterResponse();
    response.addSuccess(new MessageId("message1"));
    response.addSuccess(new MessageId("message2"));

    ackTuplesPolicy.onFlush(sensorType, response);

    assertEquals(0, ackTuplesPolicy.getTupleMessageMap().size());
    verify(collector, times(1)).ack(tuple1);
    verify(collector, times(1)).ack(tuple2);
    verifyNoMoreInteractions(collector);
  }

  @Test
  public void shouldOnlyAckTupleAfterHandlingAllMessages() {
    ackTuplesPolicy.addTupleMessageIds(tuple1, Arrays.asList("message1", "message2", "message3"));

    BulkWriterResponse response = new BulkWriterResponse();
    response.addSuccess(new MessageId("message1"));
    response.addSuccess(new MessageId("message2"));

    ackTuplesPolicy.onFlush(sensorType, response);
    verify(collector, times(0)).ack(any());

    response = new BulkWriterResponse();
    response.addSuccess(new MessageId("message3"));

    ackTuplesPolicy.onFlush(sensorType, response);

    assertEquals(0, ackTuplesPolicy.getTupleMessageMap().size());
    verify(collector, times(1)).ack(tuple1);
    verifyNoMoreInteractions(collector);
  }
}
