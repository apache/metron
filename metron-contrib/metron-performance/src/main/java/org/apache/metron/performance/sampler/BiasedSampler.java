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
package org.apache.metron.performance.sampler;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class BiasedSampler implements Sampler {
  TreeMap<Double, Map.Entry<Integer, Integer>> discreteDistribution;
  public BiasedSampler(List<Map.Entry<Integer, Integer>>  discreteDistribution, int max) {
    this.discreteDistribution = createDistribution(discreteDistribution, max);
  }

  public static List<Map.Entry<Integer, Integer>> readDistribution(BufferedReader distrFile) throws IOException {
    return readDistribution(distrFile, false);
  }

  public static List<Map.Entry<Integer, Integer>> readDistribution(BufferedReader distrFile, boolean quiet) throws IOException {
    List<Map.Entry<Integer, Integer>> ret = new ArrayList<>();
    if(!quiet) {
      System.out.println("Using biased sampler with the following biases:");
    }
    int sumLeft = 0;
    int sumRight = 0;
    for(String line = null;(line = distrFile.readLine()) != null;) {
      if(line.startsWith("#")) {
        continue;
      }
      Iterable<String> it = Splitter.on(",").split(line.trim());
      if(Iterables.size(it) != 2) {
        throw new IllegalArgumentException(line + " should be a comma separated pair of integers, but was not.");
      }
      int left = Integer.parseInt(Iterables.getFirst(it, null));
      int right = Integer.parseInt(Iterables.getLast(it, null));
      if(left <= 0 || left > 100) {
        throw new IllegalArgumentException(line + ": " + (left < 0?left:right) + " must a positive integer in (0, 100]");
      }
      if(right <= 0 || right > 100) {
        throw new IllegalArgumentException(line + ": " + right + " must a positive integer in (0, 100]");
      }
      if(!quiet) {
        System.out.println("\t" + left + "% of templates will comprise roughly " + right + "% of sample output");
      }
      ret.add(new AbstractMap.SimpleEntry<>(left, right));
      sumLeft += left;
      sumRight += right;
    }
    if(sumLeft > 100 || sumRight > 100 ) {
      throw new IllegalStateException("Neither columns must sum to beyond 100.  " +
              "The first column is the % of templates. " +
              "The second column is the % of the sample that % of template occupies.");
    }
    else if(sumLeft < 100 && sumRight < 100) {
      int left = 100 - sumLeft;
      int right = 100 - sumRight;
      if(!quiet) {
        System.out.println("\t" + left + "% of templates will comprise roughly " + right + "% of sample output");
      }
      ret.add(new AbstractMap.SimpleEntry<>(left, right));
    }
    return ret;

  }

  private static TreeMap<Double, Map.Entry<Integer, Integer>>
                 createDistribution(List<Map.Entry<Integer, Integer>>  discreteDistribution, int max) {
    TreeMap<Double, Map.Entry<Integer, Integer>> ret = new TreeMap<>();
    int from = 0;
    double weight = 0.0d;
    for(Map.Entry<Integer, Integer> kv : discreteDistribution) {
      double pctVals = kv.getKey()/100.0;
      int to = from + (int)(max*pctVals);
      double pctWeight = kv.getValue()/100.0;
      ret.put(weight, new AbstractMap.SimpleEntry<>(from, to));
      weight += pctWeight;
      from = to;
    }
    return ret;
  }

  @Override
  public int sample(Random rng, int limit) {
    double weight = rng.nextDouble();
    Map.Entry<Integer, Integer> range = discreteDistribution.floorEntry(weight).getValue();
    return rng.nextInt(range.getValue() - range.getKey()) + range.getKey();
  }
}
