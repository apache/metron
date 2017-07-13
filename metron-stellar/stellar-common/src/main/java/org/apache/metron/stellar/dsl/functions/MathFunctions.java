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
package org.apache.metron.stellar.dsl.functions;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.List;
import java.util.function.Function;

public class MathFunctions {

  private enum SingleArgMathFunctions implements Function<Double, Double> {
    ABS(d -> Math.abs(d)),
    LOG10(d -> Math.log10(d)),
    LOG2(d -> Math.log(d)/Math.log(2)),
    LN(d -> Math.log(d)),
    SQRT(d -> Math.sqrt(d)),
    CEIL(d -> Math.ceil(d)),
    FLOOR(d -> Math.floor(d)),
    SIN(d -> Math.sin(d)),
    COS(d -> Math.cos(d)),
    TAN(d -> Math.tan(d)),
    ;

    Function<Double, Double> _func;
    SingleArgMathFunctions(Function<Double, Double> _func) {
      this._func = _func;
    }

    @Override
    public Double apply(Double d) {
      return _func.apply(d);
    }
  }

  private static class SingleArgMathFunction implements StellarFunction {
    Function<Double, Double> _func;
    public SingleArgMathFunction(Function<Double, Double> _func) {
      this._func = _func;
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(args.size() < 1) {
        return Double.NaN;
      }
      Number n = (Number)args.get(0);
      if(n == null) {
        return Double.NaN;
      }
      return _func.apply(n.doubleValue());
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }


  @Stellar(name="ABS"
          ,description="Returns the absolute value of a number."
          ,params = {
                "number - The number to take the absolute value of"
                    }
          , returns="The absolute value of the number passed in."
          )
  public static class Abs extends SingleArgMathFunction{


    public Abs() {
      super(SingleArgMathFunctions.ABS);
    }
  }

  @Stellar(name="LOG10"
          ,description="Returns the log (base 10) of a number."
          ,params = {
                "number - The number to take the log (base 10) value of"
                    }
          , returns="The log base 10 of the number passed in."
          )
  public static class Log10 extends SingleArgMathFunction {
   public Log10() {
      super(SingleArgMathFunctions.LOG10);
    }

  }

  @Stellar(name="LOG2"
          ,description="Returns the log (base 2) of a number."
          ,params = {
                "number - The number to take the log (base 2) value of"
                    }
          , returns="The log base 2 of the number passed in."
          )
  public static class Log2 extends SingleArgMathFunction {
   public Log2() {
      super(SingleArgMathFunctions.LOG2);
    }

  }

  @Stellar(name="LN"
          ,description="Returns the natural log of a number."
          ,params = {
                "number - The number to take the natural log value of"
                    }
          , returns="The natural log of the number passed in."
          )
  public static class Ln extends SingleArgMathFunction {
   public Ln() {
      super(SingleArgMathFunctions.LN);
    }

  }

  @Stellar(name="SQRT"
          ,description="Returns the square root of a number."
          ,params = {
                "number - The number to take the square root of"
                    }
          , returns="The square root of the number passed in."
          )
  public static class Sqrt extends SingleArgMathFunction {
   public Sqrt() {
      super(SingleArgMathFunctions.SQRT);
    }

  }

  @Stellar(name="CEIL"
          ,description="Returns the ceiling of a number."
          ,params = {
                "number - The number to take the ceiling of"
                    }
          , returns="The ceiling of the number passed in."
          )
  public static class Ceil extends SingleArgMathFunction {
   public Ceil() {
      super(SingleArgMathFunctions.CEIL);
    }

  }

  @Stellar(name="FLOOR"
          ,description="Returns the floor of a number."
          ,params = {
                "number - The number to take the floor of"
                    }
          , returns="The floor of the number passed in."
          )
  public static class Floor extends SingleArgMathFunction {
   public Floor() {
      super(SingleArgMathFunctions.FLOOR);
    }
  }

  @Stellar(name="SIN"
          ,description="Returns the sin of a number."
          ,params = {
                "number - The number to take the sin of"
                    }
          , returns="The sin of the number passed in."
          )
  public static class Sin extends SingleArgMathFunction {
   public Sin() {
      super(SingleArgMathFunctions.SIN);
    }
  }

  @Stellar(name="COS"
          ,description="Returns the cos of a number."
          ,params = {
                "number - The number to take the cos of"
                    }
          , returns="The cos of the number passed in."
          )
  public static class Cos extends SingleArgMathFunction {
   public Cos() {
      super(SingleArgMathFunctions.COS);
    }
  }

  @Stellar(name="TAN"
          ,description="Returns the tan of a number."
          ,params = {
                "number - The number to take the tan of"
                    }
          , returns="The tan of the number passed in."
          )
  public static class Tan extends SingleArgMathFunction {
   public Tan() {
      super(SingleArgMathFunctions.TAN);
    }
  }
}
