/**
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

package org.apache.metron.common.dsl.functions;

import org.apache.metron.common.dsl.BaseStellarFunction;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.stellar.LambdaExpression;

import java.util.ArrayList;
import java.util.List;

public class FunctionalFunctions {

  @Stellar(name="MAP"
          , description="Applies lambda expression to a list of arguments. e.g. MAP( [ 'foo', 'bar' ] , ( x ) -> TO_UPPER(x) ) would yield [ 'FOO', 'BAR' ]"
          , params = {
                      "list - List of arguments."
                     ,"transform_expression - The lambda expression to apply. This expression is assumed to take one argument."
                     }
          , returns = "The input list transformed item-wise by the lambda expression."
          )
  public static class Map extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      List<Object> input = (List<Object>) args.get(0);
      LambdaExpression expression = (LambdaExpression)args.get(1);
      if(input == null || expression == null) {
        return input;
      }
      List<Object> ret = new ArrayList<>();
      for(Object o : input) {
        ret.add(expression.apply(listOf(o)));
      }
      return ret;
    }
  }

  @Stellar(name="FILTER"
          , description="Applies a filter in the form of a lambda expression to a list. e.g. FILTER( [ 'foo', 'bar' ] , (x) -> x == 'foo') would yield [ 'foo']"
          , params = {
                      "list - List of arguments."
                     ,"predicate - The lambda expression to apply.  This expression is assumed to take one argument and return a boolean."
                     }
          , returns = "The input list filtered by the predicate."
          )
  public static class Filter extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      List<Object> input = (List<Object>) args.get(0);
      LambdaExpression expression = (LambdaExpression) args.get(1);
      if(input == null || expression == null) {
        return input;
      }
      List<Object> ret = new ArrayList<>();
      for(Object o : input) {
        Object result = expression.apply(listOf(o));
        if(result != null && result instanceof Boolean && (Boolean)result) {
          ret.add(o);
        }
      }
      return ret;
    }
  }

  @Stellar(name="REDUCE"
          , description="Reduces a list by a binary lambda expression. That is, the expression takes two arguments.  Usage example: REDUCE( [ 1, 2, 3 ] , (x, y) -> x + y) would sum the input list, yielding 6."
          , params = {
                      "list - List of arguments."
                     ,"binary_operation - The lambda expression function to apply to reduce the list. It is assumed that this takes two arguments, the first being the running total and the second being an item from the list."
                     ,"initial_value - The initial value to use."
                     }
          , returns = "The reduction of the list."
          )
  public static class Reduce extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      List<Object> input = (List<Object>) args.get(0);
      if(input == null || input.size() < 3) {
        return null;
      }
      LambdaExpression expression = (LambdaExpression) args.get(1);

      Object runningResult = args.get(2);
      if(expression == null || runningResult == null) {
        return null;
      }
      for(int i = 0;i < input.size();++i) {
        Object rhs = input.get(i);
        runningResult = expression.apply(listOf(runningResult, rhs));
      }
      return runningResult;
    }
  }

  private static List<Object> listOf(Object... vals) {
    List<Object> ret = new ArrayList<>(vals.length);
    for(int i = 0;i < vals.length;++i) {
      ret.add(vals[i]);
    }
    return ret;
  }
}
