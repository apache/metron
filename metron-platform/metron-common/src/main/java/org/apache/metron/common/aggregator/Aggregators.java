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

import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

public enum Aggregators implements Aggregator {
   MAX( (numbers, config) -> accumulate(0d, (x,y) -> Math.max(x.doubleValue(),y.doubleValue()), numbers))
  ,MIN( (numbers, config) -> accumulate(0d, (x,y) -> Math.min(x.doubleValue(),y.doubleValue()), numbers))
  ,SUM( (numbers, config) -> accumulate(0d, (x,y) -> x.doubleValue() + y.doubleValue(), numbers))
  ,MEAN( (numbers, config) -> scale(SUM.aggregate(numbers, config), numbers, n -> true))
  ,POSITIVE_MEAN( (numbers, config) -> scale(SUM.aggregate(numbers, config), numbers, n -> n.doubleValue() > 0))
  ;
  Aggregator aggregator;
  Aggregators(Aggregator agg) {
    aggregator = agg;
  }
  public Aggregator getAggregator() {
    return aggregator;
  }

  private static double accumulate(double initial, BinaryOperator<Number> op, List<Number> list) {
    if(list.isEmpty()) {
      return 0d;
    }
    return list.stream()
               .reduce(initial, op)
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
