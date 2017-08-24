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
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.metron.rest.MetronRestConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HadoopConfig.class, UserGroupInformation.class})
public class HadoopConfigTest {

  private Environment environment;
  private HadoopConfig hadoopConfig;

  @Before
  public void setUp() throws Exception {
    environment = mock(Environment.class);
    hadoopConfig = new HadoopConfig(environment);
    mockStatic(UserGroupInformation.class);
  }

  @Test
  public void configurationShouldReturnProperKerberosConfiguration() throws IOException {
    when(environment.getProperty(MetronRestConstants.KERBEROS_KEYTAB_SPRING_PROPERTY)).thenReturn("metron keytabLocation");
    when(environment.getProperty(MetronRestConstants.KERBEROS_PRINCIPLE_SPRING_PROPERTY)).thenReturn("metron principal");

    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(true);

    Configuration configuration = hadoopConfig.configuration();

    verifyStatic();
    UserGroupInformation.setConfiguration(any(Configuration.class));
    UserGroupInformation.loginUserFromKeytab("metron keytabLocation", "metron principal");
  }

  @Test
  public void configurationShouldReturnProperConfiguration() throws IOException {
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);

    Configuration configuration = hadoopConfig.configuration();

    verifyStatic(never());
    UserGroupInformation.setConfiguration(any(Configuration.class));
    UserGroupInformation.loginUserFromKeytab(anyString(), anyString());

    assertEquals("simple", configuration.get("hadoop.security.authentication"));
  }
}
