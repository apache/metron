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
import java.util.Collections;
import java.util.HashMap;

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
@PrepareForTest({ObjectGet.class, ObjectCache.class})
public class ObjectGetTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ObjectGet objectGet;
  private ObjectCache objectCache;
  private Context context;

  @Before
  public void setup() throws Exception {
    objectGet = new ObjectGet();
    objectCache = mock(ObjectCache.class);
    context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, HashMap::new)
            .build();

    whenNew(ObjectCache.class).withNoArguments().thenReturn(objectCache);
  }

  @Test
  public void shouldInitialize() throws Exception {
    when(objectCache.isInitialized()).thenReturn(true);

    assertFalse(objectGet.isInitialized());
    objectGet.initialize(context);

    ObjectCacheConfig expectedConfig = new ObjectCacheConfig(new HashMap<>());

    verify(objectCache, times(1)).initialize(expectedConfig);
    assertTrue(objectGet.isInitialized());
  }

  @Test
  public void shouldApplyObjectGet() {
    Object object = mock(Object.class);
    when(objectCache.get("/path")).thenReturn(object);

    assertNull(objectGet.apply(Collections.singletonList("/path"), context));

    when(objectCache.isInitialized()).thenReturn(true);
    objectGet.initialize(context);

    assertNull(objectGet.apply(new ArrayList<>(), context));
    assertNull(objectGet.apply(Collections.singletonList(null), context));
    assertEquals(object, objectGet.apply(Collections.singletonList("/path"), context));
  }

  @Test
  public void shouldThrowIllegalStateExceptionOnInvalidPath() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to retrieve 1 as it is not a path");

    when(objectCache.isInitialized()).thenReturn(true);
    objectGet.initialize(context);
    objectGet.apply(Collections.singletonList(1), context);
  }

}
