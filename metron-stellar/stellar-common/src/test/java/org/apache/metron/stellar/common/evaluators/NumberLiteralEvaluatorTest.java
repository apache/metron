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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class NumberLiteralEvaluatorTest {
  NumberEvaluator<StellarParser.IntLiteralContext> intLiteralContextNumberEvaluator;
  NumberEvaluator<StellarParser.DoubleLiteralContext> doubleLiteralContextNumberEvaluator;
  NumberEvaluator<StellarParser.FloatLiteralContext> floatLiteralContextNumberEvaluator;
  NumberEvaluator<StellarParser.LongLiteralContext> longLiteralContextNumberEvaluator;

  Map<Class<? extends StellarParser.Arithmetic_operandsContext>, NumberEvaluator> instanceMap;

  @BeforeEach
  public void setUp() throws Exception {
    intLiteralContextNumberEvaluator = mock(IntLiteralEvaluator.class);
    doubleLiteralContextNumberEvaluator = mock(DoubleLiteralEvaluator.class);
    floatLiteralContextNumberEvaluator = mock(FloatLiteralEvaluator.class);
    longLiteralContextNumberEvaluator = mock(LongLiteralEvaluator.class);
    instanceMap = new HashMap<Class<? extends StellarParser.Arithmetic_operandsContext>, NumberEvaluator>() {{
      put(mock(StellarParser.IntLiteralContext.class).getClass(), intLiteralContextNumberEvaluator);
      put(mock(StellarParser.DoubleLiteralContext.class).getClass(), doubleLiteralContextNumberEvaluator);
      put(mock(StellarParser.FloatLiteralContext.class).getClass(), floatLiteralContextNumberEvaluator);
      put(mock(StellarParser.LongLiteralContext.class).getClass(), longLiteralContextNumberEvaluator);
    }};
  }

  @Test
  public void verifyIntLiteralContextIsProperlyEvaluated() {
    StellarParser.IntLiteralContext context = mock(StellarParser.IntLiteralContext.class);
    NumberLiteralEvaluator.INSTANCE.evaluate(context, instanceMap, null);

    verify(intLiteralContextNumberEvaluator).evaluate(context, null);
    verifyNoInteractions(doubleLiteralContextNumberEvaluator, floatLiteralContextNumberEvaluator, longLiteralContextNumberEvaluator);
  }

  @Test
  public void verifyDoubleLiteralContextIsProperlyEvaluated() {
    StellarParser.DoubleLiteralContext context = mock(StellarParser.DoubleLiteralContext.class);
    NumberLiteralEvaluator.INSTANCE.evaluate(context, instanceMap, null);

    verify(doubleLiteralContextNumberEvaluator).evaluate(context, null);
    verifyNoInteractions(intLiteralContextNumberEvaluator, floatLiteralContextNumberEvaluator, longLiteralContextNumberEvaluator);
  }

  @Test
  public void verifyFloatLiteralContextIsProperlyEvaluated() {
    StellarParser.FloatLiteralContext context = mock(StellarParser.FloatLiteralContext.class);
    NumberLiteralEvaluator.INSTANCE.evaluate(context, instanceMap, null);

    verify(floatLiteralContextNumberEvaluator).evaluate(context, null);
    verifyNoInteractions(doubleLiteralContextNumberEvaluator, intLiteralContextNumberEvaluator, longLiteralContextNumberEvaluator);
  }

  @Test
  public void verifyLongLiteralContextIsProperlyEvaluated() {
    StellarParser.LongLiteralContext context = mock(StellarParser.LongLiteralContext.class);
    NumberLiteralEvaluator.INSTANCE.evaluate(context, instanceMap, null);

    verify(longLiteralContextNumberEvaluator).evaluate(context, null);
    verifyNoInteractions(doubleLiteralContextNumberEvaluator, floatLiteralContextNumberEvaluator, intLiteralContextNumberEvaluator);
  }

  @Test
  public void verifyExceptionThrownForUnsupportedContextType() {
    StellarParser.VariableContext context = mock(StellarParser.VariableContext.class);

    ParseException e = assertThrows(ParseException.class, () -> NumberLiteralEvaluator.INSTANCE.evaluate(context, instanceMap, null));
    assertEquals("Does not support evaluation for type " + context.getClass(), e.getMessage());

    verifyNoInteractions(longLiteralContextNumberEvaluator, doubleLiteralContextNumberEvaluator, floatLiteralContextNumberEvaluator, intLiteralContextNumberEvaluator);
  }
}
