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
import javax.security.auth.Subject;
import oi.thekraken.grok.api.Grok;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.GrokValidation;
import org.apache.metron.rest.service.GrokService;
import org.apache.metron.rest.service.HdfsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.rest.MetronRestConstants.GROK_TEMP_PATH_SPRING_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class GrokServiceImplNoMockTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private Environment environment;
  private Grok grok;
  private GrokService grokService;

  private Configuration configuration;
  private HdfsService hdfsService;
  private String testDir = "./target/hdfsUnitTest/";

  @Before
  public void setup() throws IOException {
    configuration = new Configuration();
    hdfsService = new HdfsServiceImpl(configuration);
    File file = new File(testDir);
    if (!file.exists()) {
      file.mkdirs();
    }
    FileUtils.cleanDirectory(file);
    environment = mock(Environment.class);
    when(environment.getProperty(MetronRestConstants.HDFS_METRON_APPS_ROOT)).thenReturn(testDir);
    grok = mock(Grok.class);
    grokService = new GrokServiceImpl(environment, grok, new Configuration(),hdfsService);
  }

  @After
  public void teardown() throws IOException {
    File file = new File(testDir);
    FileUtils.cleanDirectory(file);
  }

  @Test
  public void saveStatementShouldSaveStatement() throws Exception {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("user1");
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String statement = "grok statement";
    grokService.saveStatement("/patterns/test/test", statement.getBytes(StandardCharsets.UTF_8));
    assertEquals(statement,grokService.getStatement("/patterns/test/test"));

  }

  @Test
  public void getStatementFromClasspathShouldReturnStatement() throws Exception {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("user1");
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String expected = FileUtils.readFileToString(new File("../../metron-platform/metron-parsers/src/main/resources/patterns/common"));
    assertEquals(expected, grokService.getStatement("/patterns/common"));
  }

  @Test
  public void getStatementFromClasspathShouldThrowRestException() throws Exception {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("user1");
    SecurityContextHolder.getContext().setAuthentication(authentication);
    exception.expect(RestException.class);
    exception.expectMessage("Could not find a statement at path /bad/path");

    grokService.getStatement("/bad/path");
  }
}
