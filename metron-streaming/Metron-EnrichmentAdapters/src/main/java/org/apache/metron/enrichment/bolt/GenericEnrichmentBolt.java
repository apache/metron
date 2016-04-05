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

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.metron.Constants;
import org.apache.metron.bolt.ConfiguredBolt;
import org.apache.metron.domain.Enrichment;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.helpers.topology.ErrorUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Uses an adapter to enrich telemetry messages with additional metadata
 * entries. For a list of available enrichment adapters see
 * org.apache.metron.enrichment.adapters.
 * <p/>
 * At the moment of release the following enrichment adapters are available:
 * <p/>
 * <ul>
 * <p/>
 * <li>geo = attaches geo coordinates to IPs
 * <li>whois = attaches whois information to domains
 * <li>host = attaches reputation information to known hosts
 * <li>CIF = attaches information from threat intelligence feeds
 * <ul>
 * <p/>
 * <p/>
 * Enrichments are optional
 **/

@SuppressWarnings({"rawtypes", "serial"})
public class GenericEnrichmentBolt extends ConfiguredBolt {

  private static final Logger LOG = LoggerFactory
          .getLogger(GenericEnrichmentBolt.class);
  private OutputCollector collector;


  protected String enrichmentType;
  protected EnrichmentAdapter adapter;
  protected transient CacheLoader<String, JSONObject> loader;
  protected transient LoadingCache<String, JSONObject> cache;
  protected Long maxCacheSize;
  protected Long maxTimeRetain;

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

  @Override
  public void prepare(Map conf, TopologyContext topologyContext,
                      OutputCollector collector) {
    this.collector = collector;
    if (this.maxCacheSize == null)
      throw new IllegalStateException("MAX_CACHE_SIZE_OBJECTS_NUM must be specified");
    if (this.maxTimeRetain == null)
      throw new IllegalStateException("MAX_TIME_RETAIN_MINUTES must be specified");
    if (this.adapter == null)
      throw new IllegalStateException("Adapter must be specified");
    loader = new CacheLoader<String, JSONObject>() {
      public JSONObject load(String key) throws Exception {
        return adapter.enrich(key);
      }
    };
    cache = CacheBuilder.newBuilder().maximumSize(maxCacheSize)
            .expireAfterWrite(maxTimeRetain, TimeUnit.MINUTES)
            .build(loader);
    boolean success = adapter.initializeAdapter();
    if (!success) {
      LOG.error("[Metron] EnrichmentSplitterBolt could not initialize adapter");
      throw new IllegalStateException("Could not initialize adapter...");
    }
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream(enrichmentType, new Fields("key", "message"));
    declarer.declareStream("error", new Fields("message"));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    String key = tuple.getStringByField("key");
    JSONObject rawMessage = (JSONObject) tuple.getValueByField("message");
    JSONObject enrichedMessage = new JSONObject();
    enrichedMessage.put("adapter." + adapter.getClass().getSimpleName().toLowerCase() + ".begin.ts", "" + System.currentTimeMillis());
    try {
      if (rawMessage == null || rawMessage.isEmpty())
        throw new Exception("Could not parse binary stream to JSON");
      if (key == null)
        throw new Exception("Key is not valid");
      for (Object o : rawMessage.keySet()) {
        String field = (String) o;
        String value = (String) rawMessage.get(field);
        if (field.equals(Constants.SENSOR_TYPE)) {
          enrichedMessage.put(Constants.SENSOR_TYPE, value);
        } else {
          JSONObject enrichedField = new JSONObject();
          if (value != null && value.length() != 0) {
            adapter.logAccess(value);
            enrichedField = cache.getUnchecked(value);
            if (enrichedField == null)
              throw new Exception("[Metron] Could not enrich string: "
                      + value);
          }
          if (!enrichedField.isEmpty()) {
            for (Object enrichedKey : enrichedField.keySet()) {
              enrichedMessage.put(field + "." + enrichedKey, enrichedField.get(enrichedKey));
            }
          } else {
            enrichedMessage.put(field, "");
          }
        }
      }

      enrichedMessage.put("adapter." + adapter.getClass().getSimpleName().toLowerCase() + ".end.ts", "" + System.currentTimeMillis());
      if (!enrichedMessage.isEmpty()) {
        collector.emit(enrichmentType, new Values(key, enrichedMessage));
      }
    } catch (Exception e) {
      LOG.error("[Metron] Unable to enrich message: " + rawMessage, e);
      JSONObject error = ErrorUtils.generateErrorMessage("Enrichment problem: " + rawMessage, e);
      if (key != null) {
        collector.emit(enrichmentType, new Values(key, enrichedMessage));
      }
      collector.emit("error", new Values(error));
    }
  }

  @Override
  public void cleanup() {
    adapter.cleanup();
  }
}
