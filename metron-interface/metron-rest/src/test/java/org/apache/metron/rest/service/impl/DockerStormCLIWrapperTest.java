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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.mockito.Mockito.*;

public class DockerStormCLIWrapperTest {
  private ProcessBuilder processBuilder;
  private Environment environment;
  private DockerStormCLIWrapper dockerStormCLIWrapper;

  @BeforeEach
  public void setUp() {
    processBuilder = mock(ProcessBuilder.class);
    environment = mock(Environment.class);

    dockerStormCLIWrapper = mock(DockerStormCLIWrapper.class,
            withSettings().useConstructor(environment).defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  public void getProcessBuilderShouldProperlyGenerateProcessorBuilder() throws Exception {
    doReturn(processBuilder).when(dockerStormCLIWrapper).getDockerEnvironmentProcessBuilder();

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
    assertThat(actualBuilder.environment(), hasEntry("METRON_VERSION", "1"));
    assertThat(actualBuilder.environment(), hasEntry("DOCKER_HOST", "tcp://192.168.99.100:2376"));

      // Needs to contain what we normally construct + what we passed in.
    assertThat(actualBuilder.command(), contains("docker-compose", "-f", "/test", "-p", "metron", "exec", "storm", "oo", "ooo"));

    verify(process).waitFor();
  }
}