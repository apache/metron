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

import org.apache.metron.rest.MetronRestConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.kerberos.client.KerberosRestTemplate;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RestTemplateConfig.class, KerberosRestTemplate.class, RestTemplate.class})
public class RestTemplateConfigTest {

  private Environment environment;
  private RestTemplateConfig restTemplateConfig;

  @Before
  public void setUp() throws Exception {
    environment = mock(Environment.class);
    restTemplateConfig = new RestTemplateConfig(environment);
  }

  @Test
  public void restTemplateShouldReturnProperTemplate() throws Exception {
    when(environment.getProperty(MetronRestConstants.KERBEROS_KEYTAB_SPRING_PROPERTY)).thenReturn("metron keytabLocation");
    when(environment.getProperty(MetronRestConstants.KERBEROS_PRINCIPLE_SPRING_PROPERTY)).thenReturn("metron principal");

    whenNew(KerberosRestTemplate.class).withParameterTypes(String.class, String.class).withArguments("metron keytabLocation", "metron principal").thenReturn(null);
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(true);

    restTemplateConfig.restTemplate();
    verifyNew(KerberosRestTemplate.class).withArguments("metron keytabLocation", "metron principal");

    whenNew(RestTemplate.class).withNoArguments().thenReturn(null);
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);

    restTemplateConfig.restTemplate();
    verifyNew(RestTemplate.class).withNoArguments();
  }


}
