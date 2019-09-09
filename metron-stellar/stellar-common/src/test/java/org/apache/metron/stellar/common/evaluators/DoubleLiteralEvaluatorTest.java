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
import org.apache.metron.stellar.dsl.Token;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class DoubleLiteralEvaluatorTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  NumberEvaluator<StellarParser.DoubleLiteralContext> evaluator;
  StellarParser.DoubleLiteralContext context;

  @BeforeEach
  public void setUp() throws Exception {
    evaluator = new DoubleLiteralEvaluator();
    context = mock(StellarParser.DoubleLiteralContext.class);
  }

  @Test
  public void verifyHappyPathEvaluation() {
    when(context.getText()).thenReturn("100D");

    Token<? extends Number> evaluated = evaluator.evaluate(context, null);
    assertEquals(new Token<>(100D, Double.class, null), evaluated);

    verify(context).getText();
    verifyNoMoreInteractions(context);
  }

  @Test
  public void verifyNumberFormationExceptionWithEmptyString() {
    exception.expect(NumberFormatException.class);

    when(context.getText()).thenReturn("");
    evaluator.evaluate(context, null);
  }

  @Test
  public void throwIllegalArgumentExceptionWhenContextIsNull() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Cannot evaluate a context that is null.");

    evaluator.evaluate(null, null);
  }
}
