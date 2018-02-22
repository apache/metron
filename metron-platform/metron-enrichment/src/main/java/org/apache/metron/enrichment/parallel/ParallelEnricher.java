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
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.performance.PerformanceLogger;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.apache.metron.enrichment.utils.ThreatIntelUtils;
import org.json.simple.JSONObject;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public class ParallelEnricher {

  private Map<String, EnrichmentAdapter<CacheKey>> enrichmentsByType = new HashMap<>();

  public static class EnrichmentResult {
    private JSONObject result;
    private List<Map.Entry<Object, Throwable>> enrichmentErrors;

    public EnrichmentResult(JSONObject result, List<Map.Entry<Object, Throwable>> enrichmentErrors) {
      this.result = result;
      this.enrichmentErrors = enrichmentErrors;
    }

    public JSONObject getResult() {
      return result;
    }

    public List<Map.Entry<Object, Throwable>> getEnrichmentErrors() {
      return enrichmentErrors;
    }
  }

  public ParallelEnricher( Map<String, EnrichmentAdapter<CacheKey>> enrichmentsByType)
  {
    this.enrichmentsByType = enrichmentsByType;
  }

  /**
   * Applies this function to the given argument.
   *
   * @param message the function argument
   * @return the function result
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
    }
    String sensorType = MessageUtils.getSensorType(message);
    message.put(getClass().getSimpleName().toLowerCase() + ".splitter.begin.ts", "" + System.currentTimeMillis());
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
      EnrichmentAdapter<CacheKey> adapter = enrichmentsByType.get(task.getKey());
      for(JSONObject m : task.getValue()) {
        for(Object o : m.keySet()) {
          String field = (String) o;
          Object value = m.get(o);
          CacheKey cacheKey = new CacheKey(field, value, config);
          String prefix = adapter.getOutputPrefix(cacheKey);
          Supplier<JSONObject> supplier = () -> {
            try {
              JSONObject ret = strategy.getCache().get(cacheKey, new EnrichmentCallable(cacheKey, adapter));
              if(ret == null) {
                ret = new JSONObject();
              }
              return EnrichmentUtils.adjustKeys(new JSONObject(), ret, cacheKey.getField(), prefix);
            } catch (Throwable e) {
              JSONObject errorMessage = new JSONObject();
              errorMessage.putAll(m);
              errorMessage.put(Constants.SENSOR_TYPE, sensorType );
              errors.add(new AbstractMap.SimpleEntry<>(errorMessage, new IllegalStateException(strategy + " error with " + task.getKey() + " failed: " + e.getMessage(), e)));
              return new JSONObject();
            }
          };
          taskList.add(CompletableFuture.supplyAsync( supplier, EnrichmentStrategies.getExecutor()));
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


  public static CompletableFuture<JSONObject> all(
            List<CompletableFuture<JSONObject>> futures
          , JSONObject identity
          , BinaryOperator<JSONObject> reduceOp
  ) {
    CompletableFuture[] cfs = futures.toArray(new CompletableFuture[futures.size()]);
    CompletableFuture<Void> future = CompletableFuture.allOf(cfs);
    return future.thenApply(aVoid -> futures.stream().map(CompletableFuture::join).reduce(identity, reduceOp));
  }

  public Map<String, List<JSONObject>> splitMessage( JSONObject message
                                                   , Strategy strategy
                                                   , SensorEnrichmentConfig config
                                                   ) {
    Map<String, List<JSONObject>> streamMessageMap = new HashMap<>();
    Map<String, Object> enrichmentFieldMap = strategy.enrichmentFieldMap(config);
    Map<String, ConfigHandler> fieldToHandler = strategy.fieldToHandler(config);
    Set<String> enrichmentTypes = new HashSet<>(enrichmentFieldMap.keySet());
    enrichmentTypes.addAll(fieldToHandler.keySet());
    for (String enrichmentType : enrichmentTypes) {
      Object fields = enrichmentFieldMap.get(enrichmentType);
      ConfigHandler retriever = fieldToHandler.get(enrichmentType);

      List<JSONObject> enrichmentObject = retriever.getType()
              .splitByFields( message
                      , fields
                      , field -> strategy.fieldToEnrichmentKey(enrichmentType, field)
                      , retriever
              );
      streamMessageMap.put(enrichmentType, enrichmentObject);
    }
    return streamMessageMap;
  }

}
