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
package org.apache.metron.common.writer;

import backtype.storm.tuple.Tuple;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.apache.metron.common.utils.ConversionUtils;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class NoopWriter extends AbstractWriter implements BulkMessageWriter<Object> {

  public static class RandomLatency implements Function<Void, Void> {
    private int min;
    private int max;

    public RandomLatency(int min, int max) {
      this.min = min;
      this.max = max;
    }

    public int getMin() {
      return min;
    }
    public int getMax() {
      return max;
    }

    @Override
    public Void apply(Void aVoid) {
      int sleepMs = ThreadLocalRandom.current().nextInt(min, max + 1);
      try {
        Thread.sleep(sleepMs);
      } catch (InterruptedException e) {
      }
      return null;
    }
  }

  public static class FixedLatency implements Function<Void, Void> {
    private int latency;
    public FixedLatency(int latency) {
      this.latency = latency;
    }
    public int getLatency() {
      return latency;
    }

    @Override
    public Void apply(Void aVoid) {
      if(latency > 0) {
        try {
          Thread.sleep(latency);
        } catch (InterruptedException e) {
        }
      }
      return null;
    }
  }
  Function<Void, Void> sleepFunction = null;

  public NoopWriter withLatency(String sleepConfig) {
    sleepFunction = getSleepFunction(sleepConfig);
    return this;
  }


  private Function<Void, Void> getSleepFunction(String sleepConfig) {
    String usageMessage = "Unexpected: " + sleepConfig + " Expected value: integer for a fixed sleep duration in milliseconds (e.g. 10) " +
            "or a range of latencies separated by a comma (e.g. \"10, 20\") to sleep a random amount in that range.";
    try {
      if (sleepConfig.contains(",")) {
        // random latency within a range.
        Iterable<String> it = Splitter.on(',').split(sleepConfig);
        Integer min = ConversionUtils.convert(Iterables.getFirst(it, "").trim(), Integer.class);
        Integer max= ConversionUtils.convert(Iterables.getLast(it, "").trim(), Integer.class);
        if (min != null && max != null) {
          return new RandomLatency(min, max);
        }
      } else {
        //fixed latency
        Integer latency = ConversionUtils.convert(sleepConfig.trim(), Integer.class);
        if(latency != null) {
          return new FixedLatency(latency);
        }
      }
    }
    catch(Throwable t) {
      throw new IllegalArgumentException(usageMessage, t);
    }
    throw new IllegalArgumentException(usageMessage);
  }

  @Override
  public void configure(String sensorName, WriterConfiguration configuration) {
    Map<String, Object> config = configuration.getSensorConfig(sensorName);
    if(config != null) {
      Object noopLatency = config.get("noopLatency");
      if(noopLatency != null) {
        sleepFunction = getSleepFunction(noopLatency.toString());
      }
    }
  }

  @Override
  public void init(Map stormConf, WriterConfiguration config) throws Exception {
  }

  @Override
  public void write(String sensorType, WriterConfiguration configurations, Iterable<Tuple> tuples, List<Object> messages) throws Exception {
    if(sleepFunction != null) {
      sleepFunction.apply(null);
    }
  }

  @Override
  public void close() throws Exception {

  }
}
