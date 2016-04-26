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
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.enrichment.converter.EnrichmentKey;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;

public class EnrichmentUtils {

  public static final String KEY_PREFIX = "enrichments";

  public static String getEnrichmentKey(String enrichmentName, String field) {
    return Joiner.on(".").join(new String[]{KEY_PREFIX, enrichmentName, field});
  }

  public static class TypeToKey implements Function<String, EnrichmentKey> {
    private final String indicator;

    public TypeToKey(String indicator) {
      this.indicator = indicator;

    }
    @Nullable
    @Override
    public EnrichmentKey apply(@Nullable String enrichmentType) {
      return new EnrichmentKey(enrichmentType, indicator);
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
