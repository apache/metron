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
package org.apache.metron.parsers;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.Constants.Fields;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.parsers.ParserRunnerImpl.ProcessResult;
import org.apache.metron.parsers.filters.StellarFilter;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ParserRunnerImplTest {
  /**
   {
   "fieldValidations" : [
     {
       "input" : [ "ip_src_addr", "ip_dst_addr"],
       "validation" : "IP"
     }
   ]
   }
   */
  @Multiline
  private String globalConfigString;

  /**
   {
     "parserClassName":"org.apache.metron.parsers.bro.BasicBroParser",
     "filterClassName":"org.apache.metron.parsers.filters.StellarFilter",
     "sensorTopic":"bro",
     "parserConfig": {
       "field": "value"
     },
     "fieldTransformations" : [
       {
         "input" : "field1",
         "transformation" : "REMOVE"
       }
     ]
   }
   */
  @Multiline
  private String broConfigString;

  /**
   {
     "parserClassName":"org.apache.metron.parsers.snort.BasicSnortParser",
     "sensorTopic":"snort",
     "parserConfig": {}
   }
   */
  @Multiline
  private String snortConfigString;

  private ParserConfigurations parserConfigurations;
  private MessageParser<JSONObject> broParser;
  private MessageParser<JSONObject> snortParser;
  private MessageFilter<JSONObject> stellarFilter;
  private ParserRunnerImpl parserRunner;


  @BeforeEach
  @SuppressWarnings("unchecked")
  public void setup() throws IOException {
    parserConfigurations = new ParserConfigurations();
    SensorParserConfig broConfig = SensorParserConfig.fromBytes(broConfigString.getBytes(
        StandardCharsets.UTF_8));
    SensorParserConfig snortConfig = SensorParserConfig.fromBytes(snortConfigString.getBytes(
        StandardCharsets.UTF_8));
    parserConfigurations.updateSensorParserConfig("bro", broConfig);
    parserConfigurations.updateSensorParserConfig("snort", snortConfig);
    parserConfigurations.updateGlobalConfig(JSONUtils.INSTANCE.load(globalConfigString, JSONUtils.MAP_SUPPLIER));
    parserRunner = new ParserRunnerImpl(new HashSet<>(Arrays.asList("bro", "snort")));
    broParser = mock(MessageParser.class);
    snortParser = mock(MessageParser.class);
    stellarFilter = mock(StellarFilter.class);
//    mockStatic(ReflectionUtils.class);
//    mockStatic(Filters.class);
    when(broParser.getReadCharset()).thenReturn(StandardCharsets.UTF_8);

//    when(ReflectionUtils.createInstance("org.apache.metron.parsers.bro.BasicBroParser")).thenReturn(broParser);
//    when(ReflectionUtils.createInstance("org.apache.metron.parsers.snort.BasicSnortParser")).thenReturn(snortParser);
//    when(Filters.get("org.apache.metron.parsers.filters.StellarFilter", broConfig.getParserConfig()))
//            .thenReturn(stellarFilter);

  }

  @Test
  public void shouldThrowExceptionOnEmptyParserSupplier() {
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> parserRunner.init(null, null));
    assertEquals("A parser config supplier must be set before initializing the ParserRunner.", e.getMessage());
  }

  @Test
  public void shouldThrowExceptionOnEmptyStellarContext() {
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> parserRunner.init(() -> parserConfigurations, null));
    assertEquals("A stellar context must be set before initializing the ParserRunner.", e.getMessage());

  }

  @Test
  public void initShouldThrowExceptionOnMissingSensorParserConfig() {
    parserRunner = new ParserRunnerImpl(new HashSet<String>() {{
      add("test");
    }});

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> parserRunner.init(() -> parserConfigurations, mock(Context.class)));
    assertEquals("Could not initialize parsers.  Cannot find configuration for sensor test.", e.getMessage());
  }

  @Test
  public void executeShouldThrowExceptionOnMissingSensorParserConfig() {
    parserRunner = new ParserRunnerImpl(new HashSet<String>() {{
      add("test");
    }});

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> parserRunner.execute("test", mock(RawMessage.class), parserConfigurations));
    assertEquals("Could not execute parser.  Cannot find configuration for sensor test.", e.getMessage());
  }

  @Test
  public void shouldInit() {
    Context stellarContext = mock(Context.class);
    SensorParserConfig broParserConfig = parserConfigurations.getSensorParserConfig("bro");
    SensorParserConfig snortParserConfig = parserConfigurations.getSensorParserConfig("snort");

    // Effectively using a spy to avoid calling static methods. However, we're using mock with settings to use the constuctor.
    // This forces the use of the doReturn Mockito declaration.
    // This is all because the configurations use parsers from metron-parsers, which are not available in metron-parsers-common
    parserRunner = mock(ParserRunnerImpl.class,
            withSettings().useConstructor(new HashSet<>(Arrays.asList("bro", "snort"))).defaultAnswer(CALLS_REAL_METHODS));
    doReturn(broParser).when(parserRunner).createParserInstance(broParserConfig);
    doReturn(snortParser).when(parserRunner).createParserInstance(snortParserConfig);
    doReturn(stellarFilter).when(parserRunner).getMessageFilter(eq(broParserConfig), any());
    parserRunner.init(() -> parserConfigurations, stellarContext);

    {
      // Verify Stellar context
      assertEquals(stellarContext, parserRunner.getStellarContext());
    }

    Map<String, ParserComponent> sensorToParserComponentMap = parserRunner.getSensorToParserComponentMap();
    {
      // Verify Bro parser initialization
      assertEquals(2, sensorToParserComponentMap.size());
      ParserComponent broComponent = sensorToParserComponentMap.get("bro");
      assertEquals(broParser, broComponent.getMessageParser());
      assertEquals(stellarFilter, broComponent.getFilter());
      verify(broParser, times(1)).init();
      verify(broParser, times(1)).configure(broParserConfig.getParserConfig());
      verifyNoMoreInteractions(broParser);
      verifyNoMoreInteractions(stellarFilter);
    }
    {
      // Verify Snort parser initialization
      ParserComponent snortComponent = sensorToParserComponentMap.get("snort");
      assertEquals(snortParser, snortComponent.getMessageParser());
      assertNull(snortComponent.getFilter());
      verify(snortParser, times(1)).init();
      verify(snortParser, times(1)).configure(snortParserConfig.getParserConfig());
      verifyNoMoreInteractions(snortParser);
    }
  }

  /**
   * This is only testing the execute method. It mocks out processMessage().
   */
  @Test
  public void shouldExecute() {
    parserRunner = spy(parserRunner);
    RawMessage rawMessage = new RawMessage("raw_message".getBytes(StandardCharsets.UTF_8), new HashMap<>());
    JSONObject parsedMessage1 = new JSONObject();
    parsedMessage1.put("field", "parsedMessage1");
    JSONObject parsedMessage2 = new JSONObject();
    parsedMessage2.put("field", "parsedMessage2");
    Object rawMessage1 = new RawMessage("raw_message1".getBytes(StandardCharsets.UTF_8), new HashMap<>());
    Object rawMessage2 = new RawMessage("raw_message2".getBytes(StandardCharsets.UTF_8), new HashMap<>());
    Throwable throwable1 = mock(Throwable.class);
    Throwable throwable2 = mock(Throwable.class);
    MessageParserResult<JSONObject> messageParserResult = new DefaultMessageParserResult<>(Arrays.asList(parsedMessage1, parsedMessage2),
            new HashMap<Object, Throwable>(){{
              put(rawMessage1, throwable1);
              put(rawMessage2, throwable2);
            }});
    JSONObject processedMessage = new JSONObject();
    processedMessage.put("field", "processedMessage1");
    MetronError processedError = new MetronError().withMessage("processedError");
    ProcessResult processedMessageResult = mock(ProcessResult.class);
    ProcessResult processedErrorResult = mock(ProcessResult.class);

    when(broParser.parseOptionalResult(rawMessage.getMessage())).thenReturn(Optional.of(messageParserResult));
    when(processedMessageResult.getMessage()).thenReturn(processedMessage);
    when(processedErrorResult.isError()).thenReturn(true);
    when(processedErrorResult.getError()).thenReturn(processedError);
    doReturn(Optional.of(processedMessageResult)).when(parserRunner)
            .processMessage("bro", parsedMessage1, rawMessage, broParser, parserConfigurations);
    doReturn(Optional.of(processedErrorResult)).when(parserRunner)
            .processMessage("bro", parsedMessage2, rawMessage, broParser, parserConfigurations);

    MetronError expectedParseError1 = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_ERROR)
            .withThrowable(throwable1)
            .withSensorType(Collections.singleton("bro"))
            .addRawMessage(rawMessage1);
    MetronError expectedParseError2 = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_ERROR)
            .withThrowable(throwable2)
            .withSensorType(Collections.singleton("bro"))
            .addRawMessage(rawMessage2);

    parserRunner.setSensorToParserComponentMap(new HashMap<String, ParserComponent>() {{
      put("bro", new ParserComponent(broParser, stellarFilter));
    }});
    ParserRunnerResults<JSONObject> parserRunnerResults = parserRunner.execute("bro", rawMessage, parserConfigurations);

    assertEquals(1, parserRunnerResults.getMessages().size());
    assertTrue(parserRunnerResults.getMessages().contains(processedMessage));
    assertEquals(3, parserRunnerResults.getErrors().size());
    assertTrue(parserRunnerResults.getErrors().contains(processedError));
    assertTrue(parserRunnerResults.getErrors().contains(expectedParseError1));
    assertTrue(parserRunnerResults.getErrors().contains(expectedParseError2));
  }

  @Test
  public void shouldExecuteWithMasterThrowable() {
    parserRunner = spy(parserRunner);
    RawMessage rawMessage = new RawMessage("raw_message".getBytes(StandardCharsets.UTF_8), new HashMap<>());
    Throwable masterThrowable = mock(Throwable.class);
    MessageParserResult<JSONObject> messageParserResult = new DefaultMessageParserResult<>(masterThrowable);


    when(broParser.parseOptionalResult(rawMessage.getMessage())).thenReturn(Optional.of(messageParserResult));

    parserRunner.setSensorToParserComponentMap(new HashMap<String, ParserComponent>() {{
      put("bro", new ParserComponent(broParser, stellarFilter));
    }});
    ParserRunnerResults<JSONObject> parserRunnerResults = parserRunner.execute("bro", rawMessage, parserConfigurations);

    verify(parserRunner, times(0))
            .processMessage(any(), any(), any(), any(), any());

    MetronError expectedError = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_ERROR)
            .withThrowable(masterThrowable)
            .withSensorType(Collections.singleton("bro"))
            .addRawMessage(rawMessage.getMessage());
    assertEquals(1, parserRunnerResults.getErrors().size());
    assertTrue(parserRunnerResults.getErrors().contains(expectedError));
  }

  /**
   * This is only testing the processMessage method
   */
  @Test
  public void shouldPopulateMessagesOnProcessMessage() {
    JSONObject inputMessage = new JSONObject();
    inputMessage.put("guid", "guid");
    inputMessage.put("ip_src_addr", "192.168.1.1");
    inputMessage.put("ip_dst_addr", "192.168.1.2");
    RawMessage rawMessage = new RawMessage("raw_message_for_testing".getBytes(StandardCharsets.UTF_8), new HashMap<>());

    JSONObject expectedOutput  = new JSONObject();
    expectedOutput.put("guid", "guid");
    expectedOutput.put("source.type", "bro");
    expectedOutput.put("ip_src_addr", "192.168.1.1");
    expectedOutput.put("ip_dst_addr", "192.168.1.2");
    expectedOutput.put(Fields.ORIGINAL.getName(), "raw_message_for_testing");

    when(stellarFilter.emit(expectedOutput, parserRunner.getStellarContext())).thenReturn(true);
    when(broParser.validate(expectedOutput)).thenReturn(true);

    parserRunner.setSensorToParserComponentMap(new HashMap<String, ParserComponent>() {{
      put("bro", new ParserComponent(broParser, stellarFilter));
    }});

    Optional<ParserRunnerImpl.ProcessResult> processResult = parserRunner.processMessage("bro", inputMessage, rawMessage, broParser, parserConfigurations);
    assertTrue(processResult.isPresent());
    assertFalse(processResult.get().isError());
    assertEquals(expectedOutput, processResult.get().getMessage());
  }

  /**
   * This is only testing the processMessage method
   */
  @Test
  public void shouldNotOverwriteOriginalStringAddedByParser() {
    JSONObject inputMessage = new JSONObject();
    inputMessage.put("guid", "guid");
    inputMessage.put("ip_src_addr", "192.168.1.1");
    inputMessage.put("ip_dst_addr", "192.168.1.2");
    inputMessage.put(Fields.ORIGINAL.getName(), "original_string_added_by_parser");
    RawMessage rawMessage = new RawMessage("raw_message_for_testing".getBytes(StandardCharsets.UTF_8), new HashMap<>());

    JSONObject expectedOutput  = new JSONObject();
    expectedOutput.put("guid", "guid");
    expectedOutput.put("source.type", "bro");
    expectedOutput.put("ip_src_addr", "192.168.1.1");
    expectedOutput.put("ip_dst_addr", "192.168.1.2");
    expectedOutput.put(Fields.ORIGINAL.getName(), "original_string_added_by_parser");

    when(stellarFilter.emit(expectedOutput, parserRunner.getStellarContext())).thenReturn(true);
    when(broParser.validate(expectedOutput)).thenReturn(true);

    parserRunner.setSensorToParserComponentMap(new HashMap<String, ParserComponent>() {{
      put("bro", new ParserComponent(broParser, stellarFilter));
    }});

    Optional<ParserRunnerImpl.ProcessResult> processResult = parserRunner.processMessage("bro", inputMessage, rawMessage, broParser, parserConfigurations);
    assertTrue(processResult.isPresent());
    assertFalse(processResult.get().isError());
    assertEquals(expectedOutput, processResult.get().getMessage());
  }

  @Test
  public void shouldReturnMetronErrorOnInvalidMessage() {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("metron.metadata.topic", "bro");
    metadata.put("metron.metadata.partition", 0);
    metadata.put("metron.metadata.offset", 123);

    JSONObject inputMessage = new JSONObject();
    inputMessage.put("guid", "guid");
    RawMessage rawMessage = new RawMessage("raw_message".getBytes(StandardCharsets.UTF_8), metadata);

    JSONObject expectedOutput  = new JSONObject();
    expectedOutput.put("guid", "guid");
    expectedOutput.put("source.type", "bro");
    expectedOutput.put(Fields.ORIGINAL.getName(), "raw_message");
    MetronError expectedMetronError = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_INVALID)
            .withSensorType(Collections.singleton("bro"))
            .withMetadata(metadata)
            .addRawMessage(inputMessage);

    when(stellarFilter.emit(expectedOutput, parserRunner.getStellarContext())).thenReturn(true);
    // This is the important switch. Not to be confused with field validators.
    when(broParser.validate(expectedOutput)).thenReturn(false);

    parserRunner.setSensorToParserComponentMap(new HashMap<String, ParserComponent>() {{
      put("bro", new ParserComponent(broParser, stellarFilter));
    }});

    Optional<ParserRunnerImpl.ProcessResult> processResult = parserRunner.processMessage("bro", inputMessage, rawMessage, broParser, parserConfigurations);

    assertTrue(processResult.isPresent());
    assertTrue(processResult.get().isError());
    assertEquals(expectedMetronError, processResult.get().getError());
  }

  @Test
  public void shouldReturnMetronErrorOnFailedFieldValidator() {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("metron.metadata.topic", "bro");
    metadata.put("metron.metadata.partition", 0);
    metadata.put("metron.metadata.offset", 123);

    JSONObject inputMessage = new JSONObject();
    inputMessage.put("guid", "guid");
    inputMessage.put("ip_src_addr", "test");
    inputMessage.put("ip_dst_addr", "test");
    RawMessage rawMessage = new RawMessage("raw_message".getBytes(StandardCharsets.UTF_8), metadata);

    JSONObject expectedOutput  = new JSONObject();
    expectedOutput.put("guid", "guid");
    expectedOutput.put("ip_src_addr", "test");
    expectedOutput.put("ip_dst_addr", "test");
    expectedOutput.put("source.type", "bro");
    expectedOutput.put(Fields.ORIGINAL.getName(), "raw_message");
    MetronError expectedMetronError = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_INVALID)
            .withSensorType(Collections.singleton("bro"))
            .addRawMessage(inputMessage)
            .withMetadata(metadata)
            .withErrorFields(new HashSet<>(Arrays.asList("ip_src_addr", "ip_dst_addr")));

    when(stellarFilter.emit(expectedOutput, parserRunner.getStellarContext())).thenReturn(true);
    when(broParser.validate(expectedOutput)).thenReturn(true);

    parserRunner.setSensorToParserComponentMap(new HashMap<String, ParserComponent>() {{
      put("bro", new ParserComponent(broParser, stellarFilter));
    }});

    Optional<ParserRunnerImpl.ProcessResult> processResult = parserRunner.processMessage("bro", inputMessage, rawMessage, broParser, parserConfigurations);

    assertTrue(processResult.isPresent());
    assertTrue(processResult.get().isError());
    assertEquals(expectedMetronError, processResult.get().getError());
  }
}
