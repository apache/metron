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
package org.apache.metron.performance.load;

import com.google.common.base.Joiner;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

public class MonitorTask extends TimerTask {
  private List<AbstractMonitor> monitors;
  private List<LinkedList<Double>> summaries = new ArrayList<>();
  private int summaryLookback;
  public MonitorTask(List<AbstractMonitor> monitors, int summaryLookback) {
    this.monitors = monitors;
    this.summaryLookback = summaryLookback;
    for(AbstractMonitor m : monitors) {
      this.summaries.add(new LinkedList<>());
    }
  }

  private void addToLookback(Double d, LinkedList<Double> lookback) {
    if(lookback.size() >= summaryLookback) {
      lookback.removeFirst();
    }
    lookback.addLast(d);
  }

  public String getSummary(List<Double> avg) {
    DescriptiveStatistics stats = new DescriptiveStatistics();
    for(Double d : avg) {
      if(d == null || Double.isNaN(d)) {
        continue;
      }
      stats.addValue(d);
    }
    return String.format("Mean: %d, Std Dev: %d", (int)stats.getMean(), (int)Math.sqrt(stats.getVariance()));
  }

  /**
   * The action to be performed by this timer task.
   */
  @Override
  public void run() {
    List<String> parts = new ArrayList<>();
    int i = 0;
    for(AbstractMonitor m : monitors) {
      Long eps = m.get();
      if(eps != null) {
        String part = String.format(m.format(), eps);
        if (summaryLookback > 0) {
          LinkedList<Double> summary = summaries.get(i++);
          addToLookback(eps == null ? Double.NaN : eps.doubleValue(), summary);
          part += " (" + getSummary(summary) + ")";
        }
        parts.add(part);
      }
    }
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    String header = dateFormat.format(date) + " - ";
    String emptyHeader = "";
    for(i = 0;i < header.length();++i) {
      emptyHeader += " ";
    }
    for(i = 0;i < parts.size();++i) {
      String part = parts.get(i);
      if(i == 0) {
        System.out.println(header + (part == null?"":part));
      }
      else {
        System.out.println(emptyHeader + (part == null?"":part));
      }
    }
  }
}
