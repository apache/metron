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
import org.apache.metron.performance.load.monitor.MonitorNaming;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class Writer {

  public static class Results {
    MonitorNaming monitor;
    Optional<DescriptiveStatistics> history;
    Long eps;
    Date dateOf;
    Results(MonitorNaming monitor, Long eps, Optional<DescriptiveStatistics> history, Date dateOf) {
      this.monitor = monitor;
      this.history = history;
      this.dateOf = dateOf;
      this.eps = eps;
    }
    public Long getEps() {
      return eps;
    }

    public Date getDateOf() {
      return dateOf;
    }

    public MonitorNaming getMonitor() {
      return monitor;
    }

    public Optional<DescriptiveStatistics> getHistory() {
      return history;
    }
  }
  private int summaryLookback;
  private List<LinkedList<Double>> summaries = new ArrayList<>();
  private List<Consumer<List<Results>>> writers;
  private List<AbstractMonitor> monitors;

  public Writer(List<AbstractMonitor> monitors, int summaryLookback, List<Consumer<List<Results>>> writers) {
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
      if(eps != null) {
        if (summaryLookback > 0) {
          LinkedList<Double> summary = summaries.get(i++);
          addToLookback(eps == null ? Double.NaN : eps.doubleValue(), summary);
          results.add(new Results(m, eps, Optional.of(getStats(summary)), dateOf));
        }
        else {
          results.add(new Results(m, eps, Optional.empty(), dateOf));
        }
      }
      else {
        results.add(new Results(m, eps, Optional.empty(), dateOf));
      }
    }
    for(Consumer<List<Results>> writer : writers) {
      writer.accept(results);
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
    //return String.format("Mean: %d, Std Dev: %d", (int)stats.getMean(), (int)Math.sqrt(stats.getVariance()));
  }
}
