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
package org.apache.metron.stellar.dsl.functions;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HashFunctionsTest {
  static final Hex HEX = new Hex(StandardCharsets.UTF_8);
  final HashFunctions.ListSupportedHashTypes listSupportedHashTypes = new HashFunctions.ListSupportedHashTypes();
  final HashFunctions.Hash hash = new HashFunctions.Hash();

  @Test(expected = IllegalArgumentException.class)
  public void nullArgumentsShouldFail() throws Exception {
    listSupportedHashTypes.apply(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getSupportedHashAlgorithmsCalledWithParametersShouldFail() throws Exception {
    listSupportedHashTypes.apply(Collections.singletonList("bogus"));
  }

  @Test
  public void listSupportedHashTypesReturnsAtMinimumTheHashingAlgorithmsThatMustBeSupported() throws Exception {
    final List<String> requiredAlgorithmsByJava = Arrays.asList("MD5", "SHA", "SHA-256"); // These are required for all Java platforms (see java.security.MessageDigest). Note: SHA is SHA-1
    final Collection<String> supportedHashes = listSupportedHashTypes.apply(Collections.emptyList());
    requiredAlgorithmsByJava.forEach(a -> assertTrue(supportedHashes.contains(a)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullArgumentListShouldThrowException() throws Exception {
    hash.apply(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyArgumentListShouldThrowException() throws Exception {
    hash.apply(Collections.emptyList());
  }

  @Test(expected = IllegalArgumentException.class)
  public void singleArgumentListShouldThrowException() throws Exception {
    hash.apply(Collections.singletonList("some value."));
  }

  @Test(expected = IllegalArgumentException.class)
  public void argumentListWithMoreThanTwoValuesShouldThrowException3() throws Exception {
    hash.apply(Arrays.asList("1", "2", "3"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void argumentListWithMoreThanTwoValuesShouldThrowException4() throws Exception {
    hash.apply(Arrays.asList("1", "2", "3", "4"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidAlgorithmArgumentShouldThrowException() throws Exception {
    hash.apply(Arrays.asList("value to hash", "invalidAlgorithm"));
  }

  @Test
  public void invalidNullAlgorithmArgumentShouldReturnNull() throws Exception {
    assertNull(hash.apply(Arrays.asList("value to hash", null)));
  }

  @Test
  public void nullInputForValueToHashShouldReturnHashedEncodedValueOf0x00() throws Exception {
    assertEquals(StringUtils.repeat('0', 32), hash.apply(Arrays.asList(null, "md5")));
  }

  @Test
  public void nullInputForValueToHashShouldReturnHashedEncodedValueOf0x00InDirectStellarCall() throws Exception {
    final String algorithm = "'md5'";
    final Map<String, Object> variables = new HashMap<>();
    variables.put("toHash", null);

    assertEquals(StringUtils.repeat('0', 32), run("HASH(toHash, " + algorithm + ")", variables));
  }

  @Test
  public void allAlgorithmsForMessageDigestShouldBeAbleToHash() throws Exception {
    final String valueToHash = "My value to hash";
    final Set<String> algorithms = Security.getAlgorithms("MessageDigest");

    algorithms.forEach(algorithm -> {
      try {
        final MessageDigest expected = MessageDigest.getInstance(algorithm);
        expected.update(valueToHash.getBytes(StandardCharsets.UTF_8));

        assertEquals(expectedHexString(expected), hash.apply(Arrays.asList(valueToHash, algorithm)));
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void allAlgorithmsForMessageDigestShouldBeAbleToHashDirectStellarCall() throws Exception {
    final String valueToHash = "My value to hash";
    final Set<String> algorithms = Security.getAlgorithms("MessageDigest");

    algorithms.forEach(algorithm -> {
      try {
        final Object actual = run("HASH('" + valueToHash + "', '" + algorithm + "')", Collections.emptyMap());

        final MessageDigest expected = MessageDigest.getInstance(algorithm);
        expected.update(valueToHash.getBytes(StandardCharsets.UTF_8));

        assertEquals(expectedHexString(expected), actual);
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void nonStringValueThatIsSerializableHashesSuccessfully() throws Exception {
    final String algorithm = "'md5'";
    final String valueToHash = "'My value to hash'";
    final Serializable input = (Serializable) Collections.singletonList(valueToHash);

    final MessageDigest expected = MessageDigest.getInstance(algorithm.replace("'", ""));
    expected.update(SerializationUtils.serialize(input));

    final Map<String, Object> variables = new HashMap<>();
    variables.put("toHash", input);

    assertEquals(expectedHexString(expected), run("HASH(toHash, " + algorithm + ")", variables));
  }

  @Test
  public void callingHashFunctionsWithVariablesAsInputHashesSuccessfully() throws Exception {
    final String algorithm = "md5";
    final String valueToHash = "'My value to hash'";
    final Serializable input = (Serializable) Collections.singletonList(valueToHash);

    final MessageDigest expected = MessageDigest.getInstance(algorithm);
    expected.update(SerializationUtils.serialize(input));

    final Map<String, Object> variables = new HashMap<>();
    variables.put("toHash", input);
    variables.put("hashType", algorithm);

    assertEquals(expectedHexString(expected), run("HASH(toHash, hashType)", variables));
  }

  @Test
  public void callingHashFunctionWhereOnlyHashTypeIsAVariableHashesSuccessfully() throws Exception {
    final String algorithm = "md5";
    final String valueToHash = "'My value to hash'";

    final MessageDigest expected = MessageDigest.getInstance(algorithm);
    expected.update(valueToHash.replace("'", "").getBytes(StandardCharsets.UTF_8));

    final Map<String, Object> variables = new HashMap<>();
    variables.put("hashType", algorithm);

    assertEquals(expectedHexString(expected), run("HASH(" + valueToHash + ", hashType)", variables));
  }

  @Test
  public void aNonNullNonSerializableObjectReturnsAValueOfNull() throws Exception {
    final Map<String, Object> variables = new HashMap<>();
    variables.put("toHash", new Object());
    assertNull(run("HASH(toHash, 'md5')", variables));
  }

  private String expectedHexString(MessageDigest expected) {
    return new String(HEX.encode(expected.digest()), StandardCharsets.UTF_8);
  }
}
