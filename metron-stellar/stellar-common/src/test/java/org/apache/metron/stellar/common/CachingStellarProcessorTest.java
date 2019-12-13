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
package org.apache.metron.stellar.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableMap;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CachingStellarProcessorTest {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static Map<String, Object> fields = new HashMap<String, Object>() {{
      put("name", "blah");
    }};

  private CachingStellarProcessor processor;
  private Cache<CachingStellarProcessor.Key, Object> cache;
  private Context contextWithCache;

  @BeforeEach
  public void setup() throws Exception {

    // create the cache
    Map<String, Object> cacheConfig = ImmutableMap.of(
            CachingStellarProcessor.MAX_CACHE_SIZE_PARAM, 2,
            CachingStellarProcessor.MAX_TIME_RETAIN_PARAM, 10,
            CachingStellarProcessor.RECORD_STATS, true
    );
    cache = CachingStellarProcessor.createCache(cacheConfig);
    contextWithCache = new Context.Builder()
            .with(Context.Capabilities.CACHE, () -> cache)
            .build();

    // create the object to test
    processor = new CachingStellarProcessor();
  }

  /**
   * Running the same expression multiple times should hit the cache.
   */
  @Test
  public void testWithCache() {

    Object result = execute("TO_UPPER(name)", contextWithCache);
    assertEquals("BLAH", result);
    assertEquals(1, cache.stats().requestCount());
    assertEquals(1, cache.stats().missCount());
    assertEquals(0, cache.stats().hitCount());

    result = execute("TO_UPPER(name)", contextWithCache);
    assertEquals("BLAH", result);
    assertEquals(2, cache.stats().requestCount());
    assertEquals(1, cache.stats().missCount());
    assertEquals(1, cache.stats().hitCount());

    result = execute("TO_UPPER(name)", contextWithCache);
    assertEquals("BLAH", result);
    assertEquals(3, cache.stats().requestCount());
    assertEquals(1, cache.stats().missCount());
    assertEquals(2, cache.stats().hitCount());
  }

  /**
   * The processor should work, even if no cache is present in the execution context.
   */
  @Test
  public void testNoCache() {

    // the execution context does not contain a cache
    Context contextNoCache = Context.EMPTY_CONTEXT();

    assertEquals("BLAH", execute("TO_UPPER(name)", contextNoCache));
    assertEquals("BLAH", execute("TO_UPPER(name)", contextNoCache));
  }

  @Test
  public void testInvalidMaxCacheSize() {
    Map<String, Object> cacheConfig = ImmutableMap.of(
            CachingStellarProcessor.MAX_CACHE_SIZE_PARAM, -1,
            CachingStellarProcessor.MAX_TIME_RETAIN_PARAM, 10
    );
    cache = CachingStellarProcessor.createCache(cacheConfig);
    assertNull(cache);
  }

  @Test
  public void testMissingMaxCacheSize() {
    Map<String, Object> cacheConfig = ImmutableMap.of(
            CachingStellarProcessor.MAX_TIME_RETAIN_PARAM, 10
    );
    cache = CachingStellarProcessor.createCache(cacheConfig);
    assertNull(cache);
  }

  @Test
  public void testInvalidMaxTimeRetain() {
    Map<String, Object> cacheConfig = ImmutableMap.of(
            CachingStellarProcessor.MAX_CACHE_SIZE_PARAM, 10,
            CachingStellarProcessor.MAX_TIME_RETAIN_PARAM, -2
    );
    cache = CachingStellarProcessor.createCache(cacheConfig);
    assertNull(cache);
  }

  @Test
  public void testMissingMaxTimeRetain() {
    Map<String, Object> cacheConfig = ImmutableMap.of(
            CachingStellarProcessor.MAX_CACHE_SIZE_PARAM, 10
    );
    cache = CachingStellarProcessor.createCache(cacheConfig);
    assertNull(cache);
  }

  /**
   * The cache should continue to hit, even if variables not used in the cached expression change.
   */
  @Test
  public void testUnrelatedVariableChange() {

    // expect miss
    Object result = execute("TO_UPPER(name)", contextWithCache);
    assertEquals("BLAH", result);
    assertEquals(1, cache.stats().requestCount());
    assertEquals(1, cache.stats().missCount());
    assertEquals(0, cache.stats().hitCount());

    // add an irrelevant variable that is not used in the expression
    fields.put("unrelated_var_1", "true");
    fields.put("unrelated_var_2", 22);

    // still expect a hit
    result = execute("TO_UPPER(name)", contextWithCache);
    assertEquals("BLAH", result);
    assertEquals(2, cache.stats().requestCount());
    assertEquals(1, cache.stats().missCount());
    assertEquals(1, cache.stats().hitCount());

  }

  /**
   * Execute each expression.
   * @param expression The expression to execute.
   */
  private Object execute(String expression, Context context) {

    Object result = processor.parse(
            expression,
            new MapVariableResolver(fields),
            StellarFunctions.FUNCTION_RESOLVER(),
            context);
    return result;
  }
}
