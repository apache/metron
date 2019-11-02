package org.apache.metron.common.utils;

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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Performance test for lazy logging.
 * By default the test is disabled due to it taking too much time to run during normal unit testing.
 * comment out the ignore attribute to allow test to run
 */
public class LazzyLoggerImplPerfTest {
  private static final LazyLogger LOG = LazyLoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final double NANO_TO_MILLIS = 1e6;
  private static final String PATH = "/foo/bar/baz";

  public interface Operation {
    void run();
  }

  @Test
  @Disabled
  public void calcTimes() {
    Map<String, Object> smallMap = new HashMap<>();
    for (int i = 0; i < 10; i++) {
      smallMap.put("key" + i, RandomStringUtils.randomAlphabetic(10));
    }
    Map<String, Object> largeMap = new HashMap<>();
    for (int i = 0; i < 500; i++) {
      largeMap.put("key" + i, RandomStringUtils.randomAlphabetic(1000));
    }
    JSONObject largeObject = new JSONObject(largeMap);
    JSONObject smallObject = new JSONObject(smallMap);
    int reps = 1000;

    StatisticalSummary summary = runTrial(reps, () -> {
      LOG.trace("Writing message {} to path: {}", smallObject.toJSONString(), PATH);
    });
    printSummary(String.format("Small object %s times", reps), summary);

    summary = runTrial(reps, () -> {
      LOG.trace("Writing message {} to path: {}", () -> smallObject.toJSONString(), () -> PATH);
    });
    printSummary(String.format("Small object %s times using lazy logging", reps), summary);


    summary = runTrial(reps, () -> {
      LOG.trace("Writing message {} to path: {}", largeObject.toJSONString(), PATH);
    });
    printSummary(String.format("Large object %s times", reps), summary);

    summary = runTrial(reps, () -> {
      LOG.trace("Writing message {} to path: {}", () -> largeObject.toJSONString(), () -> PATH);
    });
    printSummary(String.format("Large object %s times using lazy logging", reps), summary);

    summary = runTrial(reps, () -> {
      LOG.trace("Writing message {} to path: {}", "hello", PATH);
    });
    printSummary(String.format("Simple string %s times", reps), summary);

    summary = runTrial(reps, () -> {
      LOG.trace("Writing message {} to path: {}", () -> "hello", () -> PATH);
    });
    printSummary(String.format("Simple string %s times using lazy logging", reps), summary);
  }


  private StatisticalSummary runTrial(int reps, Operation operation) {
    DescriptiveStatistics stats = new DescriptiveStatistics();
    long trialTime = timeOperation(() -> {
      for (int i = 0; i < reps; i++) {
        long time = timeOperation(operation);
        stats.addValue(time / NANO_TO_MILLIS);
      }
    });
    System.out.println("Total trial time (ms): " + (trialTime / NANO_TO_MILLIS));
    return stats;
  }

  private long timeOperation(Operation o) {
    final long start = System.nanoTime();
    o.run();
    final long finish = System.nanoTime();
    return finish - start;
  }

  private void printSummary(String desc, StatisticalSummary summary) {
    final String border = "===============================";
    System.out.println(border);
    System.out.println(desc);
    System.out.println(summary.toString());
    System.out.println(border);
  }
}
