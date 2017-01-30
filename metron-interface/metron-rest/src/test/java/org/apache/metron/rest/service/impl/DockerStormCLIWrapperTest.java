package org.apache.metron.rest.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;

import java.io.IOException;
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
    InputStream inputStream = new InputStream() {
      @Override
      public int read() throws IOException {
        return -1;
      }
    };

    when(processBuilder.start()).thenReturn(process);
    when(process.getInputStream()).thenReturn(inputStream);
    when(environment.getProperty("docker.compose.path")).thenReturn("/test");
    when(environment.getProperty("metron.version")).thenReturn("1");


    ProcessBuilder actualBuilder = dockerStormCLIWrapper.getProcessBuilder("oo", "ooo");

    assertEquals(new HashMap<String, String>() {{ put("METRON_VERSION", "1"); }}, actualBuilder.environment());
    assertEquals(new ArrayList<>(), actualBuilder.command());

    verify(process).waitFor();
  }
}