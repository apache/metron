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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.ParserWriterConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.test.error.MetronErrorJSONMatcher;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
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
  private OutputCollector collector;

  @Mock
  private BulkMessageWriter<JSONObject> bulkMessageWriter;

  @Mock
  private WriterConfiguration configurations;

  @Mock
  private Tuple tuple1;

  @Mock
  private Tuple tuple2;

  @Mock
  private MessageGetStrategy messageGetStrategy;

  private String sensorType = "testSensor";
  private List<Tuple> tupleList;
  private JSONObject message1 = new JSONObject();
  private JSONObject message2 = new JSONObject();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockStatic(ErrorUtils.class);
    message1.put("value", "message1");
    message2.put("value", "message2");
    when(tuple1.getValueByField("message")).thenReturn(message1);
    when(tuple2.getValueByField("message")).thenReturn(message2);
    tupleList = Arrays.asList(tuple1, tuple2);
    when(configurations.isEnabled(any())).thenReturn(true);
    when(configurations.getBatchSize(any())).thenReturn(2);
    when(messageGetStrategy.get(tuple1)).thenReturn(message1);
    when(messageGetStrategy.get(tuple2)).thenReturn(message2);
  }

  @Test
  public void writeShouldProperlyAckTuplesInBatch() throws Exception {
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllSuccesses(tupleList);

    when(bulkMessageWriter.write(sensorType, configurations, Arrays.asList(tuple1, tuple2), Arrays.asList(message1, message2))).thenReturn(response);

    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(collector);
    bulkWriterComponent.write(sensorType, tuple1, message1, bulkMessageWriter, configurations, messageGetStrategy);

    verify(bulkMessageWriter, times(0)).write(sensorType, configurations, Collections.singletonList(tuple1), Collections.singletonList(message1));
    verify(collector, times(0)).ack(tuple1);
    verify(collector, times(0)).ack(tuple2);

    bulkWriterComponent.write(sensorType, tuple2, message2, bulkMessageWriter, configurations, messageGetStrategy);

    verify(collector, times(1)).ack(tuple1);
    verify(collector, times(1)).ack(tuple2);

    // A disabled writer should still ack
    Tuple disabledTuple = mock(Tuple.class);
    String disabledSensorType = "disabled";
    when(configurations.isEnabled(disabledSensorType)).thenReturn(false);
    bulkWriterComponent.write(disabledSensorType, disabledTuple, message2, bulkMessageWriter, configurations, messageGetStrategy);
    verify(collector, times(1)).ack(disabledTuple);

    verifyStatic(times(0));
    ErrorUtils.handleError(eq(collector), any(MetronError.class));
  }

  @Test
  public void writeShouldProperlyHandleWriterErrors() throws Exception {
    Throwable e = new Exception("test exception");
    MetronError expectedError1 = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR).withThrowable(e).withRawMessages(Collections.singletonList(message1));
    MetronError expectedError2 = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR).withThrowable(e).withRawMessages(Collections.singletonList(message2));
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllErrors(e, tupleList);

    when(bulkMessageWriter.write(sensorType, configurations, Arrays.asList(tuple1, tuple2), Arrays.asList(message1, message2))).thenReturn(response);

    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(collector);
    bulkWriterComponent.write(sensorType, tuple1, message1, bulkMessageWriter, configurations, messageGetStrategy);
    bulkWriterComponent.write(sensorType, tuple2, message2, bulkMessageWriter, configurations, messageGetStrategy);

    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM),
            new Values(argThat(new MetronErrorJSONMatcher(expectedError1.getJSONObject()))));
    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM),
            new Values(argThat(new MetronErrorJSONMatcher(expectedError2.getJSONObject()))));
    verify(collector, times(1)).ack(tuple1);
    verify(collector, times(1)).ack(tuple2);
    verify(collector, times(1)).reportError(e);
    verifyNoMoreInteractions(collector);
  }

  @Test
  public void writeShouldThrowExceptionWhenHandleErrorIsFalse() throws Exception {
    exception.expect(IllegalStateException.class);

    Throwable e = new Exception("test exception");
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllErrors(e, tupleList);

    when(bulkMessageWriter.write(sensorType, configurations, Arrays.asList(tuple1, tuple2), Arrays.asList(message1, message2))).thenReturn(response);

    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(collector, true, false);
    bulkWriterComponent.write(sensorType, tuple1, message1, bulkMessageWriter, configurations, messageGetStrategy);
    bulkWriterComponent.write(sensorType, tuple2, message2, bulkMessageWriter, configurations, messageGetStrategy);
  }

  @Test
  public void writeShouldProperlyHandleWriterException() throws Exception {
    Throwable e = new Exception("test exception");
    MetronError expectedError1 = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR)
            .withThrowable(e)
            .withRawMessages(Collections.singletonList(message1));
    MetronError expectedError2 = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR)
            .withThrowable(e)
            .withRawMessages(Collections.singletonList(message2));
    BulkWriterResponse response = new BulkWriterResponse();
    response.addAllErrors(e, tupleList);

    when(bulkMessageWriter.write(sensorType, configurations, Arrays.asList(tuple1, tuple2), Arrays.asList(message1, message2))).thenThrow(e);

    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(collector);
    bulkWriterComponent.write(sensorType, tuple1, message1, bulkMessageWriter, configurations, messageGetStrategy);
    bulkWriterComponent.write(sensorType, tuple2, message2, bulkMessageWriter, configurations, messageGetStrategy);

    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM),
            new Values(argThat(new MetronErrorJSONMatcher(expectedError1.getJSONObject()))));
    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM),
            new Values(argThat(new MetronErrorJSONMatcher(expectedError2.getJSONObject()))));
    verify(collector, times(1)).ack(tuple1);
    verify(collector, times(1)).ack(tuple2);
    verify(collector, times(1)).reportError(e);
    verifyNoMoreInteractions(collector);
  }

  @Test
  public void errorAllShouldClearMapsAndHandleErrors() throws Exception {
    Throwable e = new Exception("test exception");
    MetronError error1 = new MetronError()
            .withSensorType(Collections.singleton("sensor1"))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR)
            .withThrowable(e)
            .withRawMessages(Collections.singletonList(message1));
    MetronError error2 = new MetronError()
            .withSensorType(Collections.singleton("sensor2"))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR)
            .withThrowable(e)
            .withRawMessages(Collections.singletonList(message2));

    BulkWriterComponent<JSONObject> bulkWriterComponent = new BulkWriterComponent<>(collector);
    bulkWriterComponent.write("sensor1", tuple1, message1, bulkMessageWriter, configurations, messageGetStrategy);
    bulkWriterComponent.write("sensor2", tuple2, message2, bulkMessageWriter, configurations, messageGetStrategy);
    bulkWriterComponent.errorAll(e, messageGetStrategy);

    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM),
            new Values(argThat(new MetronErrorJSONMatcher(error1.getJSONObject()))));
    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM),
            new Values(argThat(new MetronErrorJSONMatcher(error2.getJSONObject()))));
    verify(collector, times(1)).ack(tuple1);
    verify(collector, times(1)).ack(tuple2);
    verify(collector, times(2)).reportError(e);
    verifyNoMoreInteractions(collector);

    bulkWriterComponent.write("sensor1", tuple1, message1, bulkMessageWriter, configurations, messageGetStrategy);
    verify(bulkMessageWriter, times(0)).write(sensorType, configurations, Collections.singletonList(tuple1), Collections.singletonList(message1));
  }

  @Test
  public void flushShouldAckMissingTuples() throws Exception{
    BulkMessageWriter<JSONObject> bulkMessageWriter = mock(BulkMessageWriter.class);
    Tuple successTuple = mock(Tuple.class);
    Tuple errorTuple = mock(Tuple.class);
    Tuple missingTuple = mock(Tuple.class);
    Collection<Tuple> tupleList = Arrays.asList(successTuple, errorTuple, missingTuple);
    JSONObject successMessage = new JSONObject();
    successMessage.put("name", "success");
    JSONObject errorMessage = new JSONObject();
    errorMessage.put("name", "error");
    JSONObject missingMessage = new JSONObject();
    missingMessage.put("name", "missing");
    List<JSONObject> messageList = Arrays.asList(successMessage, errorMessage, missingMessage);
    OutputCollector collector = mock(OutputCollector.class);
    BulkWriterResponse bulkWriterResponse = new BulkWriterResponse();
    bulkWriterResponse.addSuccess(successTuple);
    Throwable throwable = mock(Throwable.class);
    bulkWriterResponse.addError(throwable, errorTuple);

    when(bulkMessageWriter.write(sensorType, configurations, tupleList, messageList)).thenReturn(bulkWriterResponse);

    BulkWriterComponent bulkWriterComponent = new BulkWriterComponent(collector, true, true);
    bulkWriterComponent.flush(sensorType, bulkMessageWriter, configurations, messageGetStrategy, tupleList, messageList);

    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM), any(Values.class));
    verify(collector, times(1)).reportError(throwable);
    verify(collector, times(1)).ack(successTuple);
    verify(collector, times(1)).ack(errorTuple);
    verify(collector, times(1)).ack(missingTuple);
    verifyNoMoreInteractions(collector);
  }
}
