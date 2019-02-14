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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
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
  private BulkWriterResponseHandler bulkWriterResponseHandler;

  @Mock
  private BulkMessageWriter<JSONObject> bulkMessageWriter;

  @Mock
  private WriterConfiguration configurations;

  private String messageId1 = "messageId1";

  private String messageId2 = "messageId2";

  private String sensorType = "testSensor";
  private List<String> messageIds;
  private List<BulkWriterMessage<JSONObject>> messages;
  private JSONObject message1 = new JSONObject();
  private JSONObject message2 = new JSONObject();
  // batch size is used to test flushing so this could be anything
  private int maxBatchTimeout = 6;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockStatic(ErrorUtils.class);
    message1.put("value", "message1");
    message2.put("value", "message2");
    messageIds = Arrays.asList(messageId1, messageId2);
    messages = new ArrayList<BulkWriterMessage<JSONObject>>() {{
      add(new BulkWriterMessage<>(messageId1, message1));
      add(new BulkWriterMessage<>(messageId2, message2));
    }};
    when(configurations.isEnabled(any())).thenReturn(true);
    when(configurations.getBatchSize(any())).thenReturn(2);
  }

  @Test
  public void writeShouldProperlyAckTuplesInBatch() throws Exception {
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllSuccesses(messageIds);

    when(bulkMessageWriter.write(sensorType, configurations, messages)).thenReturn(response);

    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(bulkWriterResponseHandler, maxBatchTimeout);
    bulkWriterComponent.write(sensorType, messageId1, message1, bulkMessageWriter, configurations);

    verify(bulkMessageWriter, times(0)).write(eq(sensorType), eq(configurations), any());
    verify(bulkWriterResponseHandler, times(0)).handleFlush(any(), any());

    bulkWriterComponent.write(sensorType, messageId2, message2, bulkMessageWriter, configurations);

    BulkWriterResponse expectedResponse = new BulkWriterResponse();
    expectedResponse.addAllSuccesses(messageIds);
    verify(bulkMessageWriter, times(1)).write(sensorType, configurations,
            Arrays.asList(new BulkWriterMessage<>(messageId1, message1), new BulkWriterMessage<>(messageId2, message2)));
    verify(bulkWriterResponseHandler, times(1)).handleFlush(sensorType, expectedResponse);

    verifyNoMoreInteractions(bulkMessageWriter, bulkWriterResponseHandler);
  }

  @Test
  public void writeShouldFlushPreviousMessagesWhenDisabled() throws Exception {
    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(bulkWriterResponseHandler, maxBatchTimeout);

    when(configurations.isEnabled(sensorType)).thenReturn(false);

    bulkWriterComponent.write(sensorType, messageId1, message1, bulkMessageWriter, configurations);

    BulkWriterResponse expectedDisabledResponse = new BulkWriterResponse();
    expectedDisabledResponse.addSuccess(messageId1);

    verify(bulkMessageWriter, times(0)).write(eq(sensorType), eq(configurations), any());
    verify(bulkWriterResponseHandler, times(1)).handleFlush(sensorType, expectedDisabledResponse);

    verifyNoMoreInteractions(bulkMessageWriter, bulkWriterResponseHandler);
  }

  @Test
  public void writeShouldProperlyHandleWriterErrors() throws Exception {
    Throwable e = new Exception("test exception");
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllErrors(e, messageIds);

    when(bulkMessageWriter.write(sensorType, configurations, messages)).thenReturn(response);

    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(bulkWriterResponseHandler, maxBatchTimeout);
    bulkWriterComponent.write(sensorType, messageId1, message1, bulkMessageWriter, configurations);
    bulkWriterComponent.write(sensorType, messageId2, message2, bulkMessageWriter, configurations);

    BulkWriterResponse expectedErrorResponse = new BulkWriterResponse();
    expectedErrorResponse.addAllErrors(e, messageIds);

    verify(bulkWriterResponseHandler, times(1)).handleFlush(sensorType, expectedErrorResponse);
    verifyNoMoreInteractions(bulkWriterResponseHandler);
  }

  @Test
  public void writeShouldProperlyHandleWriterException() throws Exception {
    Throwable e = new Exception("test exception");
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllErrors(e, messageIds);

    when(bulkMessageWriter.write(sensorType, configurations, messages)).thenThrow(e);

    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(bulkWriterResponseHandler, maxBatchTimeout);
    bulkWriterComponent.write(sensorType, messageId1, message1, bulkMessageWriter, configurations);
    bulkWriterComponent.write(sensorType, messageId2, message2, bulkMessageWriter, configurations);

    BulkWriterResponse expectedErrorResponse = new BulkWriterResponse();
    expectedErrorResponse.addAllErrors(e, messageIds);

    verify(bulkWriterResponseHandler, times(1)).handleFlush(sensorType, expectedErrorResponse);
    verifyNoMoreInteractions(bulkWriterResponseHandler);
  }

  @Test
  public void flushShouldAckMissingTuples() throws Exception{
    BulkMessageWriter<JSONObject> bulkMessageWriter = mock(BulkMessageWriter.class);
    String successId = "successId";
    String errorId = "errorId";
    String missingId = "missingId";
    JSONObject successMessage = new JSONObject();
    successMessage.put("name", "success");
    JSONObject errorMessage = new JSONObject();
    errorMessage.put("name", "error");
    JSONObject missingMessage = new JSONObject();
    missingMessage.put("name", "missing");
    List<BulkWriterMessage<JSONObject>> allMessages = new ArrayList<BulkWriterMessage<JSONObject>>() {{
      add(new BulkWriterMessage<>(successId, successMessage));
      add(new BulkWriterMessage<>(errorId, errorMessage));
      add(new BulkWriterMessage<>(missingId, missingMessage));
    }};
    BulkWriterResponse bulkWriterResponse = new BulkWriterResponse();
    bulkWriterResponse.addSuccess(successId);
    Throwable throwable = mock(Throwable.class);
    bulkWriterResponse.addError(throwable, errorId);

    when(bulkMessageWriter.write(sensorType, configurations, allMessages)).thenReturn(bulkWriterResponse);

    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(bulkWriterResponseHandler, maxBatchTimeout);
    bulkWriterComponent.flush(sensorType, bulkMessageWriter, configurations, allMessages);

    BulkWriterResponse expectedResponse = new BulkWriterResponse();
    expectedResponse.addSuccess(successId);
    expectedResponse.addError(throwable, errorId);
    expectedResponse.addSuccess(missingId);

    verify(bulkWriterResponseHandler, times(1)).handleFlush(sensorType, expectedResponse);
    verifyNoMoreInteractions(bulkWriterResponseHandler);
  }
}
