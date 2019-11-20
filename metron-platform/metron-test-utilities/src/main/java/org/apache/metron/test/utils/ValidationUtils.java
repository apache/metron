/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


import java.io.IOException;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;

public class ValidationUtils {

  /**
   * Validates that two JSON Strings are equal in value.
   * Since JSON does not guarentee order of fields, we cannot just compare Strings.
   * <p>
   * This utility understands that the 'original_string' field may itself hold JSON,
   * and will attempt to validate that field as json if it fails straight string compare
   * </p>
   * @param expected the expected string value
   * @param actual the actual string value
   * @throws IOException if there is an issue parsing as json
   */
  public static void assertJsonEqual(String expected, String actual) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map m1 = mapper.readValue(expected, Map.class);
    Map m2 = mapper.readValue(actual, Map.class);
    for (Object k : m1.keySet()) {
      Object v1 = m1.get(k);
      Object v2 = m2.get(k);

      assertNotNull(v2, "Unable to find key: " + k + " in output");
      if (k.equals("timestamp") || k.equals("guid")) {
        //TODO: Take the ?!?@ timestamps out of the reference file.
        assertEquals(v1.toString().length(), v2.toString().length());
      } else if (!v2.equals(v1)) {
        boolean goodDeepDown = false;
        // if this fails, but is the original_string it may be in json format
        // where the field/value order may be random
        if (((String) k).equals("original_string")) {
          try {
            mapper.readValue((String) v1, Map.class);
            assertJsonEqual((String) v1, (String) v2);
            goodDeepDown = true;
          } catch (Exception e) {
            // nothing, the original fail stands
          }
        }
        if (!goodDeepDown) {
          assertEquals(v1, v2, "value mismatch for " + k);
        }
      }
    }
    assertEquals(m1.size(), m2.size());
  }
}
