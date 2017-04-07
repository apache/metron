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

import com.google.common.collect.ImmutableList;
import org.apache.metron.common.dsl.BaseStellarFunction;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.stellar.ReferencedExpression;

import java.util.ArrayList;
import java.util.List;

public class FunctionalFunctions {
  @Stellar(name="MAP"
          , description="Applies a function to a list of arguments. e.g. MAP( [ 'foo', 'bar' ] , &TO_UPPER($0)) would yield [ 'FOO', 'BAR' ]"
          , params = {
                      "list - List of arguments."
                     ,"function - The referenced function to apply.  Note $0 would be a variable which refers to the item being processed. (e.g. &TO_UPPER($0) )"
                     }
          , returns = "A list of the function applied to each element."
          )
  public static class Map extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      List<Object> input = (List<Object>) args.get(0);
      ReferencedExpression expression = (ReferencedExpression)args.get(1);
      if(input == null || expression == null) {
        return input;
      }
      List<Object> ret = new ArrayList<>();
      for(Object o : input) {
        if(o != null) {
          ret.add(expression.apply(ImmutableList.of(o)));
        }
      }
      return ret;
    }
  }

  @Stellar(name="FILTER"
          , description="Applies a filter to a list. e.g. FILTER( [ 'foo', 'bar' ] , &($0 == 'foo')) would yield [ 'foo']"
          , params = {
                      "list - List of arguments."
                     ,"predicate - The referenced function to apply.  This function is assumed to return a boolean.  " +
                      "Note $0 would be a variable which refers to the item being processed. (e.g. &($0 == 'foo') )"
                     }
          , returns = "A list of the function applied to each element."
          )
  public static class Filter extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      List<Object> input = (List<Object>) args.get(0);
      ReferencedExpression expression = (ReferencedExpression) args.get(1);
      if(input == null || expression == null) {
        return input;
      }
      List<Object> ret = new ArrayList<>();
      for(Object o : input) {
        if(o == null) {
          continue;
        }
        Object result = expression.apply(ImmutableList.of(o));
        if(result != null && result instanceof Boolean && (Boolean)result) {
          ret.add(o);
        }
      }
      return ret;
    }
  }

  @Stellar(name="REDUCE"
          , description="Reduces a list by a binary operation. e.g. REDUCE( [ 1, 2, 3 ] , &($0 + $1)) would yield 6"
          , params = {
                      "list - List of arguments."
                     ,"binary_operation - The referenced function to apply to reduce the list. " +
                      "Note $0 would be a variable which refers to the running result and $1 the item."
                     ,"initial_value - The initial value"
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
      ReferencedExpression expression = (ReferencedExpression) args.get(1);

      Object runningResult = args.get(2);
      if(expression == null || runningResult == null) {
        return null;
      }
      for(int i = 0;i < input.size();++i) {
        Object rhs = input.get(i);
        if(rhs == null) {
          continue;
        }
        runningResult = expression.apply(ImmutableList.of(runningResult, rhs));
      }
      return runningResult;
    }
  }
}
