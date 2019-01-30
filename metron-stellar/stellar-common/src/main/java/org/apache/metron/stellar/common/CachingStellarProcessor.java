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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
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

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static ThreadLocal<Map<String, Set<String>> > variableCache = ThreadLocal.withInitial(() -> new HashMap<>());

  /**
   * A property that defines the maximum cache size.
   */
  public static String MAX_CACHE_SIZE_PARAM = "stellar.cache.maxSize";

  /**
   * A property that defines the max time in minutes that elements are retained in the cache.
   */
  public static String MAX_TIME_RETAIN_PARAM = "stellar.cache.maxTimeRetain";

  /**
   * A property that defines if cache usage stats should be recorded.
   */
  public static String RECORD_STATS = "stellar.cache.record.stats";

  /**
   * The cache key is based on the expression and input values.
   */
  public static class Key {

    /**
     * The expression to execute.
     */
    private String expression;

    /**
     * The variables that serve as input to the expression.
     */
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
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Key key = (Key) o;
      return new EqualsBuilder()
              .append(expression, key.expression)
              .append(input, key.input)
              .isEquals();
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 37)
              .append(expression)
              .append(input)
              .toHashCode();
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this)
              .append("expression", expression)
              .append("input", input)
              .toString();
    }
  }

  /**
   * Parses and evaluates the given Stellar expression, {@code expression}. Results will be taken
   * from a cache if possible.
   *
   * @param expression The Stellar expression to parse and evaluate.
   * @param variableResolver The {@link VariableResolver} to determine values of variables used in
   *     the Stellar expression, {@code expression}.
   * @param functionResolver The {@link FunctionResolver} to determine values of functions used in
   *     the Stellar expression, {@code expression}.
   * @param context The context used during validation.
   * @return The value of the evaluated Stellar expression, {@code expression}.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Object parse(
      String expression,
      VariableResolver variableResolver,
      FunctionResolver functionResolver,
      Context context) {

    Optional<Object> cacheOpt = context.getCapability(Context.Capabilities.CACHE, false);
    if(cacheOpt.isPresent()) {

      // use the cache
      Cache<Key, Object> cache = (Cache<Key, Object>) cacheOpt.get();
      Key k = toKey(expression, variableResolver);
      return cache.get(k, x -> parseUncached(x.expression, variableResolver, functionResolver, context));

    } else {

      LOG.debug("No cache present.");
      return parseUncached(expression, variableResolver, functionResolver, context);
    }
  }

  protected Object parseUncached(String expression, VariableResolver variableResolver, FunctionResolver functionResolver, Context context) {
    LOG.debug("Executing Stellar; expression={}", expression);
    return super.parse(expression, variableResolver, functionResolver, context);
  }

  /**
   * Create a cache key using the expression and all variables used by that expression.
   *
   * @param expression The Stellar expression.
   * @param resolver The variable resolver.
   * @return A key with which to do a cache lookup.
   */
  protected Key toKey(String expression, VariableResolver resolver) {

    // fetch only the variables used in the expression
    Set<String> variablesUsed = variableCache.get().computeIfAbsent(expression, this::variablesUsed);

    // resolve each of the variables used by the expression
    Map<String, Object> input = new HashMap<>();
    for(String v : variablesUsed) {
      input.computeIfAbsent(v, resolver::resolve);
    }

    Key cacheKey = new Key(expression, input);
    LOG.debug("Created cache key; {}", cacheKey);
    return cacheKey;
  }

  /**
   * Create a cache given a config.  Note that if the cache size is {@literal <}= 0, then no cache will be returned.
   * @param config
   * @return A cache.
   */
  public static Cache<Key, Object> createCache(Map<String, Object> config) {

    // the cache configuration is required
    if(config == null) {
      LOG.debug("Cannot create cache; missing cache configuration");
      return null;
    }

    // max cache size is required
    Long maxSize = getParam(config, MAX_CACHE_SIZE_PARAM, null, Long.class);
    if(maxSize == null || maxSize <= 0) {
      LOG.error("Cannot create cache; missing or invalid configuration; {} = {}", MAX_CACHE_SIZE_PARAM, maxSize);
      return null;
    }

    // max time retain is required
    Integer maxTimeRetain = getParam(config, MAX_TIME_RETAIN_PARAM, null, Integer.class);
    if(maxTimeRetain == null || maxTimeRetain <= 0) {
      LOG.error("Cannot create cache; missing or invalid configuration; {} = {}", MAX_TIME_RETAIN_PARAM, maxTimeRetain);
      return null;
    }

    Caffeine<Object, Object> cache = Caffeine
            .newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(maxTimeRetain, TimeUnit.MINUTES);

    // record stats is optional
    Boolean recordStats = getParam(config, RECORD_STATS, false, Boolean.class);
    if(recordStats) {
      cache.recordStats();
    }

    return cache.build();
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
