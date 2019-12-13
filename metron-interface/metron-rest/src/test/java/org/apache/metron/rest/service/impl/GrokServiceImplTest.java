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

import static org.apache.metron.rest.MetronRestConstants.GROK_TEMP_PATH_SPRING_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import oi.thekraken.grok.api.Grok;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.GrokValidation;
import org.apache.metron.rest.service.GrokService;
import org.apache.metron.rest.service.HdfsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class GrokServiceImplTest {
  private Environment environment;
  private Grok grok;
  private HdfsService hdfsService;
  private GrokService grokService;

  @BeforeEach
  public void setUp() {
    environment = mock(Environment.class);
    grok = mock(Grok.class);
    hdfsService = new HdfsServiceImpl(new Configuration());
    grokService = new GrokServiceImpl(environment, grok, hdfsService);
  }

  @Test
  public void getCommonGrokPattersShouldCallGrokToGetPatterns() {
    grokService.getCommonGrokPatterns();

    verify(grok).getPatterns();
  }

  @Test
  public void getCommonGrokPattersShouldCallGrokToGetPatternsAndNotAlterValue() {
    final Map<String, String> actual = new HashMap<String, String>() {{
      put("k", "v");
      put("k1", "v1");
    }};

    when(grok.getPatterns()).thenReturn(actual);

    Map<String, String> expected = new HashMap<String, String>() {{
      put("k", "v");
      put("k1", "v1");
    }};
    assertEquals(expected, grokService.getCommonGrokPatterns());
  }

  @Test
  public void validateGrokStatementShouldThrowExceptionWithNullStringAsPatternLabel() {
    GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData("asdf asdf");
    grokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2}");

    RestException e = assertThrows(RestException.class, () -> grokService.validateGrokStatement(grokValidation));
    assertEquals("Pattern label is required", e.getMessage());
  }

  @Test
  public void validateGrokStatementShouldThrowExceptionWithEmptyStringAsStatement() {
    GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData("asdf asdf");
    grokValidation.setPatternLabel("LABEL");
    grokValidation.setStatement("");

    RestException e = assertThrows(RestException.class, () -> grokService.validateGrokStatement(grokValidation));
    assertEquals("Grok statement is required", e.getMessage());
  }

  @Test
  public void validateGrokStatementShouldThrowExceptionWithNullStringAsStatement() {
    GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData("asdf asdf");
    grokValidation.setPatternLabel("LABEL");
    grokValidation.setStatement(null);

    RestException e = assertThrows(RestException.class, () -> grokService.validateGrokStatement(grokValidation));
    assertEquals("Grok statement is required", e.getMessage());
  }

  @Test
  public void validateGrokStatementShouldProperlyMatchSampleDataAgainstGivenStatement() throws Exception {
    final GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData("asdf asdf");
    grokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2}");
    grokValidation.setPatternLabel("LABEL");

    GrokValidation expected = new GrokValidation();
    expected.setResults(new HashMap<String, Object>() {{ put("word1", "asdf"); put("word2", "asdf"); }});
    expected.setSampleData("asdf asdf");
    expected.setStatement("LABEL %{WORD:word1} %{WORD:word2}");
    expected.setPatternLabel("LABEL");

    GrokValidation actual = grokService.validateGrokStatement(grokValidation);
    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
  }

  @Test
  public void validateGrokStatementShouldProperlyMatchNothingAgainstEmptyString() throws Exception {
    final GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData("");
    grokValidation.setPatternLabel("LABEL");
    grokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2}");

    GrokValidation expected = new GrokValidation();
    expected.setResults(new HashMap<>());
    expected.setSampleData("");
    expected.setPatternLabel("LABEL");
    expected.setStatement("LABEL %{WORD:word1} %{WORD:word2}");

    assertEquals(expected, grokService.validateGrokStatement(grokValidation));
  }

  @Test
  public void validateGrokStatementShouldProperlyMatchNothingAgainstNullString() throws Exception {
    final GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData(null);
    grokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2}");
    grokValidation.setPatternLabel("LABEL");

    GrokValidation expected = new GrokValidation();
    expected.setResults(new HashMap<>());
    expected.setSampleData(null);
    expected.setStatement("LABEL %{WORD:word1} %{WORD:word2}");
    expected.setPatternLabel("LABEL");

    assertEquals(expected, grokService.validateGrokStatement(grokValidation));
  }

  @Test
  public void invalidGrokStatementShouldThrowRestException() {
    GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData(null);
    grokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2");

    assertThrows(RestException.class, () -> grokService.validateGrokStatement(grokValidation));
  }

  @Test
  public void saveTemporaryShouldProperlySaveFile() throws Exception {
    new File("./target/user1").delete();
    String statement = "grok statement";

    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("user1");
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(environment.getProperty(GROK_TEMP_PATH_SPRING_PROPERTY)).thenReturn("./target");

    grokService.saveTemporary(statement, "squid");

    File testRoot = new File("./target/user1");
    assertEquals(statement, FileUtils.readFileToString(new File(testRoot, "squid"), StandardCharsets.UTF_8));
    testRoot.delete();
  }

  @Test
  public void missingGrokStatementShouldThrowRestException() {
    RestException e = assertThrows(RestException.class, () -> grokService.saveTemporary(null, "squid"));
    assertEquals("A grokStatement must be provided", e.getMessage());
  }

  @Test
  public void getStatementFromClasspathShouldReturnStatement() throws Exception {
    String expected = FileUtils.readFileToString(new File(
        "../../metron-platform/metron-parsing/metron-parsers/src/main/resources/patterns/squid"));
    assertEquals(expected, grokService.getStatementFromClasspath("/patterns/squid"));
  }

  @Test
  public void getStatementFromClasspathShouldThrowRestException() {
    RestException e = assertThrows(RestException.class, () -> grokService.getStatementFromClasspath("/bad/path"));
    assertEquals("Could not find a statement at path /bad/path", e.getMessage());
  }
}
