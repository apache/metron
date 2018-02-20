/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.stellar.common.shell;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the DefaultStellarAutoCompleter class.
 */
public class DefaultStellarAutoCompleterTest {

  DefaultStellarAutoCompleter completer;

  @Before
  public void setup() {
    completer = new DefaultStellarAutoCompleter();
  }

  @Test
  public void testAutoCompleteFunction() {

    // setup some candidate functions for auto-completion
    completer.addCandidateFunction("FREUD");
    completer.addCandidateFunction("FRIEND");
    completer.addCandidateFunction("FOE");

    // need help auto-completer!
    Iterable<String> result = completer.autoComplete("FR");

    // there are only 2 candidates that make any sense
    List<String> completes = Lists.newArrayList(result);
    assertEquals(2, completes.size());

    // these are functions and so should include an opening paren
    assertThat(completes, hasItem("FREUD("));
    assertThat(completes, hasItem("FRIEND("));
  }

  @Test
  public void testNoCandidateFunctions() {

    // setup some candidate functions for auto-completion
    completer.addCandidateFunction("FREUD");
    completer.addCandidateFunction("FRIEND");
    completer.addCandidateFunction("FOE");

    // need help auto-completer!
    Iterable<String> result = completer.autoComplete("G");

    // there are no candidates that make any sense
    List<String> completes = Lists.newArrayList(result);
    assertEquals(0, completes.size());
  }

  @Test
  public void testAutoCompleteVariable() {

    // setup some candidates for auto-completion
    completer.addCandidateVariable("very");
    completer.addCandidateVariable("vast");
    completer.addCandidateVariable("vat");

    // need help auto-completer!
    Iterable<String> result = completer.autoComplete("va");

    // there are only 2 candidates that make any sense
    List<String> completes = Lists.newArrayList(result);
    assertEquals(2, completes.size());
    assertThat(completes, hasItem("vast"));
    assertThat(completes, hasItem("vat"));
  }

  @Test
  public void testNoCandidateVariable() {

    // setup some candidates for auto-completion
    completer.addCandidateVariable("very");
    completer.addCandidateVariable("vast");
    completer.addCandidateVariable("vat");

    // need help auto-completer!
    Iterable<String> result = completer.autoComplete("x");

    // there are only no candidates that make any sense
    List<String> completes = Lists.newArrayList(result);
    assertEquals(0, completes.size());
  }

  @Test
  public void testAutoCompleteDocString() {

    // setup some candidate functions for auto-completion
    completer.addCandidateFunction("FREUD");
    completer.addCandidateFunction("FRIEND");
    completer.addCandidateFunction("FOE");

    // need help auto-completer!
    Iterable<String> result = completer.autoComplete("?FR");

    // there are only 2 candidates that make any sense
    List<String> completes = Lists.newArrayList(result);
    assertEquals(2, completes.size());

    // the suggestions should include the docstring prefix
    assertThat(completes, hasItem("?FREUD"));
    assertThat(completes, hasItem("?FRIEND"));
  }

  @Test
  public void testNoCandidateDocStrings() {

    // setup some candidate functions for auto-completion
    completer.addCandidateFunction("FREUD");
    completer.addCandidateFunction("FRIEND");
    completer.addCandidateFunction("FOE");

    // need help auto-completer!
    Iterable<String> result = completer.autoComplete("?G");

    // there are only no candidates that make any sense
    List<String> completes = Lists.newArrayList(result);
    assertEquals(0, completes.size());
  }

  @Test
  public void testAutoCompleteMagic() {

    // setup some candidate functions for auto-completion
    completer.addCandidateFunction("%vars");
    completer.addCandidateFunction("%vast");
    completer.addCandidateFunction("%verbotten");

    // need help auto-completer!
    Iterable<String> result = completer.autoComplete("%va");

    // there are only 2 candidates that make any sense
    List<String> completes = Lists.newArrayList(result);
    assertEquals(2, completes.size());

    // the suggestions should include the docstring prefix
    assertThat(completes, hasItem("%vars"));
    assertThat(completes, hasItem("%vast"));
  }

  @Test
  public void testNoCandidateMagic() {

    // setup some candidate functions for auto-completion
    completer.addCandidateFunction("%vars");
    completer.addCandidateFunction("%vast");
    completer.addCandidateFunction("%verbotten");

    // need help auto-completer!
    Iterable<String> result = completer.autoComplete("%xy");

    // there are only no candidates that make any sense
    List<String> completes = Lists.newArrayList(result);
    assertEquals(0, completes.size());
  }
}
