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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.bolt.ConfiguredEnrichmentBolt;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.performance.PerformanceLogger;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.enrichment.configuration.Enrichment;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses an adapter to enrich telemetry messages with additional metadata
 * entries. For a list of available enrichment adapters see
 * {@link org.apache.metron.enrichment.adapters}.
 * <p>
 * At the moment of release the following enrichment adapters are available:
 * <p>
 * <ul>
 * <li>geo = attaches geo coordinates to IPs
 * <li>whois = attaches whois information to domains
 * <li>host = attaches reputation information to known hosts
 * <li>CIF = attaches information from threat intelligence feeds
 * </ul>
 * <p>
 * Enrichments are optional
 **/

@SuppressWarnings({"rawtypes", "serial"})
public class GenericEnrichmentBolt extends ConfiguredEnrichmentBolt {
  public static class Perf {} // used for performance logging
  private PerformanceLogger perfLog; // not static bc multiple bolts may exist in same worker
  private static final Logger LOG = LoggerFactory
          .getLogger(GenericEnrichmentBolt.class);
  public static final String STELLAR_CONTEXT_CONF = "stellarContext";
  private static final String ERROR_STREAM = "error";
  private OutputCollector collector;
  private Context stellarContext;
  protected String enrichmentType;
  protected EnrichmentAdapter<CacheKey> adapter;
  protected transient CacheLoader<CacheKey, JSONObject> loader;
  protected transient LoadingCache<CacheKey, JSONObject> cache;
  protected Long maxCacheSize;
  protected Long maxTimeRetain;
  protected boolean invalidateCacheOnReload = false;

  public GenericEnrichmentBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  /**
   * @param enrichment enrichment
   * @return Instance of this class
   */

  public GenericEnrichmentBolt withEnrichment(Enrichment enrichment) {
    this.enrichmentType = enrichment.getType();
    this.adapter = enrichment.getAdapter();
    return this;
  }

  /**
   * @param maxCacheSize Maximum size of cache before flushing
   * @return Instance of this class
   */

  public GenericEnrichmentBolt withMaxCacheSize(long maxCacheSize) {
    this.maxCacheSize = maxCacheSize;
    return this;
  }

  /**
   * @param maxTimeRetain Maximum time to retain cached entry before expiring
   * @return Instance of this class
   */

  public GenericEnrichmentBolt withMaxTimeRetain(long maxTimeRetain) {
    this.maxTimeRetain = maxTimeRetain;
    return this;
  }

  public GenericEnrichmentBolt withCacheInvalidationOnReload(boolean cacheInvalidationOnReload) {
    this.invalidateCacheOnReload= cacheInvalidationOnReload;
    return this;
  }


  @Override
  public void reloadCallback(String name, ConfigurationType type) {
    if(invalidateCacheOnReload) {
      if (cache != null) {
        cache.invalidateAll();
      }
    }
    if(type == ConfigurationType.GLOBAL) {
      adapter.updateAdapter(getConfigurations().getGlobalConfig());
    }
  }

  @Override
  public void prepare(Map conf, TopologyContext topologyContext,
                      OutputCollector collector) {
    super.prepare(conf, topologyContext, collector);
    this.collector = collector;
    if (this.maxCacheSize == null)
      throw new IllegalStateException("MAX_CACHE_SIZE_OBJECTS_NUM must be specified");
    if (this.maxTimeRetain == null)
      throw new IllegalStateException("MAX_TIME_RETAIN_MINUTES must be specified");
    if (this.adapter == null)
      throw new IllegalStateException("Adapter must be specified");
    loader = new CacheLoader<CacheKey, JSONObject>() {
      @Override
      public JSONObject load(CacheKey key) throws Exception {
        return adapter.enrich(key);
      }
    };
    cache = CacheBuilder.newBuilder().maximumSize(maxCacheSize)
            .expireAfterWrite(maxTimeRetain, TimeUnit.MINUTES)
            .build(loader);
    boolean success = adapter.initializeAdapter(getConfigurations().getGlobalConfig());
    if (!success) {
      LOG.error("[Metron] GenericEnrichmentBolt could not initialize adapter");
      throw new IllegalStateException("Could not initialize adapter...");
    }
    perfLog = new PerformanceLogger(() -> getConfigurations().getGlobalConfig(), GenericEnrichmentBolt.Perf.class.getName());
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

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream(enrichmentType, new Fields("key", "message", "subgroup"));
    declarer.declareStream(ERROR_STREAM, new Fields("message"));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    perfLog.mark("execute");
    String key = tuple.getStringByField("key");
    JSONObject rawMessage = (JSONObject) tuple.getValueByField("message");
    String subGroup = "";

    JSONObject enrichedMessage = new JSONObject();
    enrichedMessage.put("adapter." + adapter.getClass().getSimpleName().toLowerCase() + ".begin.ts", "" + System.currentTimeMillis());
    try {
      if (rawMessage == null || rawMessage.isEmpty())
        throw new Exception("Could not parse binary stream to JSON");
      if (key == null)
        throw new Exception("Key is not valid");
      String sourceType = null;
      if(rawMessage.containsKey(Constants.SENSOR_TYPE)) {
        sourceType = rawMessage.get(Constants.SENSOR_TYPE).toString();
      }
      else {
        throw new RuntimeException("Source type is missing from enrichment fragment: " + rawMessage.toJSONString());
      }
      String prefix = null;
      for (Object o : rawMessage.keySet()) {
        String field = (String) o;
        Object value =  rawMessage.get(field);
        if (field.equals(Constants.SENSOR_TYPE)) {
          enrichedMessage.put(Constants.SENSOR_TYPE, value);
        } else {
          JSONObject enrichedField = new JSONObject();
          if (value != null) {
            SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sourceType);
            if(config == null) {
              LOG.error("Unable to find SensorEnrichmentConfig for sourceType: " + sourceType);
              MetronError metronError = new MetronError()
                      .withErrorType(Constants.ErrorType.ENRICHMENT_ERROR)
                      .withMessage("Unable to find SensorEnrichmentConfig for sourceType: " + sourceType)
                      .addRawMessage(rawMessage);
              ErrorUtils.handleError(collector, metronError);
              continue;
            }
            config.getConfiguration().putIfAbsent(STELLAR_CONTEXT_CONF, stellarContext);
            CacheKey cacheKey= new CacheKey(field, value, config);
            try {
              adapter.logAccess(cacheKey);
              prefix = adapter.getOutputPrefix(cacheKey);
              subGroup = adapter.getStreamSubGroup(enrichmentType, field);

              perfLog.mark("enrich");
              enrichedField = cache.getUnchecked(cacheKey);
              perfLog.log("enrich", "key={}, time to run enrichment type={}", key, enrichmentType);

              if (enrichedField == null)
                throw new Exception("[Metron] Could not enrich string: "
                        + value);
            }
            catch(Exception e) {
              LOG.error(e.getMessage(), e);
              MetronError metronError = new MetronError()
                      .withErrorType(Constants.ErrorType.ENRICHMENT_ERROR)
                      .withThrowable(e)
                      .withErrorFields(new HashSet() {{ add(field); }})
                      .addRawMessage(rawMessage);
              ErrorUtils.handleError(collector, metronError);
              continue;
            }
          }
          if ( !enrichedField.isEmpty()) {
            for (Object enrichedKey : enrichedField.keySet()) {
              if(!StringUtils.isEmpty(prefix)) {
                enrichedMessage.put(field + "." + enrichedKey, enrichedField.get(enrichedKey));
              }
              else {
                enrichedMessage.put(enrichedKey, enrichedField.get(enrichedKey));
              }
            }
          }
        }
      }

      enrichedMessage.put("adapter." + adapter.getClass().getSimpleName().toLowerCase() + ".end.ts", "" + System.currentTimeMillis());
      if (!enrichedMessage.isEmpty()) {
        collector.emit(enrichmentType, new Values(key, enrichedMessage, subGroup));
      }
    } catch (Exception e) {
      handleError(key, rawMessage, subGroup, enrichedMessage, e);
    }
    perfLog.log("execute", "key={}, elapsed time to run execute", key);
  }

  // Made protected to allow for error testing in integration test. Directly flaws inputs while everything is functioning hits other
  // errors, so this is made available in order to ensure ERROR_STREAM is output properly.
  protected void handleError(String key, JSONObject rawMessage, String subGroup, JSONObject enrichedMessage, Exception e) {
    LOG.error("[Metron] Unable to enrich message: " + rawMessage, e);
    if (key != null) {
      collector.emit(enrichmentType, new Values(key, enrichedMessage, subGroup));
    }
    MetronError error = new MetronError()
            .withErrorType(Constants.ErrorType.ENRICHMENT_ERROR)
            .withThrowable(e)
            .addRawMessage(rawMessage);
    ErrorUtils.handleError(collector, error);
  }

  @Override
  public void cleanup() {
    adapter.cleanup();
  }

  public Context getStellarContext() {
    return stellarContext;
  }
}
