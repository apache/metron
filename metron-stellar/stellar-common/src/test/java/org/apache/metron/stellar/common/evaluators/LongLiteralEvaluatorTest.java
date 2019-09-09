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

package org.apache.metron.stellar.common.evaluators;

import org.apache.metron.stellar.common.generated.StellarParser;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class LongLiteralEvaluatorTest {
  NumberEvaluator<StellarParser.LongLiteralContext> evaluator;
  StellarParser.LongLiteralContext context;

  @BeforeEach
  public void setUp() throws Exception {
    evaluator = new LongLiteralEvaluator();
    context = mock(StellarParser.LongLiteralContext.class);
  }

  @Test
  public void verifyHappyPathEvaluation() {
    when(context.getText()).thenReturn("100L");

    Token<? extends Number> evaluated = evaluator.evaluate(context, null);
    assertEquals(new Token<>(100L, Long.class, null), evaluated);

    verify(context).getText();
    verifyNoMoreInteractions(context);
  }

  @Test
  public void verifyNumberFormationExceptionWithEmptyString() {
    when(context.getText()).thenReturn("");
    Exception e = assertThrows(ParseException.class, () -> evaluator.evaluate(context, null));
    assertEquals(
        "Invalid format for long. Failed trying to parse a long with the following value: ",
        e.getMessage());
  }

  @Test
  public void throwIllegalArgumentExceptionWhenContextIsNull() {
    Exception e =
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(null, null));
    assertEquals("Cannot evaluate a context that is null.", e.getMessage());
  }
}
