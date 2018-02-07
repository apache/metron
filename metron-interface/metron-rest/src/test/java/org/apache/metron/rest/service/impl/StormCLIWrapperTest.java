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

import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unchecked")
@RunWith(PowerMockRunner.class)
@PrepareForTest({DockerStormCLIWrapper.class, ProcessBuilder.class})
public class StormCLIWrapperTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private ProcessBuilder processBuilder;
  private Environment environment;
  private Process process;
  private StormCLIWrapper stormCLIWrapper;

  @Before
  public void setUp() throws Exception {
    processBuilder = mock(ProcessBuilder.class);
    environment = mock(Environment.class);
    process = mock(Process.class);
    stormCLIWrapper = new StormCLIWrapper();
    stormCLIWrapper.setEnvironment(environment);
  }

  @Test
  public void startParserTopologyShouldRunCommandProperly() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    when(processBuilder.start()).thenReturn(process);
    when(environment.getProperty(MetronRestConstants.PARSER_SCRIPT_PATH_SPRING_PROPERTY)).thenReturn("/start_parser");
    when(environment.getProperty(MetronRestConstants.KAFKA_BROKER_URL_SPRING_PROPERTY)).thenReturn("kafka_broker_url");
    when(environment.getProperty(MetronRestConstants.ZK_URL_SPRING_PROPERTY)).thenReturn("zookeeper_url");
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);
    when(environment.getProperty(MetronRestConstants.KAFKA_SECURITY_PROTOCOL_SPRING_PROPERTY)).thenReturn("kafka_security_protocol");
    when(process.exitValue()).thenReturn(0);

    assertEquals(0, stormCLIWrapper.startParserTopology("bro"));
    verify(process).waitFor();
    verifyNew(ProcessBuilder.class).withArguments("/start_parser",
            "-s", "bro",
            "-z", "zookeeper_url",
            "-k", "kafka_broker_url",
            "-ksp", "kafka_security_protocol");
  }

  /**
   * If Kerberos is enabled and the PARSER_TOPOLOGY_OPTIONS field is defined, then extra topology options
   * will be passed to the Parser topology.
   */
  @Test
  public void startParserTopologyWithExtraTopologyOptions() throws Exception {

    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    when(processBuilder.start()).thenReturn(process);
    when(environment.getProperty(MetronRestConstants.PARSER_SCRIPT_PATH_SPRING_PROPERTY)).thenReturn("/start_parser");
    when(environment.getProperty(MetronRestConstants.KAFKA_BROKER_URL_SPRING_PROPERTY)).thenReturn("kafka_broker_url");
    when(environment.getProperty(MetronRestConstants.ZK_URL_SPRING_PROPERTY)).thenReturn("zookeeper_url");
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(true);
    when(environment.getProperty(MetronRestConstants.KAFKA_SECURITY_PROTOCOL_SPRING_PROPERTY)).thenReturn("kafka_security_protocol");
    when(environment.getProperty(MetronRestConstants.PARSER_TOPOLOGY_OPTIONS_SPRING_PROPERTY)).thenReturn("parser_topology_options");
    when(process.exitValue()).thenReturn(0);

    assertEquals(0, stormCLIWrapper.startParserTopology("bro"));
    verify(process, times(2)).waitFor();
    verifyNew(ProcessBuilder.class).withArguments("/start_parser",
            "-s", "bro",
            "-z", "zookeeper_url",
            "-k", "kafka_broker_url",
            "-ksp", "kafka_security_protocol",
            "-e", "parser_topology_options");
  }

  @Test
  public void stopParserTopologyShouldRunCommandProperly() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    when(processBuilder.start()).thenReturn(process);
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);
    when(process.exitValue()).thenReturn(0);

    assertEquals(0, stormCLIWrapper.stopParserTopology("bro", false));
    verify(process).waitFor();
    verifyNew(ProcessBuilder.class).withArguments("storm", "kill", "bro");
  }

  @Test
  public void stopParserTopologyNowShouldRunCommandProperly() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    when(processBuilder.start()).thenReturn(process);
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);
    when(process.exitValue()).thenReturn(0);

    assertEquals(0, stormCLIWrapper.stopParserTopology("bro", true));
    verify(process).waitFor();
    verifyNew(ProcessBuilder.class).withArguments("storm", "kill", "bro", "-w", "0");
  }

  @Test
  public void startEnrichmentTopologyShouldRunCommandProperly() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    when(processBuilder.start()).thenReturn(process);
    when(environment.getProperty(MetronRestConstants.ENRICHMENT_SCRIPT_PATH_SPRING_PROPERTY)).thenReturn("/start_enrichment");
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);
    when(process.exitValue()).thenReturn(0);

    assertEquals(0, stormCLIWrapper.startEnrichmentTopology());
    verify(process).waitFor();
    verifyNew(ProcessBuilder.class).withArguments("/start_enrichment");

  }

  @Test
  public void stopEnrichmentTopologyShouldRunCommandProperly() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    when(processBuilder.start()).thenReturn(process);
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);
    when(process.exitValue()).thenReturn(0);

    assertEquals(0, stormCLIWrapper.stopEnrichmentTopology(false));
    verify(process).waitFor();
    verifyNew(ProcessBuilder.class).withArguments("storm", "kill", MetronRestConstants.ENRICHMENT_TOPOLOGY_NAME);
  }

  @Test
  public void startIndexingTopologyShouldRunCommandProperly() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    when(processBuilder.start()).thenReturn(process);
    when(environment.getProperty(MetronRestConstants.RANDOM_ACCESS_INDEXING_SCRIPT_PATH_SPRING_PROPERTY)).thenReturn("/start_indexing");
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);
    when(process.exitValue()).thenReturn(0);

    assertEquals(0, stormCLIWrapper.startIndexingTopology(MetronRestConstants.RANDOM_ACCESS_INDEXING_SCRIPT_PATH_SPRING_PROPERTY));
    verify(process).waitFor();
    verifyNew(ProcessBuilder.class).withArguments("/start_indexing");

  }

  @Test
  public void stopIndexingTopologyShouldRunCommandProperly() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    when(processBuilder.start()).thenReturn(process);
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);
    when(process.exitValue()).thenReturn(0);

    assertEquals(0, stormCLIWrapper.stopIndexingTopology("random_access_indexing", false));
    verify(process).waitFor();
    verifyNew(ProcessBuilder.class).withArguments("storm", "kill", MetronRestConstants.RANDOM_ACCESS_INDEXING_TOPOLOGY_NAME);
  }

  @Test
  public void getStormClientStatusShouldReturnCorrectStatus() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    Process process = mock(Process.class);
    InputStream inputStream = new ByteArrayInputStream("\nStorm 1.1".getBytes(UTF_8));

    when(processBuilder.start()).thenReturn(process);

    when(process.getInputStream()).thenReturn(inputStream);
    when(environment.getProperty(MetronRestConstants.PARSER_SCRIPT_PATH_SPRING_PROPERTY)).thenReturn("/start_parser");
    when(environment.getProperty(MetronRestConstants.ENRICHMENT_SCRIPT_PATH_SPRING_PROPERTY)).thenReturn("/start_enrichment");
    when(environment.getProperty(MetronRestConstants.RANDOM_ACCESS_INDEXING_SCRIPT_PATH_SPRING_PROPERTY)).thenReturn("/start_elasticsearch");
    when(environment.getProperty(MetronRestConstants.BATCH_INDEXING_SCRIPT_PATH_SPRING_PROPERTY)).thenReturn("/start_hdfs");

    Map<String, String> actual = stormCLIWrapper.getStormClientStatus();
    assertEquals(new HashMap<String, String>() {{
      put("randomAccessIndexingScriptPath", "/start_elasticsearch");
      put("enrichmentScriptPath", "/start_enrichment");
      put("stormClientVersionInstalled", "1.1");
      put("parserScriptPath", "/start_parser");
      put("batchIndexingScriptPath", "/start_hdfs");

    }}, actual);
    verifyNew(ProcessBuilder.class).withArguments("storm", "version");
  }

  @Test
  public void stormClientVersionInstalledShouldReturnDefault() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    Process process = mock(Process.class);
    InputStream inputStream = new ByteArrayInputStream("".getBytes(UTF_8));

    when(processBuilder.start()).thenReturn(process);
    when(process.getInputStream()).thenReturn(inputStream);
    assertEquals("Storm client is not installed", stormCLIWrapper.stormClientVersionInstalled());
  }

  @Test
  public void runCommandShouldReturnRestExceptionOnError() throws Exception {
    exception.expect(RestException.class);

    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);
    when(processBuilder.start()).thenThrow(new IOException());

    stormCLIWrapper.runCommand(new String[]{"storm", "kill"});
  }

  @Test
  public void stormClientVersionInstalledShouldReturnRestExceptionOnError() throws Exception {
    exception.expect(RestException.class);

    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);
    when(processBuilder.start()).thenThrow(new IOException());

    stormCLIWrapper.stormClientVersionInstalled();
  }

  @Test
  public void kinitShouldRunCommandProperly() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    when(processBuilder.start()).thenReturn(process);
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(true);
    when(environment.getProperty(MetronRestConstants.KERBEROS_KEYTAB_SPRING_PROPERTY)).thenReturn("metron keytabLocation");
    when(environment.getProperty(MetronRestConstants.KERBEROS_PRINCIPLE_SPRING_PROPERTY)).thenReturn("metron principal");
    when(process.exitValue()).thenReturn(0);

    stormCLIWrapper.kinit();
    verify(process, times(1)).waitFor();
    verifyNew(ProcessBuilder.class).withArguments("kinit", "-kt", "metron keytabLocation", "metron principal");
  }
}
