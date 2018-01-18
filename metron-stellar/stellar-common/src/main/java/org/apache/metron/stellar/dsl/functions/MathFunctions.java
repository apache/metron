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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.common.utils.math.MathOperations;
import org.apache.metron.stellar.common.utils.math.StellarMathFunction;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;

public class MathFunctions {


  @Stellar(name="ABS"
          ,description="Returns the absolute value of a number."
          ,params = {
                "number - The number to take the absolute value of"
                    }
          , returns="The absolute value of the number passed in."
          )
  public static class Abs extends StellarMathFunction{


    public Abs() {
      super(MathOperations.ABS);
    }
  }

  @Stellar(name="LOG10"
          ,description="Returns the log (base 10) of a number."
          ,params = {
                "number - The number to take the log (base 10) value of"
                    }
          , returns="The log (base 10) of the number passed in."
          )
  public static class Log10 extends StellarMathFunction {
   public Log10() {
      super(MathOperations.LOG10);
    }

  }

  @Stellar(name="LOG2"
          ,description="Returns the log (base 2) of a number."
          ,params = {
                "number - The number to take the log (base 2) value of"
                    }
          , returns="The log (base 2) of the number passed in."
          )
  public static class Log2 extends StellarMathFunction {
   public Log2() {
      super(MathOperations.LOG2);
    }

  }

  @Stellar(name="LN"
          ,description="Returns the natural log of a number."
          ,params = {
                "number - The number to take the natural log value of"
                    }
          , returns="The natural log of the number passed in."
          )
  public static class Ln extends StellarMathFunction {
   public Ln() {
      super(MathOperations.LN);
    }

  }

  @Stellar(name="SQRT"
          ,description="Returns the square root of a number."
          ,params = {
                "number - The number to take the square root of"
                    }
          , returns="The square root of the number passed in."
          )
  public static class Sqrt extends StellarMathFunction {
   public Sqrt() {
      super(MathOperations.SQRT);
    }

  }

  @Stellar(name="CEILING"
          ,description="Returns the ceiling of a number."
          ,params = {
                "number - The number to take the ceiling of"
                    }
          , returns="The ceiling of the number passed in."
          )
  public static class Ceil extends StellarMathFunction {
   public Ceil() {
      super(MathOperations.CEIL);
    }

  }

  @Stellar(name="FLOOR"
          ,description="Returns the floor of a number."
          ,params = {
                "number - The number to take the floor of"
                    }
          , returns="The floor of the number passed in."
          )
  public static class Floor extends StellarMathFunction {
   public Floor() {
      super(MathOperations.FLOOR);
    }
  }

  @Stellar(name="SIN"
          ,description="Returns the sine of a number."
          ,params = {
                "number - The number to take the sine of"
                    }
          , returns="The sine of the number passed in."
          )
  public static class Sin extends StellarMathFunction {
   public Sin() {
      super(MathOperations.SIN);
    }
  }

  @Stellar(name="COS"
          ,description="Returns the cosine of a number."
          ,params = {
                "number - The number to take the cosine of"
                    }
          , returns="The cosine of the number passed in."
          )
  public static class Cos extends StellarMathFunction {
   public Cos() {
      super(MathOperations.COS);
    }
  }

  @Stellar(name="TAN"
          ,description="Returns the tangent of a number."
          ,params = {
                "number - The number to take the tangent of"
                    }
          , returns="The tangent of the number passed in."
          )
  public static class Tan extends StellarMathFunction {
   public Tan() {
      super(MathOperations.TAN);
    }
  }

  @Stellar(name="EXP"
          ,description="Returns Euler's number raised to the power of the argument"
          ,params = {
                "number - The power to which e is raised."
                    }
          , returns="Euler's number raised to the power of the argument."
          )
  public static class Exp extends StellarMathFunction {
   public Exp() {
      super(MathOperations.EXP);
    }
  }

  @Stellar(name="ROUND"
          ,description="Rounds a number to the nearest integer. This is half-up rounding."
          ,params = {
                "number - The number to round"
                    }
          , returns="The nearest integer (based on half-up rounding)."
          )
  public static class Round extends StellarMathFunction {
   public Round() {
      super(MathOperations.ROUND);
    }
  }

  @Stellar(name = "IS_NAN",
      description = "Evaluates if the passed number is NaN.  The number is evaluated as a double",
       params = {
        "number - number to evaluate"
       },
       returns = "True if the value is NaN, false if it is not")
  public static class IsNaN extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {

      if (args == null || args.size() != 1) {
        throw new IllegalStateException(
            "IS_NAN expects one: [number] ");
      }

      Object obj = args.get(0);
      if (obj instanceof Number) {
        return Double.isNaN(((Number) obj).doubleValue());
      } else {
        throw new ParseException("IS_NAN() expects a number argument");
      }
    }
  }

  @Stellar(namespace = "BOYERMOORE"
      , name = "ADD"
      , description = "Adds value to a Boyer-Moore list. [Boyer-Moore](https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_majority_vote_algorithm)"
      , params = {
        "state - state holder for list of values. If null, add will initialize a new state value.",
        "value(s) - single object or list of values to add to the state object."
      }
      , returns = "Current state of the Boyer-Moore algorithm representing the current value that"
      + "holds a plurality across all values added thus far."
  )
  public static class BoyerMooreAdd extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if (args.size() < 2) {
        throw new IllegalArgumentException(
            "Must pass an initial state (may be null) and at least one value to add to the list");
      } else {
        BoyerMooreState state = ConversionUtils.convert(args.get(0), BoyerMooreState.class);
        if (state == null) {
          state = new BoyerMooreState();
        }
        Object secondArg = args.get(1);
        if (secondArg instanceof List) {
          state.addAll((List) secondArg);
        } else {
          state.add(secondArg);
        }
        return state;
      }
    }
  }

  public static class BoyerMooreState {
    private Long counter;
    private Object m;

    public BoyerMooreState() {
      counter = 0L;
    }

    public BoyerMooreState(Optional<List<BoyerMooreState>> previousStates, Optional<BoyerMooreState> currentState) {
      this();
      currentState.ifPresent(boyerMooreState -> {
        m = boyerMooreState.getPlurality();
        counter = boyerMooreState.getCounter();
      });
      for (BoyerMooreState state : previousStates.orElse(new ArrayList<>())) {
        Object plurality = state.getPlurality();
        Long pluralityCount = state.getCounter();
        add(plurality, pluralityCount);
      }
    }

    public Object add(Object item) {
      if (item != null) {
        if (counter == 0) {
          m = item;
          counter = 1L;
        } else if (item.equals(m)) {
          counter++;
        } else {
          counter--;
        }
      }
      return m;
    }

    public Object add(Object item, Long counter) {
      if (item != null) {
        if (this.counter == 0) {
          m = item;
          this.counter = counter;
        } else if (item.equals(m)) {
          this.counter += counter;
        } else if (counter > this.counter) {
          m = item;
          this.counter = counter - this.counter;
        } else {
          this.counter = 0L;
        }
      }
      return m;
    }

    public Object getPlurality() {
      return m;
    }

    public Object addAll(List<Object> items) {
      for (Object item : items) {
        add(item);
      }
      return m;
    }

    public Long getCounter() {
      return counter;
    }
  }

  @Stellar(namespace = "BOYERMOORE"
      , name = "PLURALITY"
      , description = "Calculates the item with current plurality in a Boyer-Moore list. [Boyer-Moore](https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_majority_vote_algorithm)"
      , params = {
      "state - state holder for list of values. If null, add will initialize a new state value."
  }
      , returns = "The value that holds a plurality across all values added thus far."
  )
  public static class BoyerMoorePlurality extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if (args.size() != 1) {
        throw new IllegalArgumentException("Must pass a state object to return plurality for.");
      } else {
        BoyerMooreState state = ConversionUtils.convert(args.get(0), BoyerMooreState.class);
        if (state == null) {
          return null;
        } else {
          return state.getPlurality();
        }
      }
    }
  }

  @Stellar(namespace = "BOYERMOORE"
      , name = "MERGE"
      , description = "Merges states for multiple Boyer-Moore states. [Boyer-Moore](https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_majority_vote_algorithm)"
      , params = {
      "state - a list of Boyer-Moore states to merge.",
      "currentState? - The current state (optional)"
  }
      , returns = "New single merged state."
  )
  public static class BoyerMooreMerge extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      BoyerMooreState state = null;
      @SuppressWarnings("unchecked")
      List<BoyerMooreState> states = (List<BoyerMooreState>) args.get(0);
      BoyerMooreState currentState = null;
      if (args.size() > 1) {
        currentState = (BoyerMooreState) args.get(1);
      }
      state = new BoyerMooreState(Optional.ofNullable(states), Optional.ofNullable(currentState));
      return state;
    }
  }

}
