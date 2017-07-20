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
package org.apache.metron.indexing.mutation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MutationTest {

  @Test
  public void testReplace() throws Exception {
    Mutation m = Mutation.of(MutationOperation.REPLACE, "{ \"a\" : 1 }");
    String out = m.apply(
            () -> {
              try {
                return JSONUtils.INSTANCE.load("{ \"b\" : 1 }", JsonNode.class);
              } catch (IOException e) {
                throw new IllegalStateException(e);
              }
            }

    );
    Assert.assertEquals(m.mutationArg, out);
  }

  /**
   [
   { "op": "add"
   , "path": "/b"
   , "value": "metron"
   }
   ]
   */
  @Multiline
  public static String addElement;

  @Test
  public void testPatch_add() throws Exception {
    final Map<String, Object> orig = new HashMap<String, Object>() {{
      put("a", 1);
    }};
    Map<String, Object> out = apply(Mutation.of(MutationOperation.PATCH, addElement), orig);
    mapEquals(out, new HashMap<String, Object>() {{
      putAll(orig);
      put("b", "metron");
    }});
  }


  /**
   [
   { "op": "add"
   , "path": "/b"
   , "value": "metron"
   },
   { "op": "remove"
   , "path": "/a"
   }
   ]
   */
  @Multiline
  public static String addAndThenRemoveElement;

  @Test
  public void testPatch_addThenRemove() throws Exception {
    final Map<String, Object> orig = new HashMap<String, Object>() {{
      put("a", 1);
    }};
    Map<String, Object> out = apply(Mutation.of(MutationOperation.PATCH, addAndThenRemoveElement) , orig);
    mapEquals(out, new HashMap<String, Object>() {{
      put("b", "metron");
    }});
  }

  public static Map<String, Object> apply(Mutation m, Map<String, Object> orig) throws Exception {
    String out = m.apply(
            () -> {
              try {
                String origStr = new JSONObject(orig).toJSONString();
                return JSONUtils.INSTANCE.load(origStr, JsonNode.class);
              } catch (IOException e) {
                throw new IllegalStateException(e);
              }
            }

    );
    return JSONUtils.INSTANCE.load(out, new TypeReference<Map<String, Object>>() {
    });
  }

  public static void mapEquals(Map<String, Object> m1, Map<String, Object> m2) {
    Assert.assertEquals(0, Sets.symmetricDifference(m1.entrySet(), m2.entrySet()).size());
  }
}
