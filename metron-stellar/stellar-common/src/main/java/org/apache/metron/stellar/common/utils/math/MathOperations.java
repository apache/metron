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
package org.apache.metron.stellar.common.utils.math;

import java.util.function.BiFunction;
import java.util.function.Function;

public enum MathOperations implements Function<Number[], Number> {
  ABS(d -> Math.abs(d)),
  CEIL(d -> Math.ceil(d)),
  COS(d -> Math.cos(d)),
  FLOOR(d -> Math.floor(d)),
  LOG10(d -> Math.log10(d)),
  LOG2(d -> Math.log(d)/Math.log(2)),
  LN(d -> Math.log(d)),
  SIN(d -> Math.sin(d)),
  SQRT(d -> Math.sqrt(d)),
  TAN(d -> Math.tan(d)),
  EXP(d -> Math.exp(d)),
  ROUND(new MathOperation(d -> {
    Double val = d[0].doubleValue();
    return Double.isNaN(val)?Double.NaN:Math.round(d[0].doubleValue());
  }, 1, 1)),
  ;

  private static class SingleOpFunc implements Function<Number[], Number> {
    Function<Double, Number> f;
    public SingleOpFunc(Function<Double, Number> f) {
      this.f = f;
    }
    @Override
    public Number apply(Number[] numbers) {
      return f.apply(numbers[0].doubleValue());
    }
  }

  private static class BinaryOpFunc implements Function<Number[], Number> {
    BiFunction<Double, Double, Number> f;
    public BinaryOpFunc(BiFunction<Double, Double, Number> f) {
      this.f = f;
    }
    @Override
    public Number apply(Number[] numbers) {
      return f.apply(numbers[0].doubleValue(), numbers[1].doubleValue());
    }
  }

  MathOperation op;
  MathOperations(Function<Double, Number> singleArg) {
    op = new MathOperation(new SingleOpFunc(singleArg), 1, 1);
  }

  MathOperations(BiFunction<Double, Double, Number> binaryArg) {
    op = new MathOperation(new BinaryOpFunc(binaryArg), 2, 2);
  }

  MathOperations(MathOperation op)
  {
    this.op = op;
  }

  @Override
  public Number apply(Number[] in) {
      return op.getOperation().apply(in);
  }
}
