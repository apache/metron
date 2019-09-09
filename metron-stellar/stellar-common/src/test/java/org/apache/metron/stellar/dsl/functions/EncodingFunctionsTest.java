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

package org.apache.metron.stellar.dsl.functions;

import org.apache.commons.collections4.ListUtils;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.apache.metron.stellar.common.encoding.EncodingsTest.*;
import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;
import static org.junit.jupiter.api.Assertions.*;
public class EncodingFunctionsTest {

  private static final Map<String, Object> variableMap = new HashMap<String, Object>() {{
    put("BASE32HEX_FIXTURE","91IMOR3F41BMUSJCCG======");
    put("BASE32_FIXTURE" , "JBSWY3DPEBLW64TMMQ======\r\n");
    put("BASE64_FIXTURE" , "SGVsbG8gV29ybGQ=");
    put("BINARY_FIXTURE" , "0110010001101100011100100110111101010111001000000110111101101100011011000110010101001000");
    put("HEX_FIXTURE" , "48656c6c6f20576f726c64");
    put("STRING_FIXTURE", STRING_FIXTURE);
    put("STRING_FIXTURE_PLUS_NULL", STRING_FIXTURE_PLUS_NULL);

  }};

  @Test
  @SuppressWarnings("unchecked")
  public void testSupportedEncodingsList() {
    Object ret = run("GET_SUPPORTED_ENCODINGS()", new HashMap());
    assertTrue(ret instanceof List );
    List<String> list = (List<String>)ret;
    List<String> expected = new ArrayList<>(Arrays.asList("BASE32","BASE32HEX","BASE64","BINARY","HEX"));
    assertTrue(ListUtils.isEqualList(expected,list));
  }

  @Test
  public void testEncodingIs() {
    assertTrue(runPredicate("IS_ENCODING(BASE32_FIXTURE,'BASE32')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("IS_ENCODING(STRING_FIXTURE,'BASE32')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("IS_ENCODING(BASE32HEX_FIXTURE,'BASE32HEX')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("IS_ENCODING(STRING_FIXTURE,'BASE32HEX')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("IS_ENCODING(BASE64_FIXTURE,'BASE64')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("IS_ENCODING(STRING_FIXTURE_PLUS_NULL,'BASE64')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("IS_ENCODING(BINARY_FIXTURE,'BINARY')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("IS_ENCODING(STRING_FIXTURE,'BINARY')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("IS_ENCODING(HEX_FIXTURE,'HEX')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("IS_ENCODING(STRING_FIXTURE,'HEX')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void testDecode() {
    assertEquals(STRING_FIXTURE,run("DECODE(BASE32_FIXTURE,'BASE32')",variableMap));
    assertEquals(STRING_FIXTURE, run("DECODE(BASE32HEX_FIXTURE,'BASE32HEX')",variableMap));
    assertEquals(STRING_FIXTURE, run("DECODE(BASE64_FIXTURE,'BASE64')",variableMap));
    assertEquals(STRING_FIXTURE, run("DECODE(BINARY_FIXTURE,'BINARY')",variableMap));
    assertEquals(STRING_FIXTURE, run("DECODE(HEX_FIXTURE,'HEX')",variableMap));

    // these codecs will just decode away... and return garbage without verification
    assertNotEquals(STRING_FIXTURE,run("DECODE(STRING_FIXTURE,'BASE32')",variableMap));
    assertNotEquals(STRING_FIXTURE,run("DECODE(STRING_FIXTURE,'BASE32HEX')",variableMap));
    assertNotEquals(STRING_FIXTURE,run("DECODE(STRING_FIXTURE,'BASE64')",variableMap));

    // these codecs will fail to decode and return the original string without
    // verification
    assertEquals(STRING_FIXTURE,run("DECODE(STRING_FIXTURE,'BINARY')",variableMap));
    assertEquals(STRING_FIXTURE,run("DECODE(STRING_FIXTURE, 'HEX')", variableMap));
  }

  @Test
  public void testDecodeWithVerify() {
    assertEquals(STRING_FIXTURE, run("DECODE(BASE32_FIXTURE,'BASE32',true)",variableMap));
    assertEquals(STRING_FIXTURE, run("DECODE(BASE32HEX_FIXTURE,'BASE32HEX',true)",variableMap));
    assertEquals(STRING_FIXTURE, run("DECODE(BASE64_FIXTURE,'BASE64',true)",variableMap));
    assertEquals(STRING_FIXTURE, run("DECODE(BINARY_FIXTURE,'BINARY',true)",variableMap));
    assertEquals(STRING_FIXTURE, run("DECODE(HEX_FIXTURE,'HEX',true)",variableMap));



    // with verification, we will get back the original string
    assertEquals(STRING_FIXTURE,run("DECODE(STRING_FIXTURE,'BASE32',true)",variableMap));
    assertEquals(STRING_FIXTURE,run("DECODE(STRING_FIXTURE,'BASE32HEX',true)",variableMap));
    // if the string IS coincidentally compatable with base64, then it will decode away
    assertNotEquals(STRING_FIXTURE,run("DECODE(STRING_FIXTURE,'BASE64',true)",variableMap));
    // if the string would fail... then we get the original
    assertEquals(STRING_FIXTURE_PLUS_NULL,run("DECODE(STRING_FIXTURE_PLUS_NULL,'BASE64',true)",variableMap));
    assertEquals(STRING_FIXTURE,run("DECODE(STRING_FIXTURE,'BINARY',true)",variableMap));
    assertEquals(STRING_FIXTURE,run("DECODE(STRING_FIXTURE,'HEX',true)",variableMap));
  }

  @Test
  public void testEncode() throws Exception {
    assertEquals(BASE32_FIXTURE,run("ENCODE(STRING_FIXTURE,'BASE32')",variableMap));
    assertEquals(BASE32HEX_FIXTURE, run("ENCODE(STRING_FIXTURE,'BASE32HEX')",variableMap));
    assertEquals(BASE64_FIXTURE, run("ENCODE(STRING_FIXTURE,'BASE64')",variableMap));
    assertEquals(BINARY_FIXTURE, run("ENCODE(STRING_FIXTURE,'BINARY')",variableMap));
    assertEquals(HEX_FIXTURE, run("ENCODE(STRING_FIXTURE,'HEX')",variableMap));
  }
}