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

package org.apache.metron.enrichment.stellar;

import org.apache.metron.enrichment.cache.ObjectCache;
import org.apache.metron.enrichment.cache.ObjectCacheConfig;
import org.apache.metron.stellar.dsl.Context;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.enrichment.cache.ObjectCacheConfig.OBJECT_CACHE_EXPIRATION_KEY;
import static org.apache.metron.enrichment.cache.ObjectCacheConfig.OBJECT_CACHE_MAX_FILE_SIZE_KEY;
import static org.apache.metron.enrichment.cache.ObjectCacheConfig.OBJECT_CACHE_SIZE_KEY;
import static org.apache.metron.enrichment.cache.ObjectCacheConfig.OBJECT_CACHE_TIME_UNIT_KEY;
import static org.apache.metron.enrichment.stellar.EnrichmentObjectGet.ENRICHMENT_OBJECT_GET_SETTINGS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EnrichmentObjectGet.class, ObjectCache.class})
public class EnrichmentObjectGetTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private EnrichmentObjectGet enrichmentObjectGet;
  private ObjectCache objectCache;
  private Context context;

  @Before
  public void setup() throws Exception {
    enrichmentObjectGet = new EnrichmentObjectGet();
    objectCache = mock(ObjectCache.class);
    context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, HashMap::new)
            .build();

    whenNew(ObjectCache.class).withNoArguments().thenReturn(objectCache);
  }

  @Test
  public void shouldInitializeWithDefaultSettings() throws Exception {
    when(objectCache.isInitialized()).thenReturn(true);

    enrichmentObjectGet.initialize(context);

    ObjectCacheConfig expectedConfig = new ObjectCacheConfig(new HashMap<>());

    verify(objectCache, times(1)).initialize(expectedConfig);
    assertTrue(enrichmentObjectGet.isInitialized());
  }

  @Test
  public void shouldInitializeWithCustomSettings() throws Exception {
    Map<String, Object> globalConfig = new HashMap<String, Object>() {{
      put(ENRICHMENT_OBJECT_GET_SETTINGS, new HashMap<String, Object>() {{
        put(OBJECT_CACHE_SIZE_KEY, 1);
        put(OBJECT_CACHE_EXPIRATION_KEY, 2);
        put(OBJECT_CACHE_TIME_UNIT_KEY, "SECONDS");
        put(OBJECT_CACHE_MAX_FILE_SIZE_KEY, 3);
      }});
    }};

    when(objectCache.isInitialized()).thenReturn(true);
    context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> globalConfig)
            .build();

    assertFalse(enrichmentObjectGet.isInitialized());

    enrichmentObjectGet.initialize(context);

    ObjectCacheConfig expectedConfig = new ObjectCacheConfig(new HashMap<>());
    expectedConfig.setCacheSize(1);
    expectedConfig.setCacheExpiration(2);
    expectedConfig.setTimeUnit(TimeUnit.SECONDS);
    expectedConfig.setMaxFileSize(3);

    verify(objectCache, times(1)).initialize(expectedConfig);
    assertTrue(enrichmentObjectGet.isInitialized());
  }

  @Test
  public void shouldApplyEnrichmentObjectGet() {
    Map<String, Object> enrichment = new HashMap<String, Object>() {{
      put("key", "value");
    }};
    when(objectCache.get("/path")).thenReturn(enrichment);

    assertNull(enrichmentObjectGet.apply(Arrays.asList("/path", "key"), context));

    when(objectCache.isInitialized()).thenReturn(true);
    enrichmentObjectGet.initialize(context);

    assertNull(enrichmentObjectGet.apply(Arrays.asList(null, null), context));
    assertEquals("value", enrichmentObjectGet.apply(Arrays.asList("/path", "key"), context));
  }

  @Test
  public void shouldThrowExceptionOnIncorrectObjectFormat() {
    thrown.expect(ClassCastException.class);
    thrown.expectMessage("The object stored in HDFS at '/path' must be serialized in JSON format.");

    when(objectCache.get("/path")).thenReturn("incorrect format");

    when(objectCache.isInitialized()).thenReturn(true);
    enrichmentObjectGet.initialize(context);
    enrichmentObjectGet.apply(Arrays.asList("/path", "key"), context);
  }

  @Test
  public void restGetShouldThrownExceptionOnMissingParameter() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("All parameters are mandatory, submit 'hdfs path', 'indicator'");

    enrichmentObjectGet.apply(new ArrayList<>(), context);
  }

}
