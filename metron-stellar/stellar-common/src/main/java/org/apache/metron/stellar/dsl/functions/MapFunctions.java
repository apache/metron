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

package org.apache.metron.stellar.dsl.functions;

import com.google.common.collect.Iterables;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

public class MapFunctions {

  @Stellar(name="EXISTS"
          ,namespace="MAP"
          , description="Checks for existence of a key in a map."
          , params = {
                      "key - The key to check for existence"
                     ,"map - The map to check for existence of the key"
                     }
          , returns = "True if the key is found in the map and false if otherwise."
          )
  public static class MapExists extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if(list.size() < 2) {
        return false;
      }
      Object key = list.get(0);
      Object mapObj = list.get(1);
      if(key != null && mapObj != null && mapObj instanceof Map) {
        return ((Map)mapObj).containsKey(key);
      }
      return false;
    }
  }

  @Stellar(name="GET"
      ,namespace="MAP"
      , description="Gets the value associated with a key from a map"
      , params = {
      "key - The key"
      ,"map - The map"
      ,"default - Optionally the default value to return if the key is not in the map."
  }
      , returns = "The object associated with the key in the map.  If no value is associated with the key and default is specified, then default is returned. If no value is associated with the key or default, then null is returned."
  )
  public static class MapGet extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> objects) {
      Object keyObj = objects.get(0);
      Object mapObj = objects.get(1);
      Object defaultObj = null;
      if(objects.size() >= 3) {
        defaultObj = objects.get(2);
      }
      if(keyObj == null || mapObj == null) {
        return defaultObj;
      }
      Map<Object, Object> map = (Map)mapObj;
      Object ret = map.get(keyObj);
      if(ret == null && defaultObj != null) {
        return defaultObj;
      }
      return ret;
    }
  }

  @Stellar(name = "PUT",
           namespace = "MAP",
           description = "Adds a key/value pair to a map",
           params = {
                      "key - The key",
                      "value - The value",
                      "map - The map to perform the put on"
                    },
           returns = "The original map modified with the key/value. If the map argument is null, a new map will be created and returned that contains the provided key and value - note: if the 'map' argument is null, only the returned map will be non-null and contain the key/value."
          )
  public static class MapPut extends BaseStellarFunction {

    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> objects) {
      if (objects.size() < 3) {
        throw new IllegalArgumentException("Must pass a key, value, and map");
      } else {
        Object keyObj = objects.get(0);
        Object valueObj = objects.get(1);
        Object mapObj = objects.get(2);
        if (mapObj == null) {
          mapObj = new HashMap<>();
        }
        Map<Object, Object> map = (Map) mapObj;
        map.put(keyObj, valueObj);
        return map;
      }
    }
  }

  @Stellar(name = "MERGE",
           namespace = "MAP",
           description = "Merges a list of maps",
           params = {"maps - A collection of maps to merge. Last entry wins for overlapping keys."},
           returns = "A Map. null if the list of maps is empty."
          )
  public static class MapMerge extends BaseStellarFunction {

    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      if (list.size() < 1) {
        return null;
      }
      LinkedHashMap<Object, Object> ret = new LinkedHashMap<>();
      Object o = list.get(0);
      if (o != null) {
        if (!(o instanceof Iterable)) {
          throw new IllegalArgumentException("Expected an Iterable, but " + o + " is of type " + o.getClass());
        }
        Iterable<? extends Map> maps = (Iterable<? extends Map>) o;

        if (Iterables.size(maps) == 1) {
          return Iterables.getFirst(maps, null);
        }

        for (Map m : maps) {
          if (m != null) {
            ret.putAll(m);
          }
        }
      }
      return ret;
    }
  }
}
