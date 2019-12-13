/*
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
package org.apache.metron.stellar.common.utils.hashing;

import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class DefaultHasherTest {
  private Charset charset = StandardCharsets.UTF_8;
  private BinaryEncoder encoder = new Hex(charset);

  @Test
  public void getHashAsHexOfNullValueReturnsPadded00() throws Exception {
    assertEquals(StringUtils.repeat("00", 16), new DefaultHasher("md5", encoder).getHash(null));
    assertEquals(StringUtils.repeat("00", 32), new DefaultHasher("sha-256", encoder).getHash(null));
  }

  @Test
  public void nonSerializableShouldReturnNull() throws Exception {
    assertNull(new DefaultHasher("md5", encoder, charset).getHash(new Object()));
  }

  @Test
  public void hashingStringWithEmptyString() throws Exception {
    assertEquals("d41d8cd98f00b204e9800998ecf8427e", new DefaultHasher("md5", encoder).getHash(""));
  }

  @Test
  public void hashingSerializableObject() throws Exception {
    final Collection<String> serializable = Collections.emptyList();
    assertEquals("ef5e8c8d27af3a953b4674065c99a52a", new DefaultHasher("md5", encoder, charset).getHash(serializable));
  }
}