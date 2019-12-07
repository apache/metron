/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.common.utils;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"unchecked"})
public class HashUtilsTest {

  @Test
  public void getMessageHashShouldReturnHashForHashFields() {
    JSONObject message = new JSONObject();
    message.put("field1", "value1");
    message.put("field2", "value2");
    message.put("field3", "value3");
    Collection<String> fields = Arrays.asList("field2", "field3");
    assertEquals("6eab1c2c827387803ce457c76552f0511858fc1f9505c7dc620e198c0d1f4d02", HashUtils.getMessageHash(message, fields));
  }

  @Test
  public void getMessageHashShouldReturnHashForMessage() {
    JSONObject message = new JSONObject();
    message.put("field1", "value1");
    message.put("field2", "value2");
    message.put("field3", "value3");
    assertEquals("a76cdafc5aa49180c0b22c78d4415c505f9997c54847cec6c623f4cacf6a2811", HashUtils.getMessageHash(message));
  }

  @Test
  public void getMessageHashShouldReturnHashForBytes() {
    assertEquals("ab530a13e45914982b79f9b7e3fba994cfd1f3fb22f71cea1afbf02b460c6d1d", HashUtils.getMessageHash("message".getBytes(UTF_8)));
  }
}
