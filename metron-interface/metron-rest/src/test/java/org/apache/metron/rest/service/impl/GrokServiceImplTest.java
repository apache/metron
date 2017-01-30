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

import oi.thekraken.grok.api.Grok;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.GrokValidation;
import org.apache.metron.rest.service.GrokService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GrokServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private Grok grok;
  private GrokService grokService;

  @Before
  public void setUp() throws Exception {
    grok = mock(Grok.class);
    grokService = new GrokServiceImpl(grok);
  }

  @Test
  public void getCommonGrokPattersShouldCallGrokToGetPatterns() throws Exception {
    grokService.getCommonGrokPatterns();

    verify(grok).getPatterns();
  }

  @Test
  public void getCommonGrokPattersShouldCallGrokToGetPatternsAndNotAlterValue() throws Exception {
    Map<String, String> patterns = new HashMap<String, String>() {{
      put("k", "v");
      put("k1", "v1");
    }};

    when(grok.getPatterns()).thenReturn(patterns);

    assertEquals(patterns, grokService.getCommonGrokPatterns());
  }

  @Test
  public void validateGrokStatementShouldThrowExceptionWithEmptyStringAsStatement() throws Exception {
    exception.expect(RestException.class);
    exception.expectMessage("A pattern label must be included (eg. PATTERN_LABEL %{PATTERN:field} ...)");

    GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData("asdf asdf");
    grokValidation.setStatement("");

    grokService.validateGrokStatement(grokValidation);
  }

  @Test
  public void validateGrokStatementShouldThrowExceptionWithNullStringAsStatement() throws Exception {
    exception.expect(RestException.class);
    exception.expectMessage("A pattern label must be included (eg. PATTERN_LABEL %{PATTERN:field} ...)");

    GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData("asdf asdf");
    grokValidation.setStatement(null);

    grokService.validateGrokStatement(grokValidation);
  }

  @Test
  public void validateGrokStatementShouldProperlyMatchSampleDataAgainstGivenStatement() throws Exception {
    GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData("asdf asdf");
    grokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2}");

    GrokValidation expectedGrokValidation = new GrokValidation();
    expectedGrokValidation.setResults(new HashMap<String, Object>() {{ put("word1", "asdf"); put("word2", "asdf"); }});
    expectedGrokValidation.setSampleData("asdf asdf");
    expectedGrokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2}");

    assertEquals(expectedGrokValidation, grokService.validateGrokStatement(grokValidation));
  }

  @Test
  public void validateGrokStatementShouldProperlyMatchNothingAgainstEmptyString() throws Exception {
    GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData("");
    grokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2}");

    GrokValidation expectedGrokValidation = new GrokValidation();
    expectedGrokValidation.setResults(new HashMap<>());
    expectedGrokValidation.setSampleData("");
    expectedGrokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2}");

    assertEquals(expectedGrokValidation, grokService.validateGrokStatement(grokValidation));
  }

  @Test
  public void validateGrokStatementShouldProperlyMatchNothingAgainstNullString() throws Exception {
    GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData(null);
    grokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2}");

    GrokValidation expectedGrokValidation = new GrokValidation();
    expectedGrokValidation.setResults(new HashMap<>());
    expectedGrokValidation.setSampleData(null);
    expectedGrokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2}");

    assertEquals(expectedGrokValidation, grokService.validateGrokStatement(grokValidation));
  }

  @Test
  public void invalidGrokStatementShouldThrowRestException() throws Exception {
    exception.expect(RestException.class);

    GrokValidation grokValidation = new GrokValidation();
    grokValidation.setResults(new HashMap<>());
    grokValidation.setSampleData(null);
    grokValidation.setStatement("LABEL %{WORD:word1} %{WORD:word2");

    grokService.validateGrokStatement(grokValidation);
  }
}