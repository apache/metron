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

@Stellar(namespace="OBJECT"
        ,name="GET"
        ,description="Retrieve and deserialize a serialized object from HDFS.  " +
        "The cache can be specified via three properties in the global config: " +
        "\"" + ObjectCacheConfig.OBJECT_CACHE_SIZE_KEY + "\" (default " + ObjectCacheConfig.OBJECT_CACHE_SIZE_DEFAULT + ")," +
        "\"" + ObjectCacheConfig.OBJECT_CACHE_EXPIRATION_KEY + "\" (default " + ObjectCacheConfig.OBJECT_CACHE_EXPIRATION_MIN_DEFAULT + ")," +
        "\"" + ObjectCacheConfig.OBJECT_CACHE_TIME_UNIT_KEY+ "\" (default MINUTES)." +
        "Note, if these are changed in global config, topology restart is required."
        , params = {
            "path - The path in HDFS to the serialized object"
          }
        , returns="The deserialized object."
)
public class ObjectGet implements StellarFunction {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ObjectCache objectCache;

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    if(!isInitialized()) {
      return null;
    }
    if(args.size() < 1) {
      return null;
    }
    Object o = args.get(0);
    if(o == null) {
      return null;
    }
    if(o instanceof String) {
      return objectCache.get((String) o);
    }
    else {
      throw new IllegalStateException("Unable to retrieve " + o + " as it is not a path");
    }
  }

  @Override
  public void initialize(Context context) {
    Map<String, Object> config = getConfig(context);
    objectCache = new ObjectCache();
    objectCache.initialize(new ObjectCacheConfig(config));
  }

  @Override
  public boolean isInitialized() {
    return objectCache != null && objectCache.isInitialized();
  }

  protected Map<String, Object> getConfig(Context context) {
      return (Map<String, Object>) context.getCapability(Context.Capabilities.GLOBAL_CONFIG, false).orElse(new HashMap<>());
    }
}
