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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.writer.MessageId;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BulkWriterComponent.class, ErrorUtils.class})
public class BulkWriterComponentTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Mock
  private FlushPolicy<JSONObject> flushPolicy;

  @Mock
  private BulkMessageWriter<JSONObject> bulkMessageWriter;

  @Mock
  private WriterConfiguration configurations;

  private MessageId messageId1 = new MessageId("messageId1");
  private MessageId messageId2 = new MessageId("messageId2");
  private String sensorType = "testSensor";
  private List<MessageId> messageIds;
  private List<BulkMessage<JSONObject>> messages;
  private JSONObject message1 = new JSONObject();
  private JSONObject message2 = new JSONObject();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockStatic(ErrorUtils.class);
    message1.put("value", "message1");
    message2.put("value", "message2");
    messageIds = Arrays.asList(messageId1, messageId2);
    messages = new ArrayList<BulkMessage<JSONObject>>() {{
      add(new BulkMessage<>(messageId1, message1));
      add(new BulkMessage<>(messageId2, message2));
    }};
    when(configurations.isEnabled(any())).thenReturn(true);
  }

  @Test
  public void writeShouldProperlyAckTuplesInBatch() throws Exception {
    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(Collections.singletonList(flushPolicy));
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllSuccesses(messageIds);

    when(bulkMessageWriter.write(sensorType, configurations, messages)).thenReturn(response);

    bulkWriterComponent.write(sensorType, messages.get(0), bulkMessageWriter, configurations);

    verify(bulkMessageWriter, times(0)).write(eq(sensorType), eq(configurations), any());
    verify(flushPolicy, times(1)).shouldFlush(sensorType, configurations, messages.subList(0, 1));
    verify(flushPolicy, times(0)).onFlush(any(), any());

    reset(flushPolicy);

    when(flushPolicy.shouldFlush(sensorType, configurations, messages)).thenReturn(true);

    bulkWriterComponent.write(sensorType, messages.get(1), bulkMessageWriter, configurations);

    BulkWriterResponse expectedResponse = new BulkWriterResponse();
    expectedResponse.addAllSuccesses(messageIds);
    verify(bulkMessageWriter, times(1)).write(sensorType, configurations,
            Arrays.asList(new BulkMessage<>(messageId1, message1), new BulkMessage<>(messageId2, message2)));
    verify(flushPolicy, times(1)).shouldFlush(sensorType, configurations, messages);
    verify(flushPolicy, times(1)).onFlush(sensorType, expectedResponse);

    verifyNoMoreInteractions(bulkMessageWriter, flushPolicy);
  }

  @Test
  public void writeShouldFlushPreviousMessagesWhenDisabled() throws Exception {
    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(Collections.singletonList(flushPolicy));
    BulkMessage<JSONObject> beforeDisabledMessage = messages.get(0);
    BulkMessage<JSONObject> afterDisabledMessage = messages.get(1);
    BulkWriterResponse beforeDisabledResponse = new BulkWriterResponse();
    beforeDisabledResponse.addSuccess(beforeDisabledMessage.getId());
    BulkWriterResponse afterDisabledResponse = new BulkWriterResponse();
    afterDisabledResponse.addSuccess(afterDisabledMessage.getId());

    when(bulkMessageWriter.write(sensorType, configurations, Collections.singletonList(messages.get(0)))).thenReturn(beforeDisabledResponse);

    bulkWriterComponent.write(sensorType, beforeDisabledMessage, bulkMessageWriter, configurations);

    verify(bulkMessageWriter, times(0)).write(eq(sensorType), eq(configurations), any());
    verify(flushPolicy, times(1)).shouldFlush(sensorType, configurations, messages.subList(0, 1));
    verify(flushPolicy, times(0)).onFlush(any(), any());

    when(configurations.isEnabled(sensorType)).thenReturn(false);

    bulkWriterComponent.write(sensorType, messages.get(1), bulkMessageWriter, configurations);

    verify(bulkMessageWriter, times(1)).write(sensorType, configurations, Collections.singletonList(messages.get(0)));
    verify(flushPolicy, times(1)).onFlush(sensorType, beforeDisabledResponse);
    verify(flushPolicy, times(1)).onFlush(sensorType, afterDisabledResponse);

    verifyNoMoreInteractions(bulkMessageWriter, flushPolicy);
  }

  @Test
  public void writeShouldProperlyHandleWriterErrors() throws Exception {
    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(Collections.singletonList(flushPolicy));
    Throwable e = new Exception("test exception");
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllErrors(e, messageIds);

    when(bulkMessageWriter.write(sensorType, configurations, messages)).thenReturn(response);

    bulkWriterComponent.write(sensorType, messages.get(0), bulkMessageWriter, configurations);

    verify(bulkMessageWriter, times(0)).write(eq(sensorType), eq(configurations), any());
    verify(flushPolicy, times(1)).shouldFlush(sensorType, configurations, messages.subList(0, 1));
    verify(flushPolicy, times(0)).onFlush(any(), any());

    reset(flushPolicy);

    when(flushPolicy.shouldFlush(sensorType, configurations, messages)).thenReturn(true);

    bulkWriterComponent.write(sensorType, messages.get(1), bulkMessageWriter, configurations);

    BulkWriterResponse expectedErrorResponse = new BulkWriterResponse();
    expectedErrorResponse.addAllErrors(e, messageIds);

    verify(bulkMessageWriter, times(1)).write(sensorType, configurations, messages);
    verify(flushPolicy, times(1)).shouldFlush(sensorType, configurations, messages);
    verify(flushPolicy, times(1)).onFlush(sensorType, expectedErrorResponse);

    verifyNoMoreInteractions(bulkMessageWriter, flushPolicy);
  }

  @Test
  public void writeShouldProperlyHandleWriterException() throws Exception {
    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(Collections.singletonList(flushPolicy));
    Throwable e = new Exception("test exception");
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllErrors(e, messageIds);

    when(bulkMessageWriter.write(sensorType, configurations, messages)).thenThrow(e);

    bulkWriterComponent.write(sensorType, messages.get(0), bulkMessageWriter, configurations);

    verify(bulkMessageWriter, times(0)).write(eq(sensorType), eq(configurations), any());
    verify(flushPolicy, times(1)).shouldFlush(sensorType, configurations, messages.subList(0, 1));
    verify(flushPolicy, times(0)).onFlush(any(), any());

    reset(flushPolicy);

    when(flushPolicy.shouldFlush(sensorType, configurations, messages)).thenReturn(true);

    bulkWriterComponent.write(sensorType, messages.get(1), bulkMessageWriter, configurations);

    BulkWriterResponse expectedErrorResponse = new BulkWriterResponse();
    expectedErrorResponse.addAllErrors(e, messageIds);

    verify(bulkMessageWriter, times(1)).write(sensorType, configurations, messages);
    verify(flushPolicy, times(1)).shouldFlush(sensorType, configurations, messages);
    verify(flushPolicy, times(1)).onFlush(sensorType, expectedErrorResponse);

    verifyNoMoreInteractions(flushPolicy);
  }

  @Test
  public void flushShouldAckMissingTuples() throws Exception{
    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(Collections.singletonList(flushPolicy));
    BulkMessageWriter<JSONObject> bulkMessageWriter = mock(BulkMessageWriter.class);
    MessageId successId = new MessageId("successId");
    MessageId errorId = new MessageId("errorId");
    MessageId missingId = new MessageId("missingId");
    JSONObject successMessage = new JSONObject();
    successMessage.put("name", "success");
    JSONObject errorMessage = new JSONObject();
    errorMessage.put("name", "error");
    JSONObject missingMessage = new JSONObject();
    missingMessage.put("name", "missing");
    List<BulkMessage<JSONObject>> allMessages = new ArrayList<BulkMessage<JSONObject>>() {{
      add(new BulkMessage<>(successId, successMessage));
      add(new BulkMessage<>(errorId, errorMessage));
      add(new BulkMessage<>(missingId, missingMessage));
    }};
    BulkWriterResponse bulkWriterResponse = new BulkWriterResponse();
    bulkWriterResponse.addSuccess(successId);
    Throwable throwable = mock(Throwable.class);
    bulkWriterResponse.addError(throwable, errorId);

    when(bulkMessageWriter.write(sensorType, configurations, allMessages)).thenReturn(bulkWriterResponse);

    bulkWriterComponent.flush(sensorType, bulkMessageWriter, configurations, allMessages);

    BulkWriterResponse expectedResponse = new BulkWriterResponse();
    expectedResponse.addSuccess(successId);
    expectedResponse.addError(throwable, errorId);
    expectedResponse.addSuccess(missingId);

    verify(flushPolicy, times(1)).onFlush(sensorType, expectedResponse);
    verifyNoMoreInteractions(flushPolicy);
  }

  @Test
  public void flushAllShouldFlushAllSensors() {
    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(Collections.singletonList(flushPolicy));

    bulkWriterComponent.write("sensor1", messages.get(0), bulkMessageWriter, configurations);
    bulkWriterComponent.write("sensor2", messages.get(1), bulkMessageWriter, configurations);

    reset(flushPolicy);

    bulkWriterComponent.flushAll(bulkMessageWriter, configurations);

    verify(flushPolicy, times(1)).shouldFlush("sensor1", configurations, messages.subList(0, 1));
    verify(flushPolicy, times(1)).shouldFlush("sensor2", configurations, messages.subList(1, 2));

    verifyNoMoreInteractions(flushPolicy);
  }
}
