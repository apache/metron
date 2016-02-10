/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import backtype.storm.topology.base.BaseRichBolt;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.metron.domain.Enrichment;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.apache.metron.helpers.topology.ErrorGenerator;

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
public class GenericEnrichmentBolt extends BaseRichBolt {

  private static final Logger LOG = LoggerFactory
          .getLogger(GenericEnrichmentBolt.class);
  private OutputCollector collector;


  protected String streamId;
  protected Enrichment<EnrichmentAdapter> enrichment;
  protected EnrichmentAdapter adapter;
  protected transient CacheLoader<String, JSONObject> loader;
  protected transient LoadingCache<String, JSONObject> cache;
  protected Long maxCacheSize;
  protected Long maxTimeRetain;


  /**
   * @param enrichment Object holding enrichment metadata
   * @return Instance of this class
   */

  public GenericEnrichmentBolt withEnrichment
  (Enrichment<EnrichmentAdapter> enrichment) {
    this.streamId = enrichment.getName();
    this.enrichment = enrichment;
    this.adapter = this.enrichment.getAdapter();
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
    if (this.enrichment == null)
      throw new IllegalStateException("enrichment must be specified");
    if (this.maxCacheSize == null)
      throw new IllegalStateException("MAX_CACHE_SIZE_OBJECTS_NUM must be specified");
    if (this.maxTimeRetain == null)
      throw new IllegalStateException("MAX_TIME_RETAIN_MINUTES must be specified");
    if (this.adapter == null)
      throw new IllegalStateException("Adapter must be specified");
    if (this.enrichment.getFields() == null)
      throw new IllegalStateException(
              "Fields to be enriched must be specified");
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
      LOG.error("[Metron] EnrichmentBolt could not initialize adapter");
      throw new IllegalStateException("Could not initialize adapter...");
    }
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declearer) {
    declearer.declareStream(streamId, new Fields("key", "message"));
    declearer.declareStream("error", new Fields("message"));
  }


  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    String key = tuple.getStringByField("key");
    JSONObject rawMessage = (JSONObject) tuple.getValueByField("message");
    JSONObject enrichedMessage = new JSONObject();
    try {
      if (rawMessage == null || rawMessage.isEmpty())
        throw new Exception("Could not parse binary stream to JSON");
      if (key == null)
        throw new Exception("Key is not valid");
      for (String field : enrichment.getFields()) {
        JSONObject enrichedField = new JSONObject();
        String value = (String) rawMessage.get(field);
        if (value != null && value.length() != 0) {
          enrichedField = cache.getUnchecked(value);
          if (enrichedField == null)
            throw new Exception("[Metron] Could not enrich string: "
                    + value);
        }
        enrichedMessage.put(field, enrichedField);
      }
      if (!enrichedMessage.isEmpty()) {
        collector.emit(streamId, new Values(key, enrichedMessage));
      }
    } catch (Exception e) {
      LOG.error("[Metron] Unable to enrich message: " + rawMessage);
      JSONObject error = ErrorGenerator.generateErrorMessage("Enrichment problem: " + rawMessage, e);
      if (key != null) {
        collector.emit(streamId, new Values(key, enrichedMessage));
      }
      collector.emit("error", new Values(error));
    }
  }

  @Override
  public void cleanup() {
    adapter.cleanup();
  }
}