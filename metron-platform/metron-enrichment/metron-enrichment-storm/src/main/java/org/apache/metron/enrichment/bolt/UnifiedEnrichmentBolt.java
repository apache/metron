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
package org.apache.metron.enrichment.bolt;

import static org.apache.metron.common.Constants.STELLAR_CONTEXT_CONF;

import org.apache.metron.common.Constants;
import org.apache.metron.storm.common.bolt.ConfiguredEnrichmentBolt;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.storm.common.message.MessageGetStrategy;
import org.apache.metron.storm.common.message.MessageGetters;
import org.apache.metron.common.performance.PerformanceLogger;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.enrichment.adapters.maxmind.asn.GeoLiteAsnDatabase;
import org.apache.metron.enrichment.adapters.maxmind.geo.GeoLiteCityDatabase;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.configuration.Enrichment;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.enrichment.parallel.EnrichmentContext;
import org.apache.metron.enrichment.parallel.EnrichmentStrategies;
import org.apache.metron.enrichment.parallel.ParallelEnricher;
import org.apache.metron.enrichment.parallel.ConcurrencyContext;
import org.apache.metron.enrichment.parallel.WorkerPoolStrategies;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.storm.common.utils.StormErrorUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This bolt is a unified enrichment/threat intel bolt.  In contrast to the split/enrich/join
 * bolts above, this handles the entire enrichment lifecycle in one bolt using a threadpool to
 * enrich in parallel.
 *
 * From an architectural perspective, this is a divergence from the polymorphism based strategy we have
 * used in the split/join bolts.  Rather, this bolt is provided a strategy to use, either enrichment or threat intel,
 * through composition.  This allows us to move most of the implementation into components independent
 * from Storm.  This will greater facilitate reuse.
 */
public class UnifiedEnrichmentBolt extends ConfiguredEnrichmentBolt {

  public static class Perf {} // used for performance logging
  private PerformanceLogger perfLog; // not static bc multiple bolts may exist in same worker

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The number of threads in the threadpool.  One threadpool is created per process.
   * This is a topology-level configuration
   */
  public static final String THREADPOOL_NUM_THREADS_TOPOLOGY_CONF = "metron.threadpool.size";
  /**
   * The type of threadpool to create. This is a topology-level configuration.
   */
  public static final String THREADPOOL_TYPE_TOPOLOGY_CONF = "metron.threadpool.type";

  /**
   * The enricher implementation to use.  This will do the parallel enrichment via a thread pool.
   */
  protected ParallelEnricher enricher;

  /**
   * The strategy to use for this enrichment bolt.  Practically speaking this is either
   * enrichment or threat intel.  It is configured in the topology itself.
   */
  protected EnrichmentStrategies strategy;
  /**
   * Determine the way to retrieve the message.  This must be specified in the topology.
   */
  protected MessageGetStrategy messageGetter;
  protected MessageGetters getterStrategy;
  protected OutputCollector collector;
  private Context stellarContext;
  /**
   * An enrichment type to adapter map.  This is configured externally.
   */
  protected Map<String, EnrichmentAdapter<CacheKey>> enrichmentsByType = new HashMap<>();

  /**
   * The total number of elements in a LRU cache.  This cache is used for the enrichments; if an
   * element is in the cache, then the result is returned instead of computed.
   */
  protected Long maxCacheSize;
  /**
   * The total amount of time in minutes since write to keep an element in the cache.
   */
  protected Long maxTimeRetain;
  /**
   * If the bolt is reloaded, invalidate the cache?
   */
  protected boolean invalidateCacheOnReload = false;

  /**
   * The message field to use.  If this is set, then this indicates the field to use to retrieve the message object.
   * IF this is unset, then we presume that the message is coming in as a string version of a JSON blob on the first
   * element of the tuple.
   */
  protected String messageFieldName;
  protected EnrichmentContext enrichmentContext;
  protected boolean captureCacheStats = true;

  public UnifiedEnrichmentBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  /**
   * Specify the enrichments to support.
   * @param enrichments enrichment
   * @return Instance of this class
   */
  public UnifiedEnrichmentBolt withEnrichments(List<Enrichment> enrichments) {
    for(Enrichment e : enrichments) {
      enrichmentsByType.put(e.getType(), e.getAdapter());
    }
    return this;
  }

  public UnifiedEnrichmentBolt withCaptureCacheStats(boolean captureCacheStats) {
    this.captureCacheStats = captureCacheStats;
    return this;
  }

  /**
   * Determine the message get strategy (One of the enums from MessageGetters).
   * @param getter
   * @return
   */
  public UnifiedEnrichmentBolt withMessageGetter(String getter) {
    this.getterStrategy = MessageGetters.valueOf(getter);
    return this;
  }

  /**
   * Figure out how many threads to use in the thread pool.  The user can pass an arbitrary object, so parse it
   * according to some rules.  If it's a number, then cast to an int.  IF it's a string and ends with "C", then strip
   * the C and treat it as an integral multiple of the number of cores.  If it's a string and does not end with a C, then treat
   * it as a number in string form.
   * @param numThreads
   * @return
   */
  private static int getNumThreads(Object numThreads) {
    if(numThreads instanceof Number) {
      return ((Number)numThreads).intValue();
    }
    else if(numThreads instanceof String) {
      String numThreadsStr = ((String)numThreads).trim().toUpperCase();
      if(numThreadsStr.endsWith("C")) {
        Integer factor = Integer.parseInt(numThreadsStr.replace("C", ""));
        return factor*Runtime.getRuntime().availableProcessors();
      }
      else {
        return Integer.parseInt(numThreadsStr);
      }
    }
    return 2*Runtime.getRuntime().availableProcessors();
  }

  /**
   * The strategy to use.  This indicates which part of the config that this bolt uses
   * to enrich, threat intel or enrichment.  This must conform to one of the EnrichmentStrategies
   * enum.
   * @param strategy
   * @return
   */
  public UnifiedEnrichmentBolt withStrategy(String strategy) {
    this.strategy = EnrichmentStrategies.valueOf(strategy);
    return this;
  }

  /**
   * @param maxCacheSize Maximum size of cache before flushing
   * @return Instance of this class
   */
  public UnifiedEnrichmentBolt withMaxCacheSize(long maxCacheSize) {
    this.maxCacheSize = maxCacheSize;
    return this;
  }

  /**
   * @param maxTimeRetain Maximum time to retain cached entry before expiring
   * @return Instance of this class
   */

  public UnifiedEnrichmentBolt withMaxTimeRetain(long maxTimeRetain) {
    this.maxTimeRetain = maxTimeRetain;
    return this;
  }

  /**
   * Invalidate the cache on reload of bolt.  By default, we do not.
   * @param cacheInvalidationOnReload
   * @return
   */
  public UnifiedEnrichmentBolt withCacheInvalidationOnReload(boolean cacheInvalidationOnReload) {
    this.invalidateCacheOnReload= cacheInvalidationOnReload;
    return this;
  }


  @Override
  public void reloadCallback(String name, ConfigurationType type) {
    if(invalidateCacheOnReload) {
      if(strategy != null && ConcurrencyContext.get(strategy).getCache() != null) {
        ConcurrencyContext.get(strategy).getCache().invalidateAll();
      }
    }
    if(type == ConfigurationType.GLOBAL && enrichmentsByType != null) {
      for(EnrichmentAdapter adapter : enrichmentsByType.values()) {
        adapter.updateAdapter(getConfigurations().getGlobalConfig());
      }
    }
  }


  /**
   * Fully enrich a message based on the strategy which was used to configure the bolt.
   * Each enrichment is done in parallel and the results are joined together.  Each enrichment
   * will use a cache so computation is avoided if the result has been computed before.
   *
   * Errors in the enrichment result in an error message being sent on the "error" stream.
   * The successful enrichments will be joined with the original message and the message will
   * be sent along the "message" stream.
   *
   * @param input The input tuple to be processed.
   */
  @Override
  public void execute(Tuple input) {
    JSONObject message = generateMessage(input);
    try {
      String sourceType = MessageUtils.getSensorType(message);
      SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sourceType);
      if(config == null) {
        LOG.debug("Unable to find SensorEnrichmentConfig for sourceType: {}", sourceType);
        config = new SensorEnrichmentConfig();
      }
      //This is an existing kludge for the stellar adapter to pass information along.
      //We should figure out if this can be rearchitected a bit.  This smells.
      config.getConfiguration().putIfAbsent(STELLAR_CONTEXT_CONF, stellarContext);
      String guid = getGUID(input, message);

      // enrich the message
      ParallelEnricher.EnrichmentResult result = enricher.apply(message, strategy, config, perfLog);
      JSONObject enriched = result.getResult();
      enriched = strategy.postProcess(enriched, config, enrichmentContext);

      //we can emit the message now
      collector.emit("message",
              input,
              new Values(guid, enriched));
      //and handle each of the errors in turn.  If any adapter errored out, we will have one message per.
      for(Map.Entry<Object, Throwable> t : result.getEnrichmentErrors()) {
        LOG.error("[Metron] Unable to enrich message: {}", message, t);
        MetronError error = new MetronError()
                .withErrorType(strategy.getErrorType())
                .withMessage(t.getValue().getMessage())
                .withThrowable(t.getValue())
                .addRawMessage(t.getKey());
        StormErrorUtils.handleError(collector, error);
      }
    } catch (Exception e) {
      //If something terrible and unexpected happens then we want to send an error along, but this
      //really shouldn't be happening.
      LOG.error("[Metron] Unable to enrich message: {}", message, e);
      MetronError error = new MetronError()
              .withErrorType(strategy.getErrorType())
              .withMessage(e.getMessage())
              .withThrowable(e)
              .addRawMessage(message);
      StormErrorUtils.handleError(collector, error);
    }
    finally {
      collector.ack(input);
    }
  }

  /**
   * The message field name.  If this is set, then use this field to retrieve the message.
   * @param messageFieldName
   * @return
   */
  public UnifiedEnrichmentBolt withMessageFieldName(String messageFieldName) {
    this.messageFieldName = messageFieldName;
    return this;
  }

  /**
   * Take the tuple and construct the message.
   * @param tuple
   * @return
   */
  public JSONObject generateMessage(Tuple tuple) {
    return (JSONObject) messageGetter.get(tuple);
  }

  @Override
  public final void prepare(Map map, TopologyContext topologyContext,
                       OutputCollector outputCollector) {
    super.prepare(map, topologyContext, outputCollector);
    collector = outputCollector;
    if (this.maxCacheSize == null) {
      throw new IllegalStateException("MAX_CACHE_SIZE_OBJECTS_NUM must be specified");
    }
    if (this.maxTimeRetain == null) {
      throw new IllegalStateException("MAX_TIME_RETAIN_MINUTES must be specified");
    }
    if (this.enrichmentsByType.isEmpty()) {
      throw new IllegalStateException("Adapter must be specified");
    }

    for(Map.Entry<String, EnrichmentAdapter<CacheKey>> adapterKv : enrichmentsByType.entrySet()) {
      boolean success = adapterKv.getValue().initializeAdapter(getConfigurations().getGlobalConfig());
      if (!success) {
        LOG.error("[Metron] Could not initialize adapter: " + adapterKv.getKey());
        throw new IllegalStateException("Could not initialize adapter: " + adapterKv.getKey());
      }
    }
    WorkerPoolStrategies workerPoolStrategy = WorkerPoolStrategies.FIXED;
    if(map.containsKey(THREADPOOL_TYPE_TOPOLOGY_CONF)) {
      workerPoolStrategy = WorkerPoolStrategies.valueOf(map.get(THREADPOOL_TYPE_TOPOLOGY_CONF) + "");
    }
    if(map.containsKey(THREADPOOL_NUM_THREADS_TOPOLOGY_CONF)) {
      int numThreads = getNumThreads(map.get(THREADPOOL_NUM_THREADS_TOPOLOGY_CONF));
      ConcurrencyContext.get(strategy).initialize(numThreads, maxCacheSize, maxTimeRetain, workerPoolStrategy, LOG, captureCacheStats);
    }
    else {
      throw new IllegalStateException("You must pass " + THREADPOOL_NUM_THREADS_TOPOLOGY_CONF + " via storm config.");
    }
    messageGetter = this.getterStrategy.get(messageFieldName);
    enricher = new ParallelEnricher(enrichmentsByType, ConcurrencyContext.get(strategy), captureCacheStats);
    perfLog = new PerformanceLogger(() -> getConfigurations().getGlobalConfig(), Perf.class.getName());
    GeoLiteCityDatabase.INSTANCE.update((String)getConfigurations().getGlobalConfig().get(
        GeoLiteCityDatabase.GEO_HDFS_FILE));
    GeoLiteAsnDatabase.INSTANCE.update((String)getConfigurations().getGlobalConfig().get(
        GeoLiteAsnDatabase.ASN_HDFS_FILE));
    initializeStellar();
    enrichmentContext = new EnrichmentContext(StellarFunctions.FUNCTION_RESOLVER(), stellarContext);
  }


  protected void initializeStellar() {
    stellarContext = new Context.Builder()
                         .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
                         .with(Context.Capabilities.GLOBAL_CONFIG, () -> getConfigurations().getGlobalConfig())
                         .with(Context.Capabilities.STELLAR_CONFIG, () -> getConfigurations().getGlobalConfig())
                         .build();
    StellarFunctions.initialize(stellarContext);
  }

  /**
   * Return the GUID from either the tuple or the message.
   *
   * @param tuple
   * @param message
   * @return
   */
  public String getGUID(Tuple tuple, JSONObject message) {
    String key = null, guid = null;
    try {
      key = tuple.getStringByField("key");
      guid = (String)message.get(Constants.GUID);
    }
    catch(Throwable t) {
      //swallowing this just in case.
    }
    if(key != null) {
      return key;
    }
    else if(guid != null) {
      return guid;
    }
    else {
      return UUID.randomUUID().toString();
    }
  }

  /**
   * Declare the output schema for all the streams of this topology.
   * We declare two streams: error and message.
   *
   * @param declarer this is used to declare output stream ids, output fields, and whether or not each output stream is a direct stream
   */
  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream("message", new Fields("key", "message"));
    declarer.declareStream("error", new Fields("message"));
  }
}
