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

package org.apache.metron.common.dsl.functions;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MapFunctions {

  /**
   * Get the value of a key from a map.
   *
   *  MAP_GET (key, map, [default-value])
   */
  public static Function<List<Object>, Object> MapGet = args -> {
    Object keyObj = args.get(0);
    Object mapObj = args.get(1);

    Object defaultVal = null;
    if(args.size() >= 3) {
      defaultVal = args.get(2);
    }

    if(keyObj == null || mapObj == null) {
      return defaultVal;
    }

    Map<Object, Object> map = (Map)mapObj;
    Object value = map.get(keyObj);
    return (value == null && defaultVal != null) ? defaultVal : value;
  };

  /**
   * Does a key exist in a map?
   *
   *  MAP_EXISTS (key, map)
   */
  public static Function<List<Object>, Object> MapExists = args -> {
    if (args.size() < 2) {
      return false;
    }

    Object key = args.get(0);
    Object mapObj = args.get(1);

    return (key != null && mapObj != null && mapObj instanceof Map) ? ((Map) mapObj).containsKey(key) : false;
  };
}
