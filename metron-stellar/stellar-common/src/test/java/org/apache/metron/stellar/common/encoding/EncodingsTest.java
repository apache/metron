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

package org.apache.metron.stellar.common.encoding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class EncodingsTest {

  public static final String STRING_FIXTURE = "Hello World";
  public static final String STRING_FIXTURE_PLUS_NULL = "Hello World\0";
  public static final String BASE32HEX_FIXTURE = "91IMOR3F41BMUSJCCG======";
  public static final String BASE32_FIXTURE = "JBSWY3DPEBLW64TMMQ======";
  public static final String BASE64_FIXTURE = "SGVsbG8gV29ybGQ=";
  public static final String BINARY_FIXTURE = "0110010001101100011100100110111101010111001000000110111101101100011011000110010101001000";
  public static final String HEX_FIXTURE = "48656c6c6f20576f726c64";

  @Test
  public void is() {

    // base32
    assertTrue(Encodings.BASE32.is(BASE32_FIXTURE));
    assertFalse(Encodings.BASE32.is(STRING_FIXTURE));

    // base32 hex
    assertTrue(Encodings.BASE32HEX.is(BASE32HEX_FIXTURE));
    assertFalse(Encodings.BASE32HEX.is(STRING_FIXTURE));

    // base 64
    assertTrue(Encodings.BASE64.is(BASE64_FIXTURE));
    assertFalse(Encodings.BASE64.is(STRING_FIXTURE + "\0"));

    // binary
    assertTrue(Encodings.BINARY.is(BINARY_FIXTURE));
    assertFalse(Encodings.BINARY.is(STRING_FIXTURE));

    // hex
    assertTrue(Encodings.HEX.is(HEX_FIXTURE));
    assertFalse(Encodings.HEX.is("AAA"));
  }

  @Test
  public void decode() {
    assertEquals(STRING_FIXTURE,Encodings.BASE32.decode(BASE32_FIXTURE));
    assertEquals(STRING_FIXTURE,Encodings.BASE32HEX.decode(BASE32HEX_FIXTURE));
    assertEquals(STRING_FIXTURE,Encodings.BASE64.decode(BASE64_FIXTURE));
    assertEquals(STRING_FIXTURE,Encodings.BINARY.decode(BINARY_FIXTURE));
    assertEquals(STRING_FIXTURE,Encodings.HEX.decode(HEX_FIXTURE));

    // these codecs will just decode away... and return garbage without verification
    assertNotEquals(STRING_FIXTURE,Encodings.BASE32.decode(STRING_FIXTURE));
    assertNotEquals(STRING_FIXTURE,Encodings.BASE32HEX.decode(STRING_FIXTURE));
    assertNotEquals(STRING_FIXTURE,Encodings.BASE64.decode(STRING_FIXTURE));

    // these codecs will fail to decode and return the original string without
    // verification
    assertEquals(STRING_FIXTURE,Encodings.BINARY.decode(STRING_FIXTURE));
    assertEquals(STRING_FIXTURE,Encodings.HEX.decode(STRING_FIXTURE));
  }

  @Test
  public void decodeWithVerify() {
    assertEquals(STRING_FIXTURE,Encodings.BASE32.decode(BASE32_FIXTURE,true));
    assertEquals(STRING_FIXTURE,Encodings.BASE32HEX.decode(BASE32HEX_FIXTURE,true));
    assertEquals(STRING_FIXTURE,Encodings.BASE64.decode(BASE64_FIXTURE,true));
    assertEquals(STRING_FIXTURE,Encodings.BINARY.decode(BINARY_FIXTURE,true));
    assertEquals(STRING_FIXTURE,Encodings.HEX.decode(HEX_FIXTURE,true));

    // with verification, we will get back the original string
    assertEquals(STRING_FIXTURE,Encodings.BASE32.decode(STRING_FIXTURE,true));
    assertEquals(STRING_FIXTURE,Encodings.BASE32HEX.decode(STRING_FIXTURE,true));
    // if the string IS coincidentally compatable with base64, then it will decode away
    assertNotEquals(STRING_FIXTURE,Encodings.BASE64.decode(STRING_FIXTURE,true));
    // if the string would fail... then we get the original
    assertEquals(STRING_FIXTURE + "\0",Encodings.BASE64.decode(STRING_FIXTURE + "\0",true));
    assertEquals(STRING_FIXTURE,Encodings.BINARY.decode(STRING_FIXTURE,true));
    assertEquals(STRING_FIXTURE,Encodings.HEX.decode(STRING_FIXTURE,true));
  }

  @Test
  public void testEncode() {
    assertEquals(BASE32_FIXTURE,Encodings.BASE32.encode(STRING_FIXTURE));
    assertEquals(BASE32HEX_FIXTURE,Encodings.BASE32HEX.encode(STRING_FIXTURE));
    assertEquals(BASE64_FIXTURE,Encodings.BASE64.encode(STRING_FIXTURE));
    assertEquals(BINARY_FIXTURE,Encodings.BINARY.encode(STRING_FIXTURE));
    assertEquals(HEX_FIXTURE,Encodings.HEX.encode(STRING_FIXTURE));
  }
}