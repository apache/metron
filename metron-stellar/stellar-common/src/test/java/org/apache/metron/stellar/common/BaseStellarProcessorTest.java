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

package org.apache.metron.stellar.common;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ALL")
public class BaseStellarProcessorTest {
  BaseStellarProcessor<Object> processor;

  @BeforeEach
  public void setUp() throws Exception {
    processor = new BaseStellarProcessor<>(Object.class);
  }

  @Test
  public void makeSureBasicValidationIsWorking() throws Exception {
    {
      assertTrue(processor.validate(""));
      assertTrue(processor.validate(null));
      assertTrue(processor.validate("'ah'"));
      assertTrue(processor.validate("true"));
      assertTrue(processor.validate("1"));
      assertTrue(processor.validate("1L"));
      assertTrue(processor.validate("1F"));
      assertTrue(processor.validate("1 < 2 // always true"));
      assertTrue(processor.validate("1 < foo"));
      assertTrue(processor.validate("(1 < foo)"));
      assertTrue(processor.validate("foo < bar"));
      assertTrue(processor.validate("foo < TO_FLOAT(bar)", false, Context.EMPTY_CONTEXT()));
      assertTrue(processor.validate("if true then b else c"));
      assertTrue(processor.validate("IF false THEN b ELSE c"));
    }

    {
      assertFalse(processor.validate("'", false, Context.EMPTY_CONTEXT()));
      assertFalse(processor.validate("(", false, Context.EMPTY_CONTEXT()));
      assertFalse(processor.validate("()", false, Context.EMPTY_CONTEXT()));
      assertFalse(processor.validate("'foo", false, Context.EMPTY_CONTEXT()));
      assertFalse(processor.validate("foo << foo", false, Context.EMPTY_CONTEXT()));
      assertFalse(processor.validate("1-1", false, Context.EMPTY_CONTEXT()));
      assertFalse(processor.validate("1 -1", false, Context.EMPTY_CONTEXT()));
      assertFalse(processor.validate("If a then b else c", false, Context.EMPTY_CONTEXT()));
    }
  }

  @Test
  public void validateShouldProperlyThrowExceptionOnInvalidStellarExpression() throws Exception {
    ParseException e = assertThrows(ParseException.class, () -> processor.validate("'", true, Context.EMPTY_CONTEXT()));
    assertTrue(e.getMessage().contains("Unable to parse ': "));
  }

  @Test
  public void validateShouldProperlyThrowExceptionByDefaultOnInvalidStellarExpression() throws Exception {
    ParseException e = assertThrows(ParseException.class, () -> processor.validate("'", Context.EMPTY_CONTEXT()));
    assertTrue(e.getMessage().contains("Unable to parse ': "));
  }

  @Test
  public void validateShouldProperlyThrowExceptionByDefaultOnInvalidStellarExpression2() throws Exception {
    ParseException e = assertThrows(ParseException.class, () -> processor.validate("'"));
    assertTrue(e.getMessage().contains("Unable to parse ': "));
  }

  @Test
  public void validateMethodShouldFailOnUnknownFunctions() throws Exception {
    ParseException e = assertThrows(ParseException.class, () -> processor.validate("1 < UNKNOWN_FUNCTION(3)", Context.EMPTY_CONTEXT()));
    assertTrue(e.getMessage().contains(" Unable to resolve function named 'UNKNOWN_FUNCTION'."));
  }

  @Test
  public void validateMethodShouldNotFailOnUnknownvariables() throws Exception {
    assertTrue(processor.validate("unknown_variable\n\n"));
    assertTrue(processor.validate("unknown_variable > 2", Context.EMPTY_CONTEXT()));
  }

  @Test
  public void makeSureBasicLexerErrorsAreCaughtDuringValidation() throws Exception {
    assertFalse(processor.validate("true †", false, Context.EMPTY_CONTEXT()));
    assertFalse(processor.validate("¢ (1 + 2)", false, Context.EMPTY_CONTEXT()));
  }
}
