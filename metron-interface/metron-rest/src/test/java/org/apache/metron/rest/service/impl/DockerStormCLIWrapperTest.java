/*
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

import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unchecked")
@RunWith(PowerMockRunner.class)
@PrepareForTest({DockerStormCLIWrapper.class, ProcessBuilder.class})
public class DockerStormCLIWrapperTest {
  private ProcessBuilder processBuilder;
  private Environment environment;
  private DockerStormCLIWrapper dockerStormCLIWrapper;

  @Before
  public void setUp() throws Exception {
    processBuilder = mock(ProcessBuilder.class);
    environment = mock(Environment.class);

    dockerStormCLIWrapper = new DockerStormCLIWrapper(environment);
  }

  @Test
  public void getProcessBuilderShouldProperlyGenerateProcessorBuilder() throws Exception {
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(processBuilder);

    when(processBuilder.environment()).thenReturn(new HashMap<>());
    when(processBuilder.command()).thenReturn(new ArrayList<>());

    Process process = mock(Process.class);
    InputStream inputStream = new ByteArrayInputStream("export DOCKER_HOST=\"tcp://192.168.99.100:2376\"".getBytes(
        StandardCharsets.UTF_8));

    when(processBuilder.start()).thenReturn(process);
    when(process.getInputStream()).thenReturn(inputStream);
    when(environment.getProperty("docker.compose.path")).thenReturn("/test");
    when(environment.getProperty("metron.version")).thenReturn("1");


    ProcessBuilder actualBuilder = dockerStormCLIWrapper.getProcessBuilder("oo", "ooo");

    assertEquals(new HashMap<String, String>() {{
      put("METRON_VERSION", "1");
      put("DOCKER_HOST", "tcp://192.168.99.100:2376");
    }}, actualBuilder.environment());
    assertEquals(new ArrayList<>(), actualBuilder.command());

    verify(process).waitFor();
  }
}