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

package org.apache.metron.stellar.dsl.functions;

import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.common.LambdaExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
      if(input == null || args.size() < 3) {
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

  @Stellar(name="ZIP_LONGEST"
          , description="Zips lists into a single list where the ith element is an list " +
          "containing the ith items from the constituent lists.  " +
          "See [python](https://docs.python.org/3/library/itertools.html#itertools.zip_longest) " +
          "and [wikipedia](https://en.wikipedia.org/wiki/Convolution_(computer_science)) for more context."
          , params = {
                      "list(s) - List(s) to zip."
                     }
          , returns = "The zip of the lists.  The returned list is the max size of all the lists.  " +
          "Empty elements are null " +
          "e.g. ZIP_LONGEST( [ 1, 2 ], [ 3, 4, 5] ) == [ [1, 3], [2, 4], [null, 5] ]"
          )
  public static class LongestZip extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if(args == null || args.size() == 0) {
        return new ArrayList<>();
      }
      return zip(args, true);
    }
  }

  @Stellar(name="ZIP"
          , description="Zips lists into a single list where the ith element is an list containing the ith items from the constituent lists. " +
          "See [python](https://docs.python.org/3/library/functions.html#zip) and [wikipedia](https://en.wikipedia.org/wiki/Convolution_(computer_science)) for more context."
          , params = {
                      "list(s) - List(s) to zip."
                     }
          ,returns = "The zip of the lists.  The returned list is the min size of all the lists.  " +
          "e.g. ZIP( [ 1, 2 ], [ 3, 4, 5] ) == [ [1, 3], [2, 4] ]"
          )
  public static class Zip extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if(args == null || args.size() == 0) {
        return new ArrayList<>();
      }
      return zip(args, false);
    }
  }

  private static List<List<Object>> zip(List<Object> args, boolean jagged) {
    List<List<Object>> lists = new ArrayList<>();
    Integer resultSize = null;
    for(Object o : args) {
      if(o instanceof List) {
        List<Object> l = (List<Object>)o;
        if( resultSize == null) {
          resultSize = l.size();
        }
        else if(jagged) {
          resultSize = Math.max(l.size(), resultSize);
        }
        else {
          resultSize = Math.min(l.size(), resultSize);
        }
        lists.add(l);
      }
    }
    if(resultSize == null) {
      return new ArrayList<>();
    }

    return IntStream.range(0, resultSize)
            .mapToObj(i -> {
              List<Object> o = new ArrayList<>();
              for(List<Object> list : lists) {
                o.add( i < list.size() ? list.get(i): null);
              }
              return o;
            })
            .collect(Collectors.toList());
  }

  private static List<Object> listOf(Object... vals) {
    List<Object> ret = new ArrayList<>(vals.length);
    for(int i = 0;i < vals.length;++i) {
      ret.add(vals[i]);
    }
    return ret;
  }
}
