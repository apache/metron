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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public enum Filters {
   BRO(BroMessageFilter.class)
  ,QUERY(QueryFilter.class)
  ,DEFAULT(GenericMessageFilter.class)
  ;
  Class<? extends AbstractMessageFilter> clazz;
  Filters(Class<? extends AbstractMessageFilter> clazz) {
    this.clazz = clazz;
  }
  public static AbstractMessageFilter get(String filterName, Map<String, Object> config) {
    if(filterName == null || filterName.trim().isEmpty()) {
      return new GenericMessageFilter(config);
    }
    Class<? extends AbstractMessageFilter> filterClass;
    try {
      Filters f = Filters.valueOf(filterName);
      filterClass = f.clazz;
    }
    catch(Exception ex) {
      try {
        filterClass = (Class<? extends AbstractMessageFilter>) Class.forName(filterName);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("Unable to find class " + filterName, e);
      }
    }
    Constructor<?> cons = null;
    try {
      cons = filterClass.getConstructor(Map.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Unable to find constructor for class " + filterName, e);
    }
    try {
      return (AbstractMessageFilter) cons.newInstance(config);
    } catch (InstantiationException e) {
      throw new IllegalStateException("Unable to instantiate class " + filterName, e);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Unable to instantiate class " + filterName, e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException("Unable to instantiate class " + filterName, e);
    }
  }
}
