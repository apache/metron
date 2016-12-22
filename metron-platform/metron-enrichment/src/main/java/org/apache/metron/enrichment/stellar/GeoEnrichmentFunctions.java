/*
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

import org.apache.log4j.Logger;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.enrichment.adapters.geo.GeoLiteDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GeoEnrichmentFunctions {
  private static final Logger LOG = Logger.getLogger(GeoEnrichmentFunctions.class);

  @Stellar(name="GET"
          ,namespace="GEO"
          ,description="Look up an IPV4 address and returns geographic information about it"
          ,params = {
                      "ip - The IPV4 address to lookup"
                    }
          ,returns = "Map containing GeoIP information if an entry exists and false otherwise"
  )
  public static class GeoGet implements StellarFunction {
    boolean initialized = false;

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(!initialized) {
        return false;
      }
      if(args.size() < 1) {
        throw new IllegalStateException("Requires an IPV4 address");
      }
      int i = 0;
      String ip = (String) args.get(i++);
      if(ip == null) {
        return false;
      }

      Optional<HashMap<String, String>> result = GeoLiteDatabase.INSTANCE.get(ip);
      if(result.isPresent()) {
        return result.get();
      }
      return false;
    }

    @Override
    public void initialize(Context context) {
        LOG.info("Initializing GeoEnrichmentFunctions");
        Map<String, Object> config = getConfig(context);
        String hdfsDir = (String) config.get(GeoLiteDatabase.GEO_HDFS_FILE);
        GeoLiteDatabase.INSTANCE.update(hdfsDir);
        initialized = true;
    }

    private static Map<String, Object> getConfig(Context context) {
      return (Map<String, Object>) context.getCapability(Context.Capabilities.GLOBAL_CONFIG, false).orElse(new HashMap<>());
    }

    @Override
    public boolean isInitialized() {
      return initialized;
    }

  }
}
