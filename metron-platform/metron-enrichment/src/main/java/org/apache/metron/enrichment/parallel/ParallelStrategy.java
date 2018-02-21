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
package org.apache.metron.enrichment.parallel;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.json.simple.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class ParallelStrategy implements Strategy {
  private Executor executor;
  private Cache<CacheKey, JSONObject> cache;

  @Override
  public synchronized void initializeThreading(int numThreads, long maxCacheSize, long maxTimeRetain) {
    if(!(executor == null && cache == null)) {
      return;
    }
    executor = Executors.newFixedThreadPool(numThreads);
    cache = CacheBuilder.newBuilder().maximumSize(maxCacheSize)
          .expireAfterWrite(maxTimeRetain, TimeUnit.MINUTES)
          .build();
  }

  @Override
  public Executor getExecutor() {
    return executor;
  }

  @Override
  public Cache<CacheKey, JSONObject> getCache() {
    return cache;
  }
}
