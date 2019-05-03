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

package org.apache.metron.enrichment.stellar;

import org.apache.metron.enrichment.cache.ObjectCache;
import org.apache.metron.enrichment.cache.ObjectCacheConfig;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Stellar(namespace="ENRICHMENT"
        ,name="IN_MEMORY_GET"
        ,description="Retrieve and deserialize a serialized object from HDFS and stores it in the ObjectCache,  " +
        "then returns the value associated with the indicator."
        , params = {
            "path - The path in HDFS to the serialized object" +
            "indicator - The string indicator to look up"
          }
        , returns="Value associated with the indicator."
)
public class InMemoryEnrichmentGet implements StellarFunction {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public final static String IN_MEMORY_ENRICHMENT_SETTINGS = "in.memory.enrichment.settings";
  public final static String IN_MEMORY_CACHE_SIZE = "cache.size";
  public final static String IN_MEMORY_CACHE_EXPIRATION = "cache.expiration";
  public final static String IN_MEMORY_TIME_UNIT = "cache.time.unit";

  private ObjectCache objectCache;

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    if(args.size() != 2) {
      throw new IllegalArgumentException("All parameters are mandatory, submit 'hdfs path', 'indicator'");
    }
    if(!isInitialized()) {
      return null;
    }

    String path = (String) args.get(0);
    String indicator = (String) args.get(1);
    if(path == null || indicator == null) {
      return null;
    }

    Object value;
    try {
      Map cachedMap = (Map) objectCache.get(path);
      LOG.debug("Looking up value from object at path '{}' using indicator {}", path, indicator);
      value = cachedMap.get(indicator);
    } catch(ClassCastException e) {
      throw new ClassCastException(String.format("The object stored in HDFS at '%s' must be serialized in JSON format.", path));
    }

    return value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(Context context) {
    Map<String, Object> config = (Map<String, Object>) context.getCapability(Context.Capabilities.GLOBAL_CONFIG, false)
            .orElse(new HashMap<>());
    ObjectCacheConfig objectCacheConfig = ObjectCacheConfig.fromGlobalConfig(config);
    if (config.containsKey(IN_MEMORY_ENRICHMENT_SETTINGS)) {
      Map<String, Object> inMemoryEnrichmentConfig = (Map<String, Object>) config.get(IN_MEMORY_ENRICHMENT_SETTINGS);
      if (inMemoryEnrichmentConfig.containsKey(IN_MEMORY_CACHE_SIZE)) {
        objectCacheConfig.setCacheSize(ConversionUtils.convert(inMemoryEnrichmentConfig.get(IN_MEMORY_CACHE_SIZE), Long.class));
      }
      if (inMemoryEnrichmentConfig.containsKey(IN_MEMORY_CACHE_EXPIRATION)) {
        objectCacheConfig.setCacheExpiration(ConversionUtils.convert(inMemoryEnrichmentConfig.get(IN_MEMORY_CACHE_EXPIRATION), Long.class));
      }
      if (inMemoryEnrichmentConfig.containsKey(IN_MEMORY_TIME_UNIT)) {
        objectCacheConfig.setTimeUnit(TimeUnit.valueOf((String) inMemoryEnrichmentConfig.get(IN_MEMORY_TIME_UNIT)));
      }
    }
    objectCache = new ObjectCache();
    objectCache.initialize(objectCacheConfig);
  }

  @Override
  public boolean isInitialized() {
    return objectCache != null && objectCache.isInitialized();
  }
}
