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

package org.apache.metron.indexing.dao.metaalert;

import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaScores {

  protected Map<String, Object> metaScores = new HashMap<>();

  public MetaScores(List<Double> scores) {
    // A meta alert could be entirely alerts with no values.
    DoubleSummaryStatistics stats = scores
        .stream()
        .mapToDouble(a -> a)
        .summaryStatistics();
    metaScores.put("max", stats.getMax());
    metaScores.put("min", stats.getMin());
    metaScores.put("average", stats.getAverage());
    metaScores.put("count", stats.getCount());
    metaScores.put("sum", stats.getSum());

    // median isn't in the stats summary
    double[] arr = scores
        .stream()
        .mapToDouble(d -> d)
        .toArray();
    metaScores.put("median", new Median().evaluate(arr));
  }

  public Map<String, Object> getMetaScores() {
    return metaScores;
  }
}
