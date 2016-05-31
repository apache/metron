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

package org.apache.metron.parsers.filters;

import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.json.simple.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public enum Filters {
   BRO(BroMessageFilter.class)
  ,QUERY(QueryFilter.class)
  ,DEFAULT(GenericMessageFilter.class)
  ;
  Class<? extends MessageFilter> clazz;
  Filters(Class<? extends MessageFilter> clazz) {
    this.clazz = clazz;
  }
  public static MessageFilter<JSONObject> get(String filterName, Map<String, Object> config) {
    if(filterName == null || filterName.trim().isEmpty()) {
      return new GenericMessageFilter();
    }
    Class<? extends MessageFilter> filterClass;
    try {
      Filters f = Filters.valueOf(filterName);
      filterClass = f.clazz;
    }
    catch(Exception ex) {
      try {
        filterClass = (Class<? extends MessageFilter>) Class.forName(filterName);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("Unable to find class " + filterName, e);
      }
    }
    MessageFilter<JSONObject> filter = ReflectionUtils.createInstance(filterClass);
    filter.configure(config);
    return filter;
  }
}
