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
package org.apache.metron.performance.load.monitor.writers;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.metron.performance.load.monitor.AbstractMonitor;
import org.apache.metron.performance.load.monitor.Results;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class Writer {

  private int summaryLookback;
  private List<LinkedList<Double>> summaries = new ArrayList<>();
  private List<Consumer<Writable>> writers;
  private List<AbstractMonitor> monitors;

  public Writer(List<AbstractMonitor> monitors, int summaryLookback, List<Consumer<Writable>> writers) {
    this.summaryLookback = summaryLookback;
    this.writers = writers;
    this.monitors = monitors;
    for(AbstractMonitor m : monitors) {
      this.summaries.add(new LinkedList<>());
    }
  }

  public void writeAll() {
    int i = 0;
    Date dateOf = new Date();
    List<Results> results = new ArrayList<>();
    for(AbstractMonitor m : monitors) {
      Long eps = m.get();
      if(eps != null && summaryLookback > 0) {
          LinkedList<Double> summary = summaries.get(i);
          addToLookback(eps.doubleValue(), summary);
          results.add(new Results(m.format(), m.name(), eps, Optional.of(getStats(summary))));
      }
      else {
        results.add(new Results(m.format(), m.name(), eps, Optional.empty()));
      }
      i++;
    }
    Writable writable = new Writable(dateOf, results);
    for(Consumer<Writable> writer : writers) {
      writer.accept(writable);
    }
  }

  private void addToLookback(Double d, LinkedList<Double> lookback) {
    if(lookback.size() >= summaryLookback) {
      lookback.removeFirst();
    }
    lookback.addLast(d);
  }

  public DescriptiveStatistics getStats(List<Double> avg) {
    DescriptiveStatistics stats = new DescriptiveStatistics();
    for(Double d : avg) {
      if(d == null || Double.isNaN(d)) {
        continue;
      }
      stats.addValue(d);
    }
    return stats;
  }
}
