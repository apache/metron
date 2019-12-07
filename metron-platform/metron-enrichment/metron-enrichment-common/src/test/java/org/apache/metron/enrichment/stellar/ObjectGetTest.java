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
import org.apache.metron.stellar.dsl.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectGetTest {
  private ObjectCache objectCache;
  private Context context;

  @BeforeEach
  public void setup() throws Exception {
    objectCache = mock(ObjectCache.class);
    context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, HashMap::new)
            .build();
  }

  @Test
  public void shouldInitialize() {
    ObjectGet objectGet = new ObjectGet();
    assertFalse(objectGet.isInitialized());
    objectGet.initialize(context);

    assertTrue(objectGet.isInitialized());
    assertTrue(objectGet.getObjectCache().isInitialized());
    assertEquals(new HashMap<String, Object>(), objectGet.getConfig(context));
  }

  @Test
  public void shouldApplyObjectGet() {
    Object object = mock(Object.class);
    when(objectCache.get("/path")).thenReturn(object);
    ObjectGet objectGet = new ObjectGet();
    assertNull(objectGet.apply(Collections.singletonList("/path"), context));

    objectGet.initialize(objectCache);

    assertNull(objectGet.apply(new ArrayList<>(), context));
    assertNull(objectGet.apply(Collections.singletonList(null), context));
    objectGet.apply(Collections.singletonList("/path"), context);
  }

  @Test
  public void shouldThrowIllegalStateExceptionOnInvalidPath() {
    ObjectGet objectGet = new ObjectGet();
    objectGet.initialize(context);
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> objectGet.apply(Collections.singletonList(1), context));
    assertTrue(e.getMessage().contains("Unable to retrieve 1 as it is not a path"));
  }
}
