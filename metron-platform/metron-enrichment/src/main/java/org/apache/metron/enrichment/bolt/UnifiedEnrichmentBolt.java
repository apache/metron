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

import org.apache.metron.common.Constants;
import org.apache.metron.common.bolt.ConfiguredEnrichmentBolt;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.performance.PerformanceLogger;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.enrichment.adapters.geo.GeoLiteDatabase;
import org.apache.metron.enrichment.configuration.Enrichment;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.enrichment.parallel.EnrichmentStrategies;
import org.apache.metron.enrichment.parallel.ParallelEnricher;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UnifiedEnrichmentBolt extends ConfiguredEnrichmentBolt {

  public static class Perf {} // used for performance logging
  private PerformanceLogger perfLog; // not static bc multiple bolts may exist in same worker

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String STELLAR_CONTEXT_CONF = "stellarContext";

  protected ParallelEnricher enricher;
  protected EnrichmentStrategies strategy;
  private JSONParser parser;
  protected OutputCollector collector;
  private Context stellarContext;
  protected Map<String, EnrichmentAdapter<CacheKey>> enrichmentsByType = new HashMap<>();
  protected Long maxCacheSize;
  protected Long maxTimeRetain;
  protected boolean invalidateCacheOnReload = false;
  protected String messageFieldName;
  protected Integer numThreads = null;

  public UnifiedEnrichmentBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  /**
   * @param enrichments enrichment
   * @return Instance of this class
   */
  public UnifiedEnrichmentBolt withEnrichments(List<Enrichment> enrichments) {
    for(Enrichment e : enrichments) {
      enrichmentsByType.put(e.getType(), e.getAdapter());
    }
    return this;
  }

  public UnifiedEnrichmentBolt withNumThreads(Object numThreads) {
    if(numThreads instanceof Number) {
      this.numThreads = ((Number)numThreads).intValue();
    }
    else if(numThreads instanceof String) {
      String numThreadsStr = ((String)numThreads).trim().toUpperCase();
      if(numThreadsStr.endsWith("C")) {
        Integer factor = Integer.parseInt(numThreadsStr.replace("C", ""));
        this.numThreads = factor*Runtime.getRuntime().availableProcessors();
      }
      else {
        this.numThreads = Integer.parseInt(numThreadsStr);
      }
    }
    if(this.numThreads == null) {
      this.numThreads = 2*Runtime.getRuntime().availableProcessors();
    }
    return this;
  }

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

  public UnifiedEnrichmentBolt withCacheInvalidationOnReload(boolean cacheInvalidationOnReload) {
    this.invalidateCacheOnReload= cacheInvalidationOnReload;
    return this;
  }


  @Override
  public void reloadCallback(String name, ConfigurationType type) {
    if(invalidateCacheOnReload) {
      if(strategy.getCache() != null) {
        strategy.getCache().invalidateAll();
      }
    }
    if(type == ConfigurationType.GLOBAL && enrichmentsByType != null) {
      for(EnrichmentAdapter adapter : enrichmentsByType.values()) {
        adapter.updateAdapter(getConfigurations().getGlobalConfig());
      }
    }
  }


  /**
   * Process a single tuple of input. The Tuple object contains metadata on it
   * about which component/stream/task it came from. The values of the Tuple can
   * be accessed using Tuple#getValue. The IBolt does not have to process the Tuple
   * immediately. It is perfectly fine to hang onto a tuple and process it later
   * (for instance, to do an aggregation or join).
   * <p/>
   * Tuples should be emitted using the OutputCollector provided through the prepare method.
   * It is required that all input tuples are acked or failed at some point using the OutputCollector.
   * Otherwise, Storm will be unable to determine when tuples coming off the spouts
   * have been completed.
   * <p/>
   * For the common case of acking an input tuple at the end of the execute method,
   * see IBasicBolt which automates this.
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
        LOG.error("Unable to find SensorEnrichmentConfig for sourceType: {}", sourceType);
        MetronError metronError = new MetronError()
                .withErrorType(Constants.ErrorType.ENRICHMENT_ERROR)
                .withMessage("Unable to find SensorEnrichmentConfig for sourceType: " + sourceType)
                .addRawMessage(message);
        ErrorUtils.handleError(collector, metronError);
      }
      else {
        config.getConfiguration().putIfAbsent(STELLAR_CONTEXT_CONF, stellarContext);
        String key = getKey(input, message);
        ParallelEnricher.EnrichmentResult result = enricher.apply(message, strategy, config);
        JSONObject enriched = result.getResult();
        enriched = strategy.postProcess(enriched, config, StellarFunctions.FUNCTION_RESOLVER(), stellarContext);
        collector.emit("message",
                input,
                new Values(key, enriched));
        for(Map.Entry<Object, Throwable> t : result.getEnrichmentErrors()) {
          LOG.error("[Metron] Unable to enrich message: {}", message, t);
          MetronError error = new MetronError()
                  .withErrorType(strategy.getErrorType())
                  .withMessage(t.getValue().getMessage())
                  .withThrowable(t.getValue())
                  .addRawMessage(t.getKey());
          ErrorUtils.handleError(collector, error);
        }
      }
    } catch (Exception e) {
      LOG.error("[Metron] Unable to enrich message: {}", message, e);
      MetronError error = new MetronError()
              .withErrorType(strategy.getErrorType())
              .withMessage(e.getMessage())
              .withThrowable(e)
              .addRawMessage(message);
      ErrorUtils.handleError(collector, error);
    }
    finally {
      collector.ack(input);
    }
  }

  public UnifiedEnrichmentBolt withMessageFieldName(String messageFieldName) {
    this.messageFieldName = messageFieldName;
    return this;
  }

  public JSONObject generateMessage(Tuple tuple) {
    JSONObject message = null;
    if (messageFieldName == null) {
      byte[] data = tuple.getBinary(0);
      try {
        message = (JSONObject) parser.parse(new String(data, "UTF8"));
      } catch (ParseException | UnsupportedEncodingException e) {
        throw new IllegalStateException("Unable to parse tuple: " + tuple);
      }
    } else {
      message = (JSONObject) tuple.getValueByField(messageFieldName);
    }
    return message;
  }

  @Override
  public final void prepare(Map map, TopologyContext topologyContext,
                       OutputCollector outputCollector) {
    super.prepare(map, topologyContext, outputCollector);
    parser = new JSONParser();
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
    strategy.initializeThreading(numThreads, maxCacheSize, maxTimeRetain);
    enricher = new ParallelEnricher(enrichmentsByType);
    perfLog = new PerformanceLogger(() -> getConfigurations().getGlobalConfig(), Perf.class.getName());
    GeoLiteDatabase.INSTANCE.update((String)getConfigurations().getGlobalConfig().get(GeoLiteDatabase.GEO_HDFS_FILE));
    initializeStellar();
  }


  protected void initializeStellar() {
    stellarContext = new Context.Builder()
                         .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
                         .with(Context.Capabilities.GLOBAL_CONFIG, () -> getConfigurations().getGlobalConfig())
                         .with(Context.Capabilities.STELLAR_CONFIG, () -> getConfigurations().getGlobalConfig())
                         .build();
    StellarFunctions.initialize(stellarContext);
  }

  public String getKey(Tuple tuple, JSONObject message) {
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
   *
   * @param declarer this is used to declare output stream ids, output fields, and whether or not each output stream is a direct stream
   */
  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream("message", new Fields("key", "message"));
    declarer.declareStream("error", new Fields("message"));
  }
}
