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
package org.apache.metron.test.utils;

import com.google.common.collect.Iterables;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;

import java.io.IOException;
import java.util.Map;

public class ValidationUtils {

  public static void assertJSONEqual(String expected, String actual) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map m1 = mapper.readValue(expected, Map.class);
    Map m2 = mapper.readValue(actual, Map.class);
    for(Object k : m1.keySet()) {
      Object v1 = m1.get(k);
      Object v2 = m2.get(k);

      if(v2 == null) {
        Assert.fail("Unable to find key: " + k + " in output");
      }
      if(k.equals("timestamp") || k.equals("ingest_timestamp")) {
        //TODO: Take the ?!?@ timestamps out of the reference file.
        Assert.assertEquals(v1.toString().length(), v2.toString().length());
      }
      else if(!v2.equals(v1)) {
        Assert.assertEquals("value mismatch for " + k ,v1, v2);
      }
    }
    Assert.assertTrue(m2.containsKey("timestamp"));
    Assert.assertTrue(m2.containsKey("ingest_timestamp"));
    Assert.assertEquals(getMapSize(m1), getMapSize(m2));
  }
  private static int getMapSize(Map m) {
    return Iterables.size(Iterables.filter(m.keySet(), k -> !k.toString().contains("timestamp")));
  }
}
