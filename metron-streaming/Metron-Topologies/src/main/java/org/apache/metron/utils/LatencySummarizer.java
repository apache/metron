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
package org.apache.metron.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.cli.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.*;

public class LatencySummarizer {
  public static class Pair extends AbstractMap.SimpleEntry<String, String> {
    public Pair(String key, String value) {
      super(key, value);
    }
  }

  public static class LatencyStats {
    private NavigableMap<Integer, Map<Pair, DescriptiveStatistics>> depthMap = new TreeMap<>();
    private List<String> metrics;
    public void updateMetrics(List<String> metrics) {
      this.metrics = metrics;
    }
    public Map<Pair, DescriptiveStatistics> getStatsMap(int depth) {
      Map<Pair, DescriptiveStatistics> statsMap = depthMap.get(depth);
      if(statsMap == null) {
        statsMap = new HashMap<>();
        depthMap.put(depth, statsMap);
      }
      return statsMap;
    }
    public DescriptiveStatistics getStats( int depth, Pair p) {
      Map<Pair, DescriptiveStatistics> statsMap = getStatsMap(depth);
      DescriptiveStatistics stats = statsMap.get(p);
      if(stats == null) {
        stats = new DescriptiveStatistics();
        statsMap.put(p, stats);
      }
      return stats;
    }
    public void put(int depth, Pair p, double val) {
      getStats(depth, p).addValue(val);
    }

    public static void summary(String title, DescriptiveStatistics statistics, PrintStream pw, boolean meanOnly) {
      if(meanOnly) {
        pw.println(title + ": "
                + "\n\tMean: " + statistics.getMean()
        );
      }
      else {
        pw.println(title + ": "
                + "\n\tMean: " + statistics.getMean()
                + "\n\tMin: " + statistics.getMin()
                + "\n\t1th: " + statistics.getPercentile(1)
                + "\n\t5th: " + statistics.getPercentile(5)
                + "\n\t10th: " + statistics.getPercentile(10)
                + "\n\t25th: " + statistics.getPercentile(25)
                + "\n\t50th: " + statistics.getPercentile(50)
                + "\n\t90th: " + statistics.getPercentile(90)
                + "\n\t95th: " + statistics.getPercentile(95)
                + "\n\t99th: " + statistics.getPercentile(99)
                + "\n\tMax: " + statistics.getMax()
                + "\n\tStdDev: " + statistics.getStandardDeviation()
        );
      }
    }
    public void printDepthSummary(int depth, boolean meanOnly) {
      Map<Pair, DescriptiveStatistics> statsMap = depthMap.get(depth);
      System.out.println("\nDistance " + depth);
      System.out.println("----------------\n");
      List<Map.Entry<Pair, DescriptiveStatistics>> sortedStats = new ArrayList<>();
      for(Map.Entry<Pair, DescriptiveStatistics> stats : statsMap.entrySet()) {
        sortedStats.add(stats);
      }
      Collections.sort(sortedStats, new Comparator<Map.Entry<Pair, DescriptiveStatistics>>() {
        @Override
        public int compare(Map.Entry<Pair, DescriptiveStatistics> o1, Map.Entry<Pair, DescriptiveStatistics> o2) {
          return -1*Double.compare(o1.getValue().getMean(), o2.getValue().getMean());
        }
      });
      for(Map.Entry<Pair, DescriptiveStatistics> stats : sortedStats) {
        summary(stats.getKey().getKey() + " -> " + stats.getKey().getValue(), stats.getValue(), System.out, meanOnly);
      }
    }
    public void printSummary(boolean meanOnly) {
      System.out.println("Flow:");
      System.out.println("\t" + Joiner.on(" -> ").join(metrics));
      System.out.println("\nSUMMARY BY DISTANCE\n--------------------------");
      for(int depth : depthMap.keySet()) {
        printDepthSummary(depth, meanOnly);
      }
    }

  }

  public static String getBaseMetric(String s) {
    Iterable<String> tokenIt = Splitter.on('.').split(s);
    int num = Iterables.size(tokenIt);
    return Joiner.on('.').join(Iterables.limit(tokenIt, num-1));
  }

  public static void updateStats(LatencyStats stats, Map<String, Object> doc) {
    Map<String, Long> latencyMap = new HashMap<>();
    NavigableMap<Long, String> latencyInvMap = new TreeMap<>();
    for(Map.Entry<String, Object> kv : doc.entrySet()) {
      if(kv.getKey().endsWith(".ts")) {
        String base = getBaseMetric(kv.getKey());
        long latency = Long.parseLong(kv.getValue().toString());
        latencyInvMap.put(latency, base);
        latencyMap.put( base, latency);
      }
    }
    List<String> metrics = new ArrayList<>();
    for(Map.Entry<Long, String> kv : latencyInvMap.entrySet()) {
      metrics.add(kv.getValue());
    }
    stats.updateMetrics(metrics);
    for(int i = 0;i < metrics.size();++i) {
      for(int j = i+1;j < metrics.size();++j) {
        Pair p = new Pair(metrics.get(i), metrics.get(j));
        long ms = latencyMap.get(metrics.get(j)) - latencyMap.get(metrics.get(i));
        stats.put(j-i, p, ms);
      }
    }
  }



  public static void main(String... argv) throws IOException {
    Options options = new Options();
    {
      Option o = new Option("h", "help", false, "This screen");
      o.setRequired(false);
      options.addOption(o);
    }
    {
      Option o = new Option("m", "mean_only", false, "Print the mean only when we summarize");
      o.setRequired(false);
      options.addOption(o);
    }
    CommandLineParser parser = new PosixParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, argv);
    }
    catch(ParseException pe) {
      pe.printStackTrace();
      final HelpFormatter usageFormatter = new HelpFormatter();
      usageFormatter.printHelp(LatencySummarizer.class.getSimpleName().toLowerCase(), null, options, null, true);
      System.exit(-1);
    }
    if( cmd.hasOption("h") ){
      final HelpFormatter usageFormatter = new HelpFormatter();
      usageFormatter.printHelp(LatencySummarizer.class.getSimpleName().toLowerCase(), null, options, null, true);
      System.exit(0);
    }
    LatencyStats statsMap = new LatencyStats();
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    for(String line = null;(line = reader.readLine()) != null;) {
      Map<String, Object> doc = JSONUtils.INSTANCE.load(line, new TypeReference<HashMap<String, Object>>() {});
      updateStats(statsMap, doc);
    }
    statsMap.printSummary(cmd.hasOption('m'));
  }
}
