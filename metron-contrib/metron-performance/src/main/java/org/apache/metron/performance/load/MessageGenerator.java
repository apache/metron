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

import org.apache.metron.performance.sampler.Sampler;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class MessageGenerator implements Supplier<String> {
  private static ThreadLocal<Random> rng = ThreadLocal.withInitial(() -> new Random());
  private static AtomicLong guidOffset = new AtomicLong(0);
  private static String guidPrefix = "00000000-0000-0000-0000-";
  private List<String> patterns;
  private Sampler sampler;
  public MessageGenerator(List<String> patterns, Sampler sampler) {
    this.patterns = patterns;
    this.sampler = sampler;
  }

  @Override
  public String get() {
    int sample = sampler.sample(rng.get(), patterns.size());
    String pattern = patterns.get(sample);
    long guidId = guidOffset.getAndIncrement();
    String guid = guidPrefix + guidId;
    String ts = "" + System.currentTimeMillis();
    return pattern.replace("$METRON_TS", ts)
                            .replace("$METRON_GUID", guid);
  }
}
