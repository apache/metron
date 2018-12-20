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

import com.google.common.collect.Iterables;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

import java.util.List;
import java.util.function.BiFunction;

import static org.apache.metron.stellar.common.utils.ConversionUtils.convert;


public class OrdinalFunctions {

  /**
   * Stellar Function: MAX
   * <p>
   * Return the maximum value of a list of input values in a Stellar list
   */
  @Stellar(name = "MAX"
          , description = "Returns the maximum value of a list of input values or from a statistics object"
          , params = {"stats - The Stellar statistics object"
          ,"list - List of arguments. The list may only contain objects that are mutually comparable / ordinal (implement java.lang.Comparable interface)" +
          " Multi type numeric comparisons are supported: MAX([10,15L,15.3]) would return 15.3, but MAX(['23',25]) will fail and return null as strings and numbers can't be compared."}
          , returns = "The maximum value in the list or from stats, or null if the list is empty or the input values were not comparable.")
  public static class Max extends BaseStellarFunction {

    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> args) {
      if (args.size() < 1 || args.get(0) == null) {
        throw new IllegalStateException("MAX function requires at least one argument");
      }
      Object firstArg = args.get(0);
      if (firstArg instanceof Ordinal) {
        Ordinal stats = convert(firstArg, Ordinal.class);
        return stats.getMax();
      } else if (firstArg instanceof Iterable) {
        Iterable<Comparable> list = (Iterable<Comparable>) args.get(0);
        return orderList(list, (ret, val) -> ret.compareTo(val) < 0, "MAX");
      } else {
        throw new IllegalStateException("MAX function expects either 'a StatisticsProvider object' or 'Stellar list of values'");
      }

    }
  }

  /**
   * Stellar Function: MIN.
   *
   * <p>
   * Return the minimum value of a list of input values in a Stellar list
   * </p>
   */
  @Stellar(name = "MIN",
          description = "Returns the minimum value of a list of input values",
          params = {"stats - The Stellar statistics object",
                  "list - List of arguments. The list may only contain objects that are mutually comparable "
                  + "/ ordinal (implement java.lang.Comparable interface)"
                  + " Multi type numeric comparisons are supported: MIN([10,15L,15.3]) would return 10,"
                  + "but MIN(['23',25]) will fail and return null as strings and numbers can't be compared."},
          returns = "The minimum value in the list or from stats, or null if the list is empty or the input values"
                  + " were not comparable.")
  public static class Min extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> args) {
      if (args.size() < 1 || args.get(0) == null) {
        throw new IllegalStateException("MIN function requires at least one argument");
      }
      Object firstArg = args.get(0);
      if (firstArg instanceof Ordinal) {
        Ordinal stats = convert(firstArg, Ordinal.class);
        return stats.getMin();
      } else if (firstArg instanceof Iterable) {
        Iterable<Comparable> list = (Iterable<Comparable>) args.get(0);
        return orderList(list, (ret, val) -> ret.compareTo(val) > 0, "MIN");
      } else {
        throw new IllegalStateException("MIN function expects either 'a StatisticsProvider object' or 'Stellar list of values' ");
      }
    }
  }

  private static Comparable orderList(Iterable<Comparable> list, BiFunction<Comparable, Comparable, Boolean> eval, String funcName) {
    if (Iterables.isEmpty(list)) {
      return null;
    }
    Object o = Iterables.getFirst(list, null);
    Comparable ret = null;
    for(Object valueVal : list) {
      if(valueVal == null) {
        continue;
      }
      Comparable value = null;
      if(!(valueVal instanceof Comparable)) {
        throw new IllegalStateException("Noncomparable object type " + valueVal.getClass().getName()
                + " submitted to " + funcName);
      }
      else {
        value = (Comparable)valueVal;
      }
      try {
        Comparable convertedRet = ConversionUtils.convert(ret, value.getClass());
        if(convertedRet == null && ret != null) {
          throw new IllegalStateException("Incomparable objects were submitted to " + funcName
                  + ": " + ret.getClass() + " is incomparable to " + value.getClass());
        }
        if(ret == null || eval.apply(convertedRet, value) ) {
          ret = value;
        }
      }
      catch(ClassCastException cce) {
        throw new IllegalStateException("Incomparable objects were submitted to " + funcName
                + ": " + cce.getMessage(), cce);
      }
    }
    return ret;
  }

}
