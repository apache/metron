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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.metron.enrichment.cache.CacheKey;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * This provides the parallel infrastructure, the thread pool and the cache.
 * The threadpool is static and the cache is instance specific.
 */
public class ConcurrencyContext {
  private static Executor executor;
  private Cache<CacheKey, JSONObject> cache;

  private static EnumMap<EnrichmentStrategies, ConcurrencyContext> strategyToInfrastructure
          = new EnumMap<EnrichmentStrategies, ConcurrencyContext>(EnrichmentStrategies.class) {{
    for(EnrichmentStrategies e : EnrichmentStrategies.values()) {
      put(e, new ConcurrencyContext());
    }
  }};

  public static ConcurrencyContext get(EnrichmentStrategies strategy) {
    return strategyToInfrastructure.get(strategy);
  }

  protected ConcurrencyContext() { }

  /*
   * Initialize the thread pool and cache.  The threadpool is static and the cache is per strategy.
   *
   * @param numThreads The number of threads in the threadpool.
   * @param maxCacheSize The maximum size of the cache, beyond which and keys are evicted.
   * @param maxTimeRetain The maximum time to retain an element in the cache (in minutes)
   * @param poolStrategy The strategy for creating a threadpool
   * @param log The logger to use
   * @param logStats Should we record stats in the cache?
   */
  public synchronized void initialize( int numThreads
                                     , long maxCacheSize
                                     , long maxTimeRetain
                                     , WorkerPoolStrategies poolStrategy
                                     , Logger log
                                     , boolean logStats
                                     ) {
    if(executor == null) {
      if (log != null) {
        log.info("Creating new threadpool of size {}", numThreads);
      }
      executor = (poolStrategy == null? WorkerPoolStrategies.FIXED:poolStrategy).create(numThreads);
    }
    if(cache == null) {
      if (log != null) {
        log.info("Creating new cache with maximum size {}, and expiration after write of {} minutes", maxCacheSize, maxTimeRetain);
      }
      Caffeine builder = Caffeine.newBuilder().maximumSize(maxCacheSize)
                           .expireAfterWrite(maxTimeRetain, TimeUnit.MINUTES)
                           .executor(executor)
                         ;
      if(logStats) {
        builder = builder.recordStats();
      }
      cache = builder.build();
    }
  }

  public static Executor getExecutor() {
    return executor;
  }

  public Cache<CacheKey, JSONObject> getCache() {
    return cache;
  }
}
