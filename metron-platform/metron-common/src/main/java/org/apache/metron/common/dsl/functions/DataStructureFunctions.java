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

import org.apache.metron.common.dsl.BaseStellarFunction;
import org.apache.metron.common.dsl.Stellar;

import java.util.Collection;
import java.util.List;

public class DataStructureFunctions {
  @Stellar(name="IS_EMPTY"
          , description="Returns true if string or collection is empty and false otherwise"
          , params = { "input - Object of string or collection type (e.g. list)"}
          , returns = "Boolean"
          )
  public static class IsEmpty extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if(list.size() == 0) {
        throw new IllegalStateException("IS_EMPTY expects one string arg");
      }
      Object o = list.get(0);
      if(o instanceof Collection) {
        return ((Collection)o).isEmpty();
      }
      else if(o instanceof String) {
        String val = (String) list.get(0);
        return val == null || val.isEmpty() ? true : false;
      }
      else {
        throw new IllegalStateException("IS_EMPTY expects a collection or string");
      }
    }
  }
}
