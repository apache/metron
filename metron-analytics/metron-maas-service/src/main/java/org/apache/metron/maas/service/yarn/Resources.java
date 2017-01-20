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
package org.apache.metron.maas.service.yarn;

import org.apache.hadoop.yarn.api.records.Resource;

import java.util.AbstractMap;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public enum Resources {

    MEMORY( resource -> resource.getMemory() )
  , V_CORE( resource -> resource.getVirtualCores())
  ;

  private Function<Resource, Integer> callback;
  Resources(Function<Resource, Integer> resourcesCallback) {
    this.callback = resourcesCallback;
  }
  public static EnumMap<Resources, Integer> getRealisticResourceRequest( EnumMap<Resources, Integer> requestedResources
                                                               , Resource resource
                                                               )
  {
    EnumMap<Resources, Integer> ret = new EnumMap<>(Resources.class);
    for(Resources r : values()) {
      Integer request = requestedResources.get(r);
      int resourceAmt = r.callback.apply(resource);
      if(request == null || request < 0) {
          ret.put(r, resourceAmt);
      }
      else {
        ret.put(r, Math.min(resourceAmt, request));
      }
    }
    return ret;
  }
  public Map.Entry<Resources, Integer> of(int n) {
    return new AbstractMap.SimpleEntry<>(this, n);
  }
  @SafeVarargs
  public static EnumMap<Resources, Integer> toResourceMap( Map.Entry<Resources, Integer>... entry ) {
    EnumMap<Resources, Integer> ret = new EnumMap<>(Resources.class);
    for(Map.Entry<Resources, Integer> kv : entry) {
      ret.put(kv.getKey(), kv.getValue());
    }
    return ret;
  }
  public static Resource toResource(EnumMap<Resources, Integer> resourceMap) {
    return Resource.newInstance(resourceMap.get(Resources.MEMORY), resourceMap.get(Resources.V_CORE));
  }
}
