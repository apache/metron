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

import org.junit.Assert;
import org.junit.Test;

public class EncodingsTest {

  public static final String STRING_FIXTURE = "Hello World";
  public static final String STRING_FIXTURE_PLUS_NULL = "Hello World\0";
  public static final String BASE32HEX_FIXTURE = "91IMOR3F41BMUSJCCG======";
  public static final String BASE32_FIXTURE = "JBSWY3DPEBLW64TMMQ======";
  public static final String BASE64_FIXTURE = "SGVsbG8gV29ybGQ=";
  public static final String BINARY_FIXTURE = "0110010001101100011100100110111101010111001000000110111101101100011011000110010101001000";
  public static final String HEX_FIXTURE = "48656c6c6f20576f726c64";

  @Test
  public void is() throws Exception {

    // base32
    Assert.assertTrue(Encodings.BASE32.is(BASE32_FIXTURE));
    Assert.assertFalse(Encodings.BASE32.is(STRING_FIXTURE));

    // base32 hex
    Assert.assertTrue(Encodings.BASE32HEX.is(BASE32HEX_FIXTURE));
    Assert.assertFalse(Encodings.BASE32HEX.is(STRING_FIXTURE));

    // base 64
    Assert.assertTrue(Encodings.BASE64.is(BASE64_FIXTURE));
    Assert.assertFalse(Encodings.BASE64.is(STRING_FIXTURE + "\0"));

    // binary
    Assert.assertTrue(Encodings.BINARY.is(BINARY_FIXTURE));
    Assert.assertFalse(Encodings.BINARY.is(STRING_FIXTURE));

    // hex
    Assert.assertTrue(Encodings.HEX.is(HEX_FIXTURE));
    Assert.assertFalse(Encodings.HEX.is("AAA"));
  }

  @Test
  public void decode() throws Exception {
    Assert.assertEquals(STRING_FIXTURE,Encodings.BASE32.decode(BASE32_FIXTURE));
    Assert.assertEquals(STRING_FIXTURE,Encodings.BASE32HEX.decode(BASE32HEX_FIXTURE));
    Assert.assertEquals(STRING_FIXTURE,Encodings.BASE64.decode(BASE64_FIXTURE));
    Assert.assertEquals(STRING_FIXTURE,Encodings.BINARY.decode(BINARY_FIXTURE));
    Assert.assertEquals(STRING_FIXTURE,Encodings.HEX.decode(HEX_FIXTURE));

    // these codecs will just decode away... and return garbage without verification
    Assert.assertNotEquals(STRING_FIXTURE,Encodings.BASE32.decode(STRING_FIXTURE));
    Assert.assertNotEquals(STRING_FIXTURE,Encodings.BASE32HEX.decode(STRING_FIXTURE));
    Assert.assertNotEquals(STRING_FIXTURE,Encodings.BASE64.decode(STRING_FIXTURE));

    // these codecs will fail to decode and return the original string without
    // verification
    Assert.assertEquals(STRING_FIXTURE,Encodings.BINARY.decode(STRING_FIXTURE));
    Assert.assertEquals(STRING_FIXTURE,Encodings.HEX.decode(STRING_FIXTURE));
  }

  @Test
  public void decodeWithVerify() throws Exception {
    Assert.assertEquals(STRING_FIXTURE,Encodings.BASE32.decode(BASE32_FIXTURE,true));
    Assert.assertEquals(STRING_FIXTURE,Encodings.BASE32HEX.decode(BASE32HEX_FIXTURE,true));
    Assert.assertEquals(STRING_FIXTURE,Encodings.BASE64.decode(BASE64_FIXTURE,true));
    Assert.assertEquals(STRING_FIXTURE,Encodings.BINARY.decode(BINARY_FIXTURE,true));
    Assert.assertEquals(STRING_FIXTURE,Encodings.HEX.decode(HEX_FIXTURE,true));

    // with verification, we will get back the original string
    Assert.assertEquals(STRING_FIXTURE,Encodings.BASE32.decode(STRING_FIXTURE,true));
    Assert.assertEquals(STRING_FIXTURE,Encodings.BASE32HEX.decode(STRING_FIXTURE,true));
    // if the string IS coincidentally compatable with base64, then it will decode away
    Assert.assertNotEquals(STRING_FIXTURE,Encodings.BASE64.decode(STRING_FIXTURE,true));
    // if the string would fail... then we get the original
    Assert.assertEquals(STRING_FIXTURE + "\0",Encodings.BASE64.decode(STRING_FIXTURE + "\0",true));
    Assert.assertEquals(STRING_FIXTURE,Encodings.BINARY.decode(STRING_FIXTURE,true));
    Assert.assertEquals(STRING_FIXTURE,Encodings.HEX.decode(STRING_FIXTURE,true));
  }

  @Test
  public void testEncode() throws Exception{
    Assert.assertEquals(BASE32_FIXTURE,Encodings.BASE32.encode(STRING_FIXTURE));
    Assert.assertEquals(BASE32HEX_FIXTURE,Encodings.BASE32HEX.encode(STRING_FIXTURE));
    Assert.assertEquals(BASE64_FIXTURE,Encodings.BASE64.encode(STRING_FIXTURE));
    Assert.assertEquals(BINARY_FIXTURE,Encodings.BINARY.encode(STRING_FIXTURE));
    Assert.assertEquals(HEX_FIXTURE,Encodings.HEX.encode(STRING_FIXTURE));
  }
}