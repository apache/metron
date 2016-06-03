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
  public static class MapGet implements Function<List<Object>, Object> {
    @Override
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
}
