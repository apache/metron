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
package org.apache.metron.rest.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.metron.rest.MetronRestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class HadoopConfigTest {

  private Environment environment;
  private HadoopConfig hadoopConfig;

  @BeforeEach
  public void setUp() {
    environment = mock(Environment.class);
    hadoopConfig = spy(new HadoopConfig(environment));
  }

  @Test
  public void configurationShouldReturnProperKerberosConfiguration() throws IOException {
    when(environment.getProperty(MetronRestConstants.KERBEROS_KEYTAB_SPRING_PROPERTY)).thenReturn("metron keytabLocation");
    when(environment.getProperty(MetronRestConstants.KERBEROS_PRINCIPLE_SPRING_PROPERTY)).thenReturn("metron principal");

    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(true);

    doNothing().when(hadoopConfig).setUGIConfiguration(any());
    doNothing().when(hadoopConfig).loginUserFromKeytab(any(), any());

    hadoopConfig.configuration();

    verify(hadoopConfig).setUGIConfiguration(any());
    verify(hadoopConfig).loginUserFromKeytab("metron keytabLocation", "metron principal");
  }

  @Test
  public void configurationShouldReturnProperConfiguration() throws IOException {
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);

    Configuration configuration = hadoopConfig.configuration();

    doNothing().when(hadoopConfig).setUGIConfiguration(any());
    doNothing().when(hadoopConfig).loginUserFromKeytab(any(), any());

    hadoopConfig.configuration();

    verify(hadoopConfig, times(0)).setUGIConfiguration(any());
    verify(hadoopConfig, times(0)).loginUserFromKeytab(any(), any());

    assertEquals("simple", configuration.get("hadoop.security.authentication"));
  }
}
