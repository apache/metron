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
import org.slf4j.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class ParallelStrategy implements Strategy {
  private static Executor executor;
  private Cache<CacheKey, JSONObject> cache;

  @Override
  public synchronized void initializeThreading(int numThreads, long maxCacheSize, long maxTimeRetain, Logger log) {
    if(executor == null) {
      if (log != null) {
        log.info("Creating new threadpool of size {}", numThreads);
      }
      executor = Executors.newFixedThreadPool(numThreads);
    }
    if(cache == null) {
      if (log != null) {
        log.info("Creating new cache with maximum size {}, and expiration after write of {} minutes", maxCacheSize, maxTimeRetain);
      }
      cache = CacheBuilder.newBuilder().maximumSize(maxCacheSize)
              .expireAfterWrite(maxTimeRetain, TimeUnit.MINUTES)
              .build();
    }
  }

  public static Executor getExecutor() {
    return executor;
  }

  @Override
  public Cache<CacheKey, JSONObject> getCache() {
    return cache;
  }
}
