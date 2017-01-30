package org.apache.metron.rest.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerStormCLIWrapperTest {
  private Environment environment;
  private DockerStormCLIWrapper dockerStormCLIWrapper;

  @Before
  public void setUp() throws Exception {
    environment = mock(Environment.class);

    dockerStormCLIWrapper = new DockerStormCLIWrapper(environment);
  }

  @Test
  public void getProcessBuilderShouldProperlyGenerateProcessorBuilder() throws Exception {
    ProcessBuilder expectedProcessBuilder = new ProcessBuilder("docker-compose", "-f", "/test", "-p", "metron", "exec", "storm", "oo", "ooo");
    expectedProcessBuilder.environment().put("METRON_VERSION", "1");


    when(environment.getProperty("docker.compose.path")).thenReturn("/test");
    when(environment.getProperty("metron.version")).thenReturn("1");


    ProcessBuilder actualBuilder = dockerStormCLIWrapper.getProcessBuilder("oo", "ooo");

    assertEquals(expectedProcessBuilder.environment(), actualBuilder.environment());
    assertEquals(expectedProcessBuilder.command(), actualBuilder.command());
  }
}