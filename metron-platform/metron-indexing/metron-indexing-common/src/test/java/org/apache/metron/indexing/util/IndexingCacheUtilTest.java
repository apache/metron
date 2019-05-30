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

package org.apache.metron.indexing.util;

import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexingCacheUtilTest {

  @Test
  public void getIndexLookupFunctionShouldReturnConfiguredIndex() {
    IndexingConfigurations indexingConfigs = mock(IndexingConfigurations.class);
    ConfigurationsCache cache = mock(ConfigurationsCache.class);

    Map<String, Object> broIndexingConfig = new HashMap<String, Object>() {{
      put("writer", new HashMap<String, Object>() {{
        put("index", "bro_index");
      }});
    }};
    when(indexingConfigs.getSensorIndexingConfig("bro")).thenReturn(broIndexingConfig);
    when(cache.get(IndexingConfigurations.class)).thenReturn(indexingConfigs);

    assertEquals("bro_index", IndexingCacheUtil.getIndexLookupFunction(cache, "writer").apply("bro"));
  }

  @Test
  public void getIndexLookupFunctionShouldDefaultToSensorType() {
    IndexingConfigurations indexingConfigs = mock(IndexingConfigurations.class);
    ConfigurationsCache cache = mock(ConfigurationsCache.class);

    Map<String, Object> broIndexingConfig = new HashMap<String, Object>() {{
      put("writer", new HashMap<String, Object>() {{
        put("index", "bro_index");
      }});
    }};
    when(indexingConfigs.getSensorIndexingConfig("bro")).thenReturn(broIndexingConfig);
    when(cache.get(IndexingConfigurations.class)).thenReturn(indexingConfigs);

    assertEquals("Should default to sensor type on missing sensor config", "snort", IndexingCacheUtil.getIndexLookupFunction(cache, "writer").apply("snort"));
    assertEquals("Should default to sensor type on missing writer config", "bro", IndexingCacheUtil.getIndexLookupFunction(cache, "someWriter").apply("bro"));
  }
}
