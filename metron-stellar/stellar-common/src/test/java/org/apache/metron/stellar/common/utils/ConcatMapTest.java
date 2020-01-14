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
package org.apache.metron.stellar.common.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConcatMapTest {

  @Test
  public void testToString() {
    Map<String, Object> v1 = new HashMap<>();
    v1.put("k1", "v1");
    Map<String, Object> v2 = new HashMap<>();
    v2.put("k2", "v2");
    v2.put("k3", null);
    Map<String, Object> union = new HashMap<String, Object>() {{
      putAll(v1);
      put("k2", "v2");
    }};
    ConcatMap c = create(v1, v2);
    assertEquals(c.toString(), union.toString());
  }

  private ConcatMap create(Map... ms) {
    List<Map> l = new ArrayList<>();
    for(Map m : ms) {
      l.add(m);
    }
    return new ConcatMap(l);
  }

  private void assertKryoserializable(ConcatMap c) {
    byte[] serialized = SerDeUtils.toBytes(c);
    ConcatMap deserialized = SerDeUtils.fromBytes(serialized, ConcatMap.class);
    assertEquals(deserialized, c);
  }

  @Test
  public void testKryoSerialization() {
    Map<String, Object> v1 = new HashMap<>();
    v1.put("k1", "v1");
    Map<String, Object> v2 = new HashMap<>();
    v2.put("k2", "v2");
    v2.put("k3", null);
    {
      //multi maps
      ConcatMap c = create(v1, v2);
      assertKryoserializable(c);
    }
    {
      //single maps
      ConcatMap c = create(v1);
      assertKryoserializable(c);
    }
    {
      //empty maps
      ConcatMap c = create();
      assertKryoserializable(c);
    }
  }

}
