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
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * The Caching Stellar Processor is a stellar processor that optionally fronts stellar with an expression-by-expression
 * LFU cache.
 */
public class CachingStellarProcessor extends StellarProcessor {
  private static ThreadLocal<Map<String, Set<String>> > variableCache = ThreadLocal.withInitial(() -> new HashMap<>());
  public static String MAX_CACHE_SIZE_PARAM = "stellar.cache.maxSize";
  public static String MAX_TIME_RETAIN_PARAM = "stellar.cache.maxTimeRetain";

  public static class Key {
    private String expression;
    private Map<String, Object> input;

    public Key(String expression, Map<String, Object> input) {
      this.expression = expression;
      this.input = input;
    }

    public String getExpression() {
      return expression;
    }

    public Map<String, Object> getInput() {
      return input;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Key key = (Key) o;

      if (getExpression() != null ? !getExpression().equals(key.getExpression()) : key.getExpression() != null)
        return false;
      return getInput() != null ? getInput().equals(key.getInput()) : key.getInput() == null;

    }

    @Override
    public int hashCode() {
      int result = getExpression() != null ? getExpression().hashCode() : 0;
      result = 31 * result + (getInput() != null ? getInput().hashCode() : 0);
      return result;
    }
  }


  /**
   * Parses and evaluates the given Stellar expression, {@code expression}.  Results will be taken from a cache if possible.
   *
   * @param expression             The Stellar expression to parse and evaluate.
   * @param variableResolver The {@link VariableResolver} to determine values of variables used in the Stellar expression, {@code expression}.
   * @param functionResolver The {@link FunctionResolver} to determine values of functions used in the Stellar expression, {@code expression}.
   * @param context          The context used during validation.
   * @return The value of the evaluated Stellar expression, {@code expression}.
   */
  @Override
  public Object parse(String expression, VariableResolver variableResolver, FunctionResolver functionResolver, Context context) {
    Optional<Object> cacheOpt = context.getCapability(Context.Capabilities.CACHE, false);
    if(cacheOpt.isPresent()) {
      Cache<Key, Object> cache = (Cache<Key, Object>) cacheOpt.get();
      Key k = toKey(expression, variableResolver);
      return cache.get(k, x -> parseUncached(x.expression, variableResolver, functionResolver, context));
    }
    else {
      return parseUncached(expression, variableResolver, functionResolver, context);
    }
  }

  protected Object parseUncached(String expression, VariableResolver variableResolver, FunctionResolver functionResolver, Context context) {
    return super.parse(expression, variableResolver, functionResolver, context);
  }

  private Key toKey(String expression, VariableResolver resolver) {
    Set<String> variablesUsed = variableCache.get().computeIfAbsent(expression, this::variablesUsed);
    Map<String, Object> input = new HashMap<>();
    for(String v : variablesUsed) {
      input.computeIfAbsent(v, resolver::resolve);
    }
    return new Key(expression, input);
  }

  /**
   * Create a cache given a config.  Note that if the cache size is <= 0, then no cache will be returned.
   * @param config
   * @return A cache.
   */
  public static Cache<Key, Object> createCache(Map<String, Object> config) {
    if(config == null) {
      return null;
    }
    Long maxSize = getParam(config, MAX_CACHE_SIZE_PARAM, null, Long.class);
    Integer maxTimeRetain = getParam(config, MAX_TIME_RETAIN_PARAM, null, Integer.class);
    if(maxSize == null || maxTimeRetain == null || maxSize <= 0 || maxTimeRetain <= 0) {
      return null;
    }
    return Caffeine.newBuilder()
                   .maximumSize(maxSize)
                   .expireAfterWrite(maxTimeRetain, TimeUnit.MINUTES)
                   .build();
  }

  private static <T> T getParam(Map<String, Object> config, String key, T defaultVal, Class<T> clazz) {
    Object o = config.get(key);
    if(o == null) {
      return defaultVal;
    }
    T ret = ConversionUtils.convert(o, clazz);
    return ret == null?defaultVal:ret;
  }
}
