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

package org.apache.metron.filters;

import com.google.common.collect.ImmutableMap;
import org.apache.metron.parsers.filters.Filters;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FiltersTest {
  @Test
  public void testDefault() {
    assertNull(Filters.get("DEFAULT", null));
  }

  @Test
  public void testSingleQueryFilter() {
    {
      Map<String, Object> config = new HashMap<String, Object>() {{
        put("filter.query", "exists(foo)");
      }};
      MessageFilter<JSONObject> filter = Filters.get(Filters.STELLAR.name(), config);
      assertTrue(filter.emit(new JSONObject(ImmutableMap.of("foo", 1)), Context.EMPTY_CONTEXT()));
      assertFalse(filter.emit(new JSONObject(ImmutableMap.of("bar", 1)), Context.EMPTY_CONTEXT()));
    }
  }

}
