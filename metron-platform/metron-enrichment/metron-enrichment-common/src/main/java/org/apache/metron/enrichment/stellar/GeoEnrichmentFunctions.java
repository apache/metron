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

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.enrichment.adapters.maxmind.geo.GeoLiteCityDatabase;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoEnrichmentFunctions {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Stellar(name="GET"
          ,namespace="GEO"
          ,description="Look up an IPV4 address and returns geographic information about it"
          ,params = {
                      "ip - The IPV4 address to lookup",
                      "fields - Optional list of GeoIP fields to grab. Options are locID, country, city, postalCode, dmaCode, latitude, longitude, location_point"
                    }
          ,returns = "If a Single field is requested a string of the field, If multiple fields a map of string of the fields, and null otherwise"
  )
  public static class GeoGet implements StellarFunction {
    boolean initialized = false;

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(!initialized) {
        return null;
      }
      if(args.size() > 2) {
        throw new IllegalArgumentException("GEO_GET received more arguments than expected: " + args.size());
      }

      if(args.size() == 1 && args.get(0) instanceof String) {
        // If no fields are provided, return everything
        String ip = (String) args.get(0);
        if(ip == null || ip.trim().isEmpty()) {
          return null;
        }

        Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get(ip);
        return result.orElse(Collections.emptyMap());
      } else if (args.size() == 2 && args.get(1) instanceof List) {
        // If fields are provided, return just those fields.
        String ip = (String) args.get(0);
        @SuppressWarnings("unchecked")
        List<String> fields = (List) args.get(1);
        Optional<Map<String, String>> result = GeoLiteCityDatabase.INSTANCE.get(ip);

        // If only one field is requested, just return it directly
        if(fields.size() == 1 && result.isPresent()) {
          return result.get().get(fields.get(0));
        } else if (result.isPresent()) {
          // If multiple fields are requested, return all of them
          Map<String, String> filteredInfo = new HashMap<>();
          for(String field : fields) {
            Map<String, String> geoInfo = result.get();
            filteredInfo.put(field, geoInfo.get(field));
          }
          return filteredInfo;
        }
      }

      return null;
    }

    @Override
    public void initialize(Context context) {
        LOG.info("Initializing GeoEnrichmentFunctions");
        Map<String, Object> config = getConfig(context);
        String hdfsDir = (String) config.get(GeoLiteCityDatabase.GEO_HDFS_FILE);
        GeoLiteCityDatabase.INSTANCE.update(hdfsDir);
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
