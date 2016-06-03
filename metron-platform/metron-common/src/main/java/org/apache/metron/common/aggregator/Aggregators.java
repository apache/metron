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

package org.apache.metron.common.aggregator;

import org.apache.metron.common.utils.ConversionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

public enum Aggregators implements Aggregator {
   MAX( (numbers, config) -> accumulate(0d, (x,y) -> Math.max(x.doubleValue(),y.doubleValue()), numbers, config))
  ,MIN( (numbers, config) -> accumulate(0d, (x,y) -> Math.min(x.doubleValue(),y.doubleValue()), numbers, config))
  ,SUM( (numbers, config) -> accumulate(0d, (x,y) -> x.doubleValue() + y.doubleValue(), numbers, config))
  ,MEAN( (numbers, config) -> scale(SUM.aggregate(numbers, config), numbers, n -> true))
  ,POSITIVE_MEAN( (numbers, config) -> positiveMean(numbers, config))
  ;
  public static String NEGATIVE_VALUES_TRUMP_CONF = "negativeValuesTrump";
  Aggregator aggregator;
  Aggregators(Aggregator agg) {
    aggregator = agg;
  }
  public Aggregator getAggregator() {
    return aggregator;
  }

  private static double positiveMean(List<Number> list, Map<String, Object> config) {
    Double ret = 0d;
    int num = 0;
    boolean negValuesTrump = doNegativeValuesTrump(config);
    for(Number n : list) {
      if(n.doubleValue() < 0) {
        if(negValuesTrump) {
          return Double.NEGATIVE_INFINITY;
        }
      }
      else if(n.doubleValue() > 0) {
        ret += n.doubleValue();
        num++;
      }
    }
    return num > 0?ret/num:0d;

  }

  private static boolean doNegativeValuesTrump(Map<String, Object> config) {
    boolean negativeValuesTrump = true;
    Object negValuesObj = config.get(NEGATIVE_VALUES_TRUMP_CONF);
    if(negValuesObj != null)
    {
      Boolean b = ConversionUtils.convert(negValuesObj, Boolean.class);
      if(b != null) {
        negativeValuesTrump = b;
      }
    }
    return negativeValuesTrump;
  }
  private static double accumulate(double initial, BinaryOperator<Number> op, List<Number> list, Map<String, Object> config) {
    if(list.isEmpty()) {
      return 0d;
    }
    boolean negativeValuesTrump = doNegativeValuesTrump(config);

    BinaryOperator<Number> binOp = op;
    if(negativeValuesTrump) {
      binOp =(x,y) -> {
        if (y.doubleValue() < 0 || x.doubleValue() < 0) {
          return Double.NEGATIVE_INFINITY;
        } else {
          return op.apply(x, y);
        }
      };
    }
    return list.stream()
               .reduce(initial, binOp)
               .doubleValue();
  }

  private static double scale(double numberToScale, List<Number> list, Predicate<Number> filterFunc) {
    double scale = list.stream().filter(filterFunc).count();
    if(scale < 1e-5) {
      scale = 1;
    }
    return numberToScale / scale;
  }

  @Override
  public Double aggregate(List<Number> scores, Map<String, Object> config) {
    return aggregator.aggregate(scores, config);
  }
}
