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
package org.apache.metron.enrichment.adapters.stellar;

import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.metron.enrichment.bolt.GenericEnrichmentBolt.STELLAR_CONTEXT_CONF;

public class StellarAdapter implements EnrichmentAdapter<CacheKey>,Serializable {
  public static class Perf {}
  protected static final Logger _LOG = LoggerFactory.getLogger(StellarAdapter.class);
  protected static final Logger _PERF_LOG = LoggerFactory.getLogger(Perf.class);
  public static final String STELLAR_SLOW_LOG = "stellar.slow.threshold.ms";
  public static final Long STELLAR_SLOW_LOG_DEFAULT = 1000l;

  private enum EnrichmentType implements Function<SensorEnrichmentConfig, ConfigHandler>{
    ENRICHMENT(config -> config.getEnrichment().getEnrichmentConfigs().get("stellar"))
    ,THREAT_INTEL(config -> config.getThreatIntel().getEnrichmentConfigs().get("stellar"))
    ;
    Function<SensorEnrichmentConfig, ConfigHandler> func;
    EnrichmentType(Function<SensorEnrichmentConfig, ConfigHandler> func) {
      this.func = func;
    }

    @Override
    public ConfigHandler apply(SensorEnrichmentConfig cacheKey) {
      return func.apply(cacheKey);
    }
  }
  transient Function<SensorEnrichmentConfig, ConfigHandler> getHandler;
  private String enrichmentType;
  public StellarAdapter ofType(String enrichmentType) {
    this.enrichmentType = enrichmentType;
    return this;
  }
  @Override
	public String getOutputPrefix(CacheKey value) {
		return "";
	}

  @Override
  public void logAccess(CacheKey value) {

  }

  @Override
  public String getStreamSubGroup(String enrichmentType, String field) {
    return field;
  }

  public static Iterable<Map.Entry<String, Object>> getStellarStatements(ConfigHandler handler, String field) {
    if(field.length() == 0) {
      return handler.getType().toConfig(handler.getConfig());
    }
    else {
      Map<String, Object> groupStatements = (Map<String, Object>)handler.getConfig();
      return handler.getType().toConfig(groupStatements.get(field));
    }
  }

  public static JSONObject process( Map<String, Object> message
                                           , ConfigHandler handler
                                           , String field
                                           , Long slowLogThreshold
                                           , StellarProcessor processor
                                           , VariableResolver resolver
                                           , Context stellarContext
                                           )
  {
    JSONObject ret = new JSONObject();
    Iterable<Map.Entry<String, Object>> stellarStatements = getStellarStatements(handler, field);

    _LOG.debug("message := {}", message);
    if(stellarStatements != null) {
      List<String> mapEntries = new ArrayList<>();
      for (Map.Entry<String, Object> kv : stellarStatements) {
        if(kv.getKey() != null && kv.getValue() != null) {
          if (kv.getValue() instanceof String) {
            long startTime = System.currentTimeMillis();
            String stellarStatement = (String) kv.getValue();
            Object o = processor.parse(stellarStatement, resolver, StellarFunctions.FUNCTION_RESOLVER(), stellarContext);
            if (slowLogThreshold != null && _PERF_LOG.isDebugEnabled()) {
              long duration = System.currentTimeMillis() - startTime;
              if (duration > slowLogThreshold) {
                _PERF_LOG.debug("SLOW LOG: " + stellarStatement + " took" + duration + "ms");
              }
            }
            _LOG.debug("{} := {} yields {}", kv.getKey(), stellarStatement , o);
            if (o != null && o instanceof Map) {
              mapEntries.add(kv.getKey());
            }
            if(o == null) {
              message.remove(kv.getKey());
              ret.remove(kv.getKey());
            }
            else {
              message.put(kv.getKey(), o);
              ret.put(kv.getKey(), o);
            }
          }
        }
      }
      /*
      We need to handle the map entries separately now.
      We want to explode them out, so if "var" is
      {
        "foo" : "bar"
      }
      then we want "var.foo" == "bar"
      and no "var"
       */
      for(String mapEntry : mapEntries) {
        String key = mapEntry;
        Map<Object, Object> value = (Map<Object, Object>) ret.get(key);
        if(value != null) {
          _LOG.debug("Exploding map: {} == {}", key, value);
          for (Map.Entry<Object, Object> valueKv : value.entrySet()) {
            String newKey = ((key.length() > 0) ? key + "." : "") + valueKv.getKey();
            ret.put(newKey, valueKv.getValue());
          }
          //removing the map from downstream
          ret.remove(key);
        }
      }
    }
    return ret;
  }

  @Override
  public JSONObject enrich(CacheKey value) {
    Context stellarContext = (Context) value.getConfig().getConfiguration().get(STELLAR_CONTEXT_CONF);
    ConfigHandler handler = getHandler.apply(value.getConfig());
    Map<String, Object> globalConfig = value.getConfig().getConfiguration();
    Map<String, Object> sensorConfig = value.getConfig().getEnrichment().getConfig();
    if(handler == null) {
      _LOG.trace("Stellar ConfigHandler is null.");
      return new JSONObject();
    }
    Long slowLogThreshold = null;
    if(_PERF_LOG.isDebugEnabled()) {
      slowLogThreshold = ConversionUtils.convert(globalConfig.getOrDefault(STELLAR_SLOW_LOG, STELLAR_SLOW_LOG_DEFAULT), Long.class);
    }
    Map<String, Object> message = value.getValue(Map.class);
    VariableResolver resolver = new MapVariableResolver(message, sensorConfig, globalConfig);
    StellarProcessor processor = new StellarProcessor();
    JSONObject enriched = process(message
                                 , handler
                                 , value.getField()
                                 , slowLogThreshold
                                 , processor
                                 , resolver
                                 , stellarContext
                                 );
    _LOG.trace("Stellar Enrichment Success: {}", enriched);
    return enriched;
  }

  @Override
  public boolean initializeAdapter(Map<String, Object> config) {
    getHandler = EnrichmentType.valueOf(enrichmentType);
    return true;
  }

  @Override
  public void updateAdapter(Map<String, Object> config) {
  }

  @Override
  public void cleanup() {

  }
}
