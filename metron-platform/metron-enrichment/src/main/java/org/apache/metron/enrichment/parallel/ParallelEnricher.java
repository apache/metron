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

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.performance.PerformanceLogger;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.json.simple.JSONObject;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

/**
 * This is an independent component which will accept a message and a set of enrichment adapters as well as a config which defines
 * how those enrichments should be performed and fully enrich the message.  The result will be the enriched message
 * unified together and a list of errors which happened.
 */
public class ParallelEnricher {

  private Map<String, EnrichmentAdapter<CacheKey>> enrichmentsByType = new HashMap<>();
  private EnumMap<EnrichmentStrategies, CacheStats> cacheStats = new EnumMap<>(EnrichmentStrategies.class);

  /**
   * The result of an enrichment.
   */
  public static class EnrichmentResult {
    private JSONObject result;
    private List<Map.Entry<Object, Throwable>> enrichmentErrors;

    public EnrichmentResult(JSONObject result, List<Map.Entry<Object, Throwable>> enrichmentErrors) {
      this.result = result;
      this.enrichmentErrors = enrichmentErrors;
    }

    /**
     * The unified fully enriched result.
     * @return
     */
    public JSONObject getResult() {
      return result;
    }

    /**
     * The errors that happened in the course of enriching.
     * @return
     */
    public List<Map.Entry<Object, Throwable>> getEnrichmentErrors() {
      return enrichmentErrors;
    }
  }

  private ConcurrencyContext concurrencyContext;

  /**
   * Construct a parallel enricher with a set of enrichment adapters associated with their enrichment types.
   * @param enrichmentsByType
   */
  public ParallelEnricher( Map<String, EnrichmentAdapter<CacheKey>> enrichmentsByType
                         , ConcurrencyContext concurrencyContext
                         , boolean logStats
                         )
  {
    this.enrichmentsByType = enrichmentsByType;
    this.concurrencyContext = concurrencyContext;
    if(logStats) {
      for(EnrichmentStrategies s : EnrichmentStrategies.values()) {
        cacheStats.put(s, null);
      }
    }
  }

  /**
   * Fully enriches a message.  Each enrichment is done in parallel via a threadpool.
   * Each enrichment is fronted with a LRU cache.
   *
   * @param message the message to enrich
   * @param strategy The enrichment strategy to use (e.g. enrichment or threat intel)
   * @param config The sensor enrichment config
   * @param perfLog The performance logger.  We log the performance for this call, the split portion and the enrichment portion.
   * @return the enrichment result
   */
  public EnrichmentResult apply( JSONObject message
                         , EnrichmentStrategies strategy
                         , SensorEnrichmentConfig config
                         , PerformanceLogger perfLog
                         ) throws ExecutionException, InterruptedException {
    if(message == null) {
      return null;
    }
    if(perfLog != null) {
      perfLog.mark("execute");
      if(perfLog.isDebugEnabled() && !cacheStats.isEmpty()) {
        CacheStats before =  cacheStats.get(strategy);
        CacheStats after = concurrencyContext.getCache().stats();
        if(before != null && after != null) {
          CacheStats delta = after.minus(before);
          perfLog.log("cache", delta.toString());
        }
        cacheStats.put(strategy, after);
      }
    }
    String sensorType = MessageUtils.getSensorType(message);
    message.put(getClass().getSimpleName().toLowerCase() + ".splitter.begin.ts", "" + System.currentTimeMillis());
    // Split the message into individual tasks.
    //
    // A task will either correspond to an enrichment adapter or,
    // in the case of Stellar, a stellar subgroup.  The tasks will be grouped by enrichment type (the key of the
    //tasks map).  Each JSONObject will correspond to a unit of work.
    Map<String, List<JSONObject>> tasks = splitMessage( message
                                                      , strategy
                                                      , config
                                                      );
    message.put(getClass().getSimpleName().toLowerCase() + ".splitter.end.ts", "" + System.currentTimeMillis());
    message.put(getClass().getSimpleName().toLowerCase() + ".enrich.begin.ts", "" + System.currentTimeMillis());
    if(perfLog != null) {
      perfLog.mark("enrich");
    }
    List<CompletableFuture<JSONObject>> taskList = new ArrayList<>();
    List<Map.Entry<Object, Throwable>> errors = Collections.synchronizedList(new ArrayList<>());
    for(Map.Entry<String, List<JSONObject>> task : tasks.entrySet()) {
      //task is the list of enrichment tasks for the task.getKey() adapter
      EnrichmentAdapter<CacheKey> adapter = enrichmentsByType.get(task.getKey());
      for(JSONObject m : task.getValue()) {
        /* now for each unit of work (each of these only has one element in them)
         * the key is the field name and the value is value associated with that field.
         *
         * In the case of stellar enrichment, the field name is the subgroup name or empty string.
         * The value is the subset of the message needed for the enrichment.
         *
         * In the case of another enrichment (e.g. hbase), the field name is the field name being enriched.
         * The value is the corresponding value.
         */
        for(Object o : m.keySet()) {
          String field = (String) o;
          Object value = m.get(o);
          CacheKey cacheKey = new CacheKey(field, value, config);
          String prefix = adapter.getOutputPrefix(cacheKey);
          Supplier<JSONObject> supplier = () -> {
            try {
              JSONObject ret = concurrencyContext.getCache().get(cacheKey, new EnrichmentCallable(cacheKey, adapter));
              if(ret == null) {
                ret = new JSONObject();
              }
              //each enrichment has their own unique prefix to use to adjust the keys for the enriched fields.
              return EnrichmentUtils.adjustKeys(new JSONObject(), ret, cacheKey.getField(), prefix);
            } catch (Throwable e) {
              JSONObject errorMessage = new JSONObject();
              errorMessage.putAll(m);
              errorMessage.put(Constants.SENSOR_TYPE, sensorType );
              errors.add(new AbstractMap.SimpleEntry<>(errorMessage, new IllegalStateException(strategy + " error with " + task.getKey() + " failed: " + e.getMessage(), e)));
              return new JSONObject();
            }
          };
          //add the Future to the task list
          taskList.add(CompletableFuture.supplyAsync( supplier, ConcurrencyContext.getExecutor()));
        }
      }
    }
    if(taskList.isEmpty()) {
      return new EnrichmentResult(message, errors);
    }

    EnrichmentResult ret = new EnrichmentResult(all(taskList, message, (left, right) -> join(left, right)).get(), errors);
    message.put(getClass().getSimpleName().toLowerCase() + ".enrich.end.ts", "" + System.currentTimeMillis());
    if(perfLog != null) {
      String key = message.get(Constants.GUID) + "";
      perfLog.log("enrich", "key={}, elapsed time to enrich", key);
      perfLog.log("execute", "key={}, elapsed time to run execute", key);
    }
    return ret;
  }

  private static JSONObject join(JSONObject left, JSONObject right) {
    JSONObject message = new JSONObject();
    message.putAll(left);
    message.putAll(right);
    List<Object> emptyKeys = new ArrayList<>();
    for(Object key : message.keySet()) {
      Object value = message.get(key);
      if(value == null || value.toString().length() == 0) {
        emptyKeys.add(key);
      }
    }
    for(Object o : emptyKeys) {
      message.remove(o);
    }
    return message;
  }


  /**
   * Wait until all the futures complete and join the resulting JSONObjects using the supplied binary operator
   * and identity object.
   *
   * @param futures
   * @param identity
   * @param reduceOp
   * @return
   */
  public static CompletableFuture<JSONObject> all(
            List<CompletableFuture<JSONObject>> futures
          , JSONObject identity
          , BinaryOperator<JSONObject> reduceOp
  ) {
    CompletableFuture[] cfs = futures.toArray(new CompletableFuture[futures.size()]);
    CompletableFuture<Void> future = CompletableFuture.allOf(cfs);
    return future.thenApply(aVoid -> futures.stream().map(CompletableFuture::join).reduce(identity, reduceOp));
  }

  /**
   * Take a message and a config and return a list of tasks indexed by adapter enrichment types.
   * @param message
   * @param enrichmentStrategy
   * @param config
   * @return
   */
  public Map<String, List<JSONObject>> splitMessage( JSONObject message
                                                   , EnrichmentStrategy enrichmentStrategy
                                                   , SensorEnrichmentConfig config
                                                   ) {
    Map<String, List<JSONObject>> streamMessageMap = new HashMap<>();
    Map<String, Object> enrichmentFieldMap = enrichmentStrategy.getUnderlyingConfig(config).getFieldMap();

    Map<String, ConfigHandler> fieldToHandler = enrichmentStrategy.getUnderlyingConfig(config).getEnrichmentConfigs();

    Set<String> enrichmentTypes = new HashSet<>(enrichmentFieldMap.keySet());

    //the set of enrichments configured
    enrichmentTypes.addAll(fieldToHandler.keySet());

    //For each of these enrichment types, we're going to construct JSONObjects
    //which represent the individual enrichment tasks.
    for (String enrichmentType : enrichmentTypes) {
      Object fields = enrichmentFieldMap.get(enrichmentType);
      ConfigHandler retriever = fieldToHandler.get(enrichmentType);

      //How this is split depends on the ConfigHandler
      List<JSONObject> enrichmentObject = retriever.getType()
              .splitByFields( message
                      , fields
                      , field -> enrichmentStrategy.fieldToEnrichmentKey(enrichmentType, field)
                      , retriever
              );
      streamMessageMap.put(enrichmentType, enrichmentObject);
    }
    return streamMessageMap;
  }

}
