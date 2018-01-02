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
package org.apache.metron.rest.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import oi.thekraken.grok.api.Grok;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.ParseMessageRequest;
import org.apache.metron.rest.service.GrokService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.apache.metron.rest.MetronRestConstants.GROK_TEMP_PATH_SPRING_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("ALL")
public class SensorParserConfigServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  Environment environment;
  ObjectMapper objectMapper;
  CuratorFramework curatorFramework;
  GrokService grokService;
  SensorParserConfigService sensorParserConfigService;

  /**
   {
   "parserClassName": "org.apache.metron.parsers.GrokParser",
   "sensorTopic": "squid",
   "parserConfig": {
   "grokPath": "/patterns/squid",
   "patternLabel": "SQUID_DELIMITED",
   "timestampField": "timestamp"
   }
   }
   */
  @Multiline
  public static String squidJson;

  /**
   {
   "parserClassName":"org.apache.metron.parsers.bro.BasicBroParser",
   "sensorTopic":"bro",
   "parserConfig": {}
   }
   */
  @Multiline
  public static String broJson;

  private String user = "user1";

  ConfigurationsCache cache;

  @Before
  public void setUp() throws Exception {
    objectMapper = mock(ObjectMapper.class);
    curatorFramework = mock(CuratorFramework.class);
    Environment environment = mock(Environment.class);
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(user);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(environment.getProperty(GROK_TEMP_PATH_SPRING_PROPERTY)).thenReturn("./target");
    grokService = new GrokServiceImpl(environment, mock(Grok.class), new HdfsServiceImpl(new Configuration()));
    cache = mock(ConfigurationsCache.class);
    sensorParserConfigService = new SensorParserConfigServiceImpl(objectMapper, curatorFramework, grokService, cache);
  }


  @Test
  public void deleteShouldProperlyCatchNoNodeExceptionAndReturnFalse() throws Exception {
    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    when(builder.forPath(ConfigurationType.PARSER.getZookeeperRoot() + "/bro")).thenThrow(KeeperException.NoNodeException.class);

    assertFalse(sensorParserConfigService.delete("bro"));
  }

  @Test
  public void deleteShouldProperlyCatchNonNoNodeExceptionAndThrowRestException() throws Exception {
    exception.expect(RestException.class);

    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    when(builder.forPath(ConfigurationType.PARSER.getZookeeperRoot() + "/bro")).thenThrow(Exception.class);

    assertFalse(sensorParserConfigService.delete("bro"));
  }

  @Test
  public void deleteShouldReturnTrueWhenClientSuccessfullyCallsDelete() throws Exception {
    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    when(builder.forPath(ConfigurationType.PARSER.getZookeeperRoot() + "/bro")).thenReturn(null);

    assertTrue(sensorParserConfigService.delete("bro"));

    verify(curatorFramework).delete();
  }

  @Test
  public void findOneShouldProperlyReturnSensorParserConfig() throws Exception {
    final SensorParserConfig sensorParserConfig = getTestBroSensorParserConfig();

    ParserConfigurations configs = new ParserConfigurations(){
      @Override
      public Map<String, Object> getConfigurations() {
        return ImmutableMap.of(ParserConfigurations.getKey("bro"), sensorParserConfig);
      }
    };
    when(cache.get(eq(ParserConfigurations.class)))
            .thenReturn(configs);

    //We only have bro, so we should expect it to be returned
    assertEquals(getTestBroSensorParserConfig(), sensorParserConfigService.findOne("bro"));
    //and blah should be a miss.
    assertNull(sensorParserConfigService.findOne("blah"));
  }

  @Test
  public void getAllTypesShouldProperlyReturnTypes() throws Exception {
    ParserConfigurations configs = new ParserConfigurations(){
      @Override
      public Map<String, Object> getConfigurations() {
        return ImmutableMap.of(ParserConfigurations.getKey("bro"), new HashMap<>()
                              ,ParserConfigurations.getKey("squid"), new HashMap<>()
                              );
      }
    };
    when(cache.get( eq(ParserConfigurations.class)))
            .thenReturn(configs);

    assertEquals(new ArrayList() {{
      add("bro");
      add("squid");
    }}, sensorParserConfigService.getAllTypes());
  }

  @Test
  public void getAllShouldProperlyReturnSensorParserConfigs() throws Exception {
    final SensorParserConfig broSensorParserConfig = getTestBroSensorParserConfig();
    final SensorParserConfig squidSensorParserConfig = getTestSquidSensorParserConfig();
    ParserConfigurations configs = new ParserConfigurations(){
      @Override
      public Map<String, Object> getConfigurations() {
        return ImmutableMap.of(ParserConfigurations.getKey("bro"), broSensorParserConfig
                              ,ParserConfigurations.getKey("squid"), squidSensorParserConfig
                              );
      }
    };
    when(cache.get( eq(ParserConfigurations.class)))
            .thenReturn(configs);

    assertEquals(new HashMap() {{
      put("bro", getTestBroSensorParserConfig());
      put("squid", getTestSquidSensorParserConfig());
    }}, sensorParserConfigService.getAll());
  }

  @Test
  public void saveShouldWrapExceptionInRestException() throws Exception {
    exception.expect(RestException.class);

    SetDataBuilder setDataBuilder = mock(SetDataBuilder.class);
    when(setDataBuilder.forPath(ConfigurationType.PARSER.getZookeeperRoot() + "/bro", broJson.getBytes())).thenThrow(Exception.class);

    when(curatorFramework.setData()).thenReturn(setDataBuilder);

    final SensorParserConfig sensorParserConfig = new SensorParserConfig();
    sensorParserConfig.setSensorTopic("bro");
    sensorParserConfigService.save("bro", sensorParserConfig);
  }

  @Test
  public void saveShouldReturnSameConfigThatIsPassedOnSuccessfulSave() throws Exception {
    final SensorParserConfig sensorParserConfig = getTestBroSensorParserConfig();

    when(objectMapper.writeValueAsString(sensorParserConfig)).thenReturn(broJson);

    SetDataBuilder setDataBuilder = mock(SetDataBuilder.class);
    when(setDataBuilder.forPath(ConfigurationType.PARSER.getZookeeperRoot() + "/bro", broJson.getBytes())).thenReturn(new Stat());
    when(curatorFramework.setData()).thenReturn(setDataBuilder);

    assertEquals(getTestBroSensorParserConfig(), sensorParserConfigService.save("bro", sensorParserConfig));
    verify(setDataBuilder).forPath(eq(ConfigurationType.PARSER.getZookeeperRoot() + "/bro"), eq(broJson.getBytes()));
  }

  @Test
  public void reloadAvailableParsersShouldReturnParserClasses() throws Exception {
    Map<String, String> availableParsers = sensorParserConfigService.reloadAvailableParsers();
    assertTrue(availableParsers.size() > 0);
    assertEquals("org.apache.metron.parsers.GrokParser", availableParsers.get("Grok"));
    assertEquals("org.apache.metron.parsers.bro.BasicBroParser", availableParsers.get("Bro"));
  }

  @Test
  public void parseMessageShouldProperlyReturnParsedResults() throws Exception {
    final SensorParserConfig sensorParserConfig = getTestSquidSensorParserConfig();
    String grokStatement = "SQUID_DELIMITED %{NUMBER:timestamp}[^0-9]*%{INT:elapsed} %{IP:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url}[^0-9]*(%{IP:ip_dst_addr})?";
    String sampleData = "1461576382.642    161 127.0.0.1 TCP_MISS/200 103701 GET http://www.cnn.com/ - DIRECT/199.27.79.73 text/html";
    ParseMessageRequest parseMessageRequest = new ParseMessageRequest();
    parseMessageRequest.setSensorParserConfig(sensorParserConfig);
    parseMessageRequest.setGrokStatement(grokStatement);
    parseMessageRequest.setSampleData(sampleData);

    File grokRoot = new File("./target", user);
    grokRoot.mkdir();
    File patternFile = new File(grokRoot, "squid");
    FileWriter writer = new FileWriter(patternFile);
    writer.write(grokStatement);
    writer.close();

    assertEquals(new HashMap() {{
      put("elapsed", 161);
      put("code", 200);
      put("ip_dst_addr", "199.27.79.73");
      put("ip_src_addr", "127.0.0.1");
      put("action", "TCP_MISS");
      put("bytes", 103701);
      put("method", "GET");
      put("url", "http://www.cnn.com/");
      put("timestamp", 1461576382642L);
      put("original_string", "1461576382.642    161 127.0.0.1 TCP_MISS/200 103701 GET http://www.cnn.com/ - DIRECT/199.27.79.73 text/html");
    }}, sensorParserConfigService.parseMessage(parseMessageRequest));

  }

  @Test
  public void missingSensorParserConfigShouldThrowRestException() throws Exception {
    exception.expect(RestException.class);

    ParseMessageRequest parseMessageRequest = new ParseMessageRequest();
    sensorParserConfigService.parseMessage(parseMessageRequest);
  }

  @Test
  public void missingParserClassShouldThrowRestException() throws Exception {
    exception.expect(RestException.class);

    final SensorParserConfig sensorParserConfig = new SensorParserConfig();
    sensorParserConfig.setSensorTopic("squid");
    ParseMessageRequest parseMessageRequest = new ParseMessageRequest();
    parseMessageRequest.setSensorParserConfig(sensorParserConfig);
    sensorParserConfigService.parseMessage(parseMessageRequest);
  }

  @Test
  public void invalidParserClassShouldThrowRestException() throws Exception {
    exception.expect(RestException.class);

    final SensorParserConfig sensorParserConfig = new SensorParserConfig();
    sensorParserConfig.setSensorTopic("squid");
    sensorParserConfig.setParserClassName("bad.class.package.BadClassName");
    ParseMessageRequest parseMessageRequest = new ParseMessageRequest();
    parseMessageRequest.setSensorParserConfig(sensorParserConfig);
    sensorParserConfigService.parseMessage(parseMessageRequest);
  }

  private SensorParserConfig getTestBroSensorParserConfig() {
    SensorParserConfig sensorParserConfig = new SensorParserConfig();
    sensorParserConfig.setSensorTopic("bro");
    sensorParserConfig.setParserClassName("org.apache.metron.parsers.bro.BasicBroParser");
    return sensorParserConfig;
  }

  private SensorParserConfig getTestSquidSensorParserConfig() {
    SensorParserConfig sensorParserConfig = new SensorParserConfig();
    sensorParserConfig.setSensorTopic("squid");
    sensorParserConfig.setParserClassName("org.apache.metron.parsers.GrokParser");
    sensorParserConfig.setParserConfig(new HashMap() {{
      put("grokPath", "/patterns/squid");
      put("patternLabel", "SQUID_DELIMITED");
      put("timestampField", "timestamp");
    }});
    return sensorParserConfig;
  }

 }
