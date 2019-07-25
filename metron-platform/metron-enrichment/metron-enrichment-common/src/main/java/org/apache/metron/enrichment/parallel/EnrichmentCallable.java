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

import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Enrich based on a key and enrichment adapter.  The CacheKey contains all necessary input information for an enrichment.
 */
public class EnrichmentCallable implements Callable<JSONObject>, Function<CacheKey, JSONObject> {
  CacheKey key;
  EnrichmentAdapter<CacheKey> adapter;

  public EnrichmentCallable( CacheKey key
          , EnrichmentAdapter<CacheKey> adapter
  )
  {
    this.key = key;
    this.adapter = adapter;
  }

  /**
   * Computes a result, or throws an exception if unable to do so.
   *
   * @return computed result
   * @throws Exception if unable to compute a result
   */
  @Override
  public JSONObject call() throws Exception {
    //Log access for this key.
    adapter.logAccess(key);
    return adapter.enrich(key);
  }

  /**
   * Applies this function to the given argument.
   *
   * @param cacheKey the function argument
   * @return the function result
   */
  @Override
  public JSONObject apply(CacheKey cacheKey) {
    adapter.logAccess(key);
    return adapter.enrich(cacheKey);
  }
}
