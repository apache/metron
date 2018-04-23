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
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CachingStellarProcessorTest {

  private static Map<String, Object> fields = new HashMap<String, Object>() {{
      put("name", "blah");
    }};

  @Test
  public void testNoCaching() throws Exception {
    //no caching, so every expression is a cache miss.
    Assert.assertEquals(2, countMisses(2, Context.EMPTY_CONTEXT(), "TO_UPPER(name)"));
    //Ensure the correct result is returned.
    Assert.assertEquals("BLAH", evaluateExpression(Context.EMPTY_CONTEXT(), "TO_UPPER(name)"));
  }

  @Test
  public void testCaching() throws Exception {
    Cache<CachingStellarProcessor.Key, Object> cache = CachingStellarProcessor.createCache(
                                                 ImmutableMap.of(CachingStellarProcessor.MAX_CACHE_SIZE_PARAM, 2
                                                                ,CachingStellarProcessor.MAX_TIME_RETAIN_PARAM, 10
                                                                )
                                                                           );
    Context context = new Context.Builder()
                                 .with( Context.Capabilities.CACHE , () -> cache )
                                 .build();
    //running the same expression twice should hit the cache on the 2nd time and only yield one miss
    Assert.assertEquals(1, countMisses(2, context, "TO_UPPER(name)"));

    //Ensure the correct result is returned.
    Assert.assertEquals("BLAH", evaluateExpression(context, "TO_UPPER(name)"));

    //running the same expression 20 more times should pull from the cache
    Assert.assertEquals(0, countMisses(20, context, "TO_UPPER(name)"));

    //Now we are running 4 distinct operations with a cache size of 2.  The cache has 1 element in it before we start:
    //  TO_LOWER(name) - miss (brand new), cache is full
    //  TO_UPPER(name) - hit, cache is full
    //  TO_UPPER('foo') - miss (brand new), cache is still full, but TO_LOWER is evicted as the least frequently used
    //  JOIN... - miss (brand new), cache is still full, but TO_UPPER('foo') is evicted as the least frequently used
    //this pattern repeats a 2nd time to add another 3 cache misses, totalling 6.
    Assert.assertEquals(6, countMisses(2, context, "TO_LOWER(name)", "TO_UPPER(name)", "TO_UPPER('foo')", "JOIN([name, 'blah'], ',')"));
  }

  private Object evaluateExpression(Context context, String expression) {
    StellarProcessor processor = new CachingStellarProcessor();
    return processor.parse(expression
                , new MapVariableResolver(fields)
                , StellarFunctions.FUNCTION_RESOLVER()
                , context);
  }

  private int countMisses(int numRepetition, Context context, String... expressions) {
    AtomicInteger numExpressions = new AtomicInteger(0);
    StellarProcessor processor = new CachingStellarProcessor() {
      @Override
      protected Object parseUncached(String expression, VariableResolver variableResolver, FunctionResolver functionResolver, Context context) {
        numExpressions.incrementAndGet();
        return super.parseUncached(expression, variableResolver, functionResolver, context);
      }
    };

    for(int i = 0;i < numRepetition;++i) {
      for(String expression : expressions) {
        processor.parse(expression
                , new MapVariableResolver(fields)
                , StellarFunctions.FUNCTION_RESOLVER()
                , context);
      }
    }
    return numExpressions.get();
  }
}
