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
package org.apache.metron.stellar.common.benchmark;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.common.StellarProcessor;

import java.util.function.Consumer;

public class Microbenchmark {

  public static class StellarStatement {
    String expression;
    VariableResolver variableResolver;
    FunctionResolver functionResolver;
    Context context;
  }

  public static DescriptiveStatistics run(StellarStatement statement, int warmupRounds, int benchmarkRounds )
  {
    run(warmupRounds, statement, ts -> {});
    final DescriptiveStatistics stats = new DescriptiveStatistics();
    run(benchmarkRounds, statement, ts -> { stats.addValue(ts);});
    return stats;
  }

  private static void run(int numTimes, StellarStatement statement, Consumer<Long> func) {
    StellarProcessor processor = new StellarProcessor();
    for(int i = 0;i < numTimes;++i) {
      long start = System.nanoTime();
      processor.parse(statement.expression, statement.variableResolver, statement.functionResolver, statement.context);
      func.accept((System.nanoTime() - start)/1000);
    }
  }

  public static String describe(DescriptiveStatistics stats, Double[] percentiles){
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("round: mean of %dms [+-%d], measured %d rounds;\n",
            (long)stats.getMean(),
            (long)stats.getStandardDeviation(), stats.getN() ));
    sb.append("\tMin - " + (long)stats.getMin() + "\n");
    for(double pctile : percentiles) {
      sb.append("\t" + pctile + " - " + stats.getPercentile(pctile) + "\n");
    }
    sb.append("\tMax - " + (long)stats.getMax());
    return sb.toString();
  }

}
