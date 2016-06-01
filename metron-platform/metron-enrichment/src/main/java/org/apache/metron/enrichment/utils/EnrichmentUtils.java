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
package org.apache.metron.enrichment.utils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.handler.KeyWithContext;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import sun.management.Sensor;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class EnrichmentUtils {

  public static final String KEY_PREFIX = "enrichments";

  public static String getEnrichmentKey(String enrichmentName, String field) {
    return Joiner.on(".").join(new String[]{KEY_PREFIX, enrichmentName, field});
  }

  public static class TypeToKey implements Function<String, KeyWithContext<EnrichmentKey, EnrichmentLookup.HBaseContext>> {
    private final String indicator;
    private final EnrichmentConfig config;
    private final HTableInterface table;
    public TypeToKey(String indicator, HTableInterface table, EnrichmentConfig config) {
      this.indicator = indicator;
      this.config = config;
      this.table = table;
    }
    @Nullable
    @Override
    public KeyWithContext<EnrichmentKey, EnrichmentLookup.HBaseContext> apply(@Nullable String enrichmentType) {
      EnrichmentKey key = new EnrichmentKey(enrichmentType, indicator);
      EnrichmentLookup.HBaseContext context = new EnrichmentLookup.HBaseContext(table, getColumnFamily(enrichmentType, config));
      return new KeyWithContext<>(key, context);
    }
  }
  private static ThreadLocal<Map<Object, Map<String, String>>> typeToCFs = new ThreadLocal<Map<Object, Map<String, String>>>() {
    @Override
    protected Map<Object, Map<String, String>>initialValue() {
      return new HashMap<>();
    }
  };

  public static final String TYPE_TO_COLUMN_FAMILY_CONF = "typeToColumnFamily";
  public static String getColumnFamily(String enrichmentType, EnrichmentConfig config) {
    Object o = config.getConfig().get(TYPE_TO_COLUMN_FAMILY_CONF);
    if(o == null) {
      return null;
    }
    else {
      Map<String, String> cfMap = typeToCFs.get().get(o);
      if(cfMap == null) {
        cfMap = new HashMap<>();
        if(o instanceof Map) {
          Map map = (Map) o;
          for(Object key : map.keySet()) {
            cfMap.put(key.toString(), map.get(key).toString());
          }
        }
        typeToCFs.get().put(o, cfMap);
      }
      return cfMap.get(enrichmentType);
    }
  }

  public static String toTopLevelField(String field) {
    if(field == null) {
      return null;
    }
    return Iterables.getLast(Splitter.on('.').split(field));
  }

  public static TableProvider getTableProvider(String connectorImpl, TableProvider defaultImpl) {
    if(connectorImpl == null || connectorImpl.length() == 0 || connectorImpl.charAt(0) == '$') {
      return defaultImpl;
    }
    else {
      try {
        Class<? extends TableProvider> clazz = (Class<? extends TableProvider>) Class.forName(connectorImpl);
        return clazz.getConstructor().newInstance();
      } catch (InstantiationException e) {
        throw new IllegalStateException("Unable to instantiate connector.", e);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Unable to instantiate connector: illegal access", e);
      } catch (InvocationTargetException e) {
        throw new IllegalStateException("Unable to instantiate connector", e);
      } catch (NoSuchMethodException e) {
        throw new IllegalStateException("Unable to instantiate connector: no such method", e);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("Unable to instantiate connector: class not found", e);
      }
    }
  }

}
