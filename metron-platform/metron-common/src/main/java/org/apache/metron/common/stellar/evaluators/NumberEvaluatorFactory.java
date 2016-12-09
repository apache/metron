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

package org.apache.metron.common.stellar.evaluators;

import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.stellar.generated.StellarParser;

public class NumberEvaluatorFactory {
  private NumberEvaluator<StellarParser.IntLiteralContext> intLiteralEvaluator;
  private NumberEvaluator<StellarParser.DoubleLiteralContext> doubleLiteralEvaluator;
  private NumberEvaluator<StellarParser.FloatLiteralContext> floatLiteralEvaluator;
  private NumberEvaluator<StellarParser.LongLiteralContext> longLiteralEvaluator;

  public NumberEvaluatorFactory(NumberEvaluator<StellarParser.IntLiteralContext> intLiteralEvaluator,
                                NumberEvaluator<StellarParser.DoubleLiteralContext> doubleLiteralEvaluator,
                                NumberEvaluator<StellarParser.FloatLiteralContext> floatLiteralEvaluator,
                                NumberEvaluator<StellarParser.LongLiteralContext> longLiteralEvaluator) {
    this.intLiteralEvaluator = intLiteralEvaluator;
    this.doubleLiteralEvaluator = doubleLiteralEvaluator;
    this.floatLiteralEvaluator = floatLiteralEvaluator;
    this.longLiteralEvaluator = longLiteralEvaluator;
  }

  public NumberEvaluatorFactory() {
    this.intLiteralEvaluator = new IntLiteralEvaluator();
    this.doubleLiteralEvaluator = new DoubleLiteralEvaluator();
    this.floatLiteralEvaluator = new FloatLiteralEvaluator();
    this.longLiteralEvaluator = new LongLiteralEvaluator();
  }

  public Token<? extends Number> evaluate(StellarParser.Arithmetic_operandsContext context) {
    if (context instanceof StellarParser.IntLiteralContext) {
      return intLiteralEvaluator.evaluate((StellarParser.IntLiteralContext) context);
    } else if (context instanceof StellarParser.DoubleLiteralContext) {
      return doubleLiteralEvaluator.evaluate((StellarParser.DoubleLiteralContext) context);
    } else if (context instanceof StellarParser.FloatLiteralContext) {
      return floatLiteralEvaluator.evaluate((StellarParser.FloatLiteralContext) context);
    } else if (context instanceof StellarParser.LongLiteralContext) {
      return longLiteralEvaluator.evaluate((StellarParser.LongLiteralContext) context);
    }

    throw new ParseException("Does not support evaluation for type " + context.getClass());
  }

}
