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
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.filters.AbstractMessageFilter;
import org.apache.metron.parsers.filters.Filters;
import org.apache.metron.parsers.filters.GenericMessageFilter;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FiltersTest {
  @Test
  public void testDefault() {
    Assert.assertTrue(Filters.get("DEFAULT", null).emitTuple(null));
    Assert.assertTrue(Filters.get(GenericMessageFilter.class.getName(), null).emitTuple(null));
  }

  @Test
  public void testSingleQueryFilter() throws Exception {
    {
      Map<String, Object> config = new HashMap<String, Object>() {{
        put("filter.query", "exists(foo)");
      }};
      MessageFilter<JSONObject> filter = Filters.get("QUERY", config);
      Assert.assertTrue(filter.emitTuple(new JSONObject(ImmutableMap.of("foo", 1))));
      Assert.assertFalse(filter.emitTuple(new JSONObject(ImmutableMap.of("bar", 1))));
    }

  }
}
