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

import org.apache.metron.stellar.common.FrameContext;
import org.apache.metron.stellar.common.generated.StellarParser;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Token;

import java.util.HashMap;
import java.util.Map;

public enum NumberLiteralEvaluator {
  INSTANCE;
  public enum Strategy {
     INTEGER(StellarParser.IntLiteralContext.class, new IntLiteralEvaluator())
    , DOUBLE(StellarParser.DoubleLiteralContext.class, new DoubleLiteralEvaluator())
    , FLOAT(StellarParser.FloatLiteralContext.class, new FloatLiteralEvaluator())
    , LONG(StellarParser.LongLiteralContext.class, new LongLiteralEvaluator());
    Class<? extends StellarParser.Arithmetic_operandsContext> context;
    NumberEvaluator evaluator;
    private static Map<Class<? extends StellarParser.Arithmetic_operandsContext>, NumberEvaluator> strategyMap;

    static {
      strategyMap = new HashMap<>();
      for (Strategy strat : Strategy.values()) {
        strategyMap.put(strat.context, strat.evaluator);
      }
    }

    Strategy(Class<? extends StellarParser.Arithmetic_operandsContext> context
            , NumberEvaluator<? extends StellarParser.Arithmetic_operandsContext> evaluator
    ) {
      this.context = context;
      this.evaluator = evaluator;
    }
  }

  @SuppressWarnings("unchecked")
  Token<? extends Number> evaluate(StellarParser.Arithmetic_operandsContext context,
                                          Map<Class<? extends StellarParser.Arithmetic_operandsContext>, NumberEvaluator> instanceMap,
                                          FrameContext.Context contextVariety
                                         ) {
    NumberEvaluator evaluator = instanceMap.get(context.getClass());
    if (evaluator == null) {
      throw new ParseException("Does not support evaluation for type " + context.getClass());
    }
    return evaluator.evaluate(context, contextVariety);
  }

  public Token<? extends Number> evaluate(StellarParser.Arithmetic_operandsContext context, FrameContext.Context contextVariety) {
    return evaluate(context, Strategy.strategyMap, contextVariety);
  }

}
