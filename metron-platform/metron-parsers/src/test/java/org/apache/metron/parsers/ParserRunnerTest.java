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
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.parsers.filters.Filters;
import org.apache.metron.parsers.filters.StellarFilter;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.topology.ParserComponent;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ParserRunner.class, ReflectionUtils.class, Filters.class})
public class ParserRunnerTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

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
  private ParserRunner parserRunner;


  @Before
  public void setup() throws IOException {
    parserConfigurations = new ParserConfigurations();
    SensorParserConfig broConfig = SensorParserConfig.fromBytes(broConfigString.getBytes());
    SensorParserConfig snortConfig = SensorParserConfig.fromBytes(snortConfigString.getBytes());
    parserConfigurations.updateSensorParserConfig("bro", broConfig);
    parserConfigurations.updateSensorParserConfig("snort", snortConfig);
    parserConfigurations.updateGlobalConfig(JSONUtils.INSTANCE.load(globalConfigString, JSONUtils.MAP_SUPPLIER));
    parserRunner = new ParserRunner(new HashSet<>(Arrays.asList("bro", "snort")));
    broParser = mock(MessageParser.class);
    snortParser = mock(MessageParser.class);
    stellarFilter = mock(StellarFilter.class);
    mockStatic(ReflectionUtils.class);
    mockStatic(Filters.class);

    when(ReflectionUtils.createInstance("org.apache.metron.parsers.bro.BasicBroParser")).thenReturn(broParser);
    when(ReflectionUtils.createInstance("org.apache.metron.parsers.snort.BasicSnortParser")).thenReturn(snortParser);
    when(Filters.get("org.apache.metron.parsers.filters.StellarFilter", broConfig.getParserConfig()))
            .thenReturn(stellarFilter);

  }

  @Test
  public void shouldThrowExceptionOnEmptyOnSuccess() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("An onSuccess function must be set before parsing a message.");

    parserRunner.execute("bro", null, null);
  }

  @Test
  public void shouldThrowExceptionOnEmptyOnError() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("An onError function must be set before parsing a message.");

    parserRunner.setOnSuccess(parserResult -> {});
    parserRunner.execute("bro", null, null);
  }

  @Test
  public void shouldThrowExceptionOnEmptyParserSupplier() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("A parser config supplier must be set before initializing the ParserRunner.");

    parserRunner.init(null, null);
  }

  @Test
  public void shouldThrowExceptionOnMissingSensorParserConfig() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Could not initialize parsers.  Cannot find config for sensor test.");

    parserRunner = new ParserRunner(new HashSet<String>() {{
      add("test");
    }});

    parserRunner.setOnSuccess(parserResult -> {});
    parserRunner.setOnError(parserResult -> {});
    parserRunner.init(null, () -> parserConfigurations);
  }

  @Test
  public void shouldInit() throws Exception {
    CuratorFramework client = mock(CuratorFramework.class);
    Map<String, Object> broParserConfig = parserConfigurations.getSensorParserConfig("bro").getParserConfig();
    Map<String, Object> snortParserConfig = parserConfigurations.getSensorParserConfig("snort").getParserConfig();

    parserRunner.init(client, () -> parserConfigurations);

    {
      // Verify Stellar initialization
      Assert.assertNotNull(parserRunner.getStellarContext().getCapability(Context.Capabilities.ZOOKEEPER_CLIENT));
      Assert.assertNotNull(parserRunner.getStellarContext().getCapability(Context.Capabilities.GLOBAL_CONFIG));
      Assert.assertNotNull(parserRunner.getStellarContext().getCapability(Context.Capabilities.STELLAR_CONFIG));
    }

    Map<String, ParserComponent> sensorToParserComponentMap = parserRunner.getSensorToParserComponentMap();
    {
      // Verify Bro parser initialization
      Assert.assertEquals(2, sensorToParserComponentMap.size());
      ParserComponent broComponent = sensorToParserComponentMap.get("bro");
      Assert.assertEquals(broParser, broComponent.getMessageParser());
      Assert.assertEquals(stellarFilter, broComponent.getFilter());
      verify(broParser, times(1)).init();
      verify(broParser, times(1)).configure(broParserConfig);
      verifyNoMoreInteractions(broParser);
      verifyNoMoreInteractions(stellarFilter);
    }
    {
      // Verify Snort parser initialization
      ParserComponent snortComponent = sensorToParserComponentMap.get("snort");
      Assert.assertEquals(snortParser, snortComponent.getMessageParser());
      Assert.assertNull(snortComponent.getFilter());
      verify(snortParser, times(1)).init();
      verify(snortParser, times(1)).configure(snortParserConfig);
      verifyNoMoreInteractions(snortParser);
    }
  }

  @Test
  public void shouldExecute() {
    parserRunner = spy(parserRunner);
    RawMessage rawMessage = new RawMessage("raw_message".getBytes(), new HashMap<>());
    JSONObject message = new JSONObject();

    when(broParser.parseOptional(rawMessage.getMessage())).thenReturn(Optional.of(Collections.singletonList(message)));

    parserRunner.setOnSuccess(parserResult -> {});
    parserRunner.setOnError(parserResult -> {});
    parserRunner.setSensorToParserComponentMap(new HashMap<String, ParserComponent>() {{
      put("bro", new ParserComponent(broParser, stellarFilter));
    }});
    parserRunner.execute("bro", rawMessage, parserConfigurations);

    verify(parserRunner, times(1))
            .processMessage("bro", message, rawMessage, broParser, parserConfigurations);
  }

  @Test
  public void shouldReturnParserResultOnProcessMessage() {
    JSONObject inputMessage = new JSONObject();
    inputMessage.put("guid", "guid");
    inputMessage.put("ip_src_addr", "192.168.1.1");
    inputMessage.put("ip_dst_addr", "192.168.1.2");
    inputMessage.put("field1", "value");
    RawMessage rawMessage = new RawMessage("raw_message".getBytes(), new HashMap<>());

    Consumer<ParserResult> onSuccess = mock(Consumer.class);
    JSONObject expectedOutput  = new JSONObject();
    expectedOutput.put("guid", "guid");
    expectedOutput.put("source.type", "bro");
    expectedOutput.put("ip_src_addr", "192.168.1.1");
    expectedOutput.put("ip_dst_addr", "192.168.1.2");

    ParserResult expectedResult = new ParserResult("bro", expectedOutput, rawMessage.getMessage());

    when(stellarFilter.emit(expectedOutput, parserRunner.getStellarContext())).thenReturn(true);
    when(broParser.validate(expectedOutput)).thenReturn(true);

    parserRunner.setOnSuccess(onSuccess);
    parserRunner.setOnError(metronError -> Assert.fail("OnError should not have been called."));
    parserRunner.setSensorToParserComponentMap(new HashMap<String, ParserComponent>() {{
      put("bro", new ParserComponent(broParser, stellarFilter));
    }});
    parserRunner.processMessage("bro", inputMessage, rawMessage, broParser, parserConfigurations);

    verify(onSuccess, times(1)).accept(expectedResult);
    verifyNoMoreInteractions(onSuccess);
  }

  @Test
  public void shouldReturnMetronErrorOnInvalidMessage() {
    JSONObject inputMessage = new JSONObject();
    inputMessage.put("guid", "guid");
    RawMessage rawMessage = new RawMessage("raw_message".getBytes(), new HashMap<>());

    Consumer<MetronError> onError = mock(Consumer.class);
    JSONObject expectedOutput  = new JSONObject();
    expectedOutput.put("guid", "guid");
    expectedOutput.put("source.type", "bro");
    MetronError expectedMetronError = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_INVALID)
            .withSensorType(Collections.singleton("bro"))
            .addRawMessage(inputMessage);

    when(stellarFilter.emit(expectedOutput, parserRunner.getStellarContext())).thenReturn(true);
    when(broParser.validate(expectedOutput)).thenReturn(false);

    parserRunner.setOnSuccess(parserResult -> Assert.fail("OnSuccess should not have been called."));
    parserRunner.setOnError(onError);
    parserRunner.setSensorToParserComponentMap(new HashMap<String, ParserComponent>() {{
      put("bro", new ParserComponent(broParser, stellarFilter));
    }});
    parserRunner.processMessage("bro", inputMessage, rawMessage, broParser, parserConfigurations);

    verify(onError, times(1)).accept(expectedMetronError);
    verifyNoMoreInteractions(onError);
  }

  @Test
  public void shouldReturnMetronErrorOnFailedFieldValidator() {
    JSONObject inputMessage = new JSONObject();
    inputMessage.put("guid", "guid");
    inputMessage.put("ip_src_addr", "test");
    inputMessage.put("ip_dst_addr", "test");
    RawMessage rawMessage = new RawMessage("raw_message".getBytes(), new HashMap<>());

    Consumer<MetronError> onError = mock(Consumer.class);
    JSONObject expectedOutput  = new JSONObject();
    expectedOutput.put("guid", "guid");
    expectedOutput.put("ip_src_addr", "test");
    expectedOutput.put("ip_dst_addr", "test");
    expectedOutput.put("source.type", "bro");
    MetronError expectedMetronError = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_INVALID)
            .withSensorType(Collections.singleton("bro"))
            .addRawMessage(inputMessage)
            .withErrorFields(new HashSet<>(Arrays.asList("ip_src_addr", "ip_dst_addr")));

    when(stellarFilter.emit(expectedOutput, parserRunner.getStellarContext())).thenReturn(true);
    when(broParser.validate(expectedOutput)).thenReturn(true);

    parserRunner.setOnSuccess(parserResult -> Assert.fail("OnSuccess should not have been called."));
    parserRunner.setOnError(onError);
    parserRunner.setSensorToParserComponentMap(new HashMap<String, ParserComponent>() {{
      put("bro", new ParserComponent(broParser, stellarFilter));
    }});
    parserRunner.processMessage("bro", inputMessage, rawMessage, broParser, parserConfigurations);

    verify(onError, times(1)).accept(expectedMetronError);
    verifyNoMoreInteractions(onError);
  }
}
