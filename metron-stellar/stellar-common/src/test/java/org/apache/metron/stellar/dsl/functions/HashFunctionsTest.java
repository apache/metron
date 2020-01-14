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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHHasher;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.junit.jupiter.api.Assertions.*;

public class HashFunctionsTest {
  static final Hex HEX = new Hex(StandardCharsets.UTF_8);
  final HashFunctions.ListSupportedHashTypes listSupportedHashTypes = new HashFunctions.ListSupportedHashTypes();
  final HashFunctions.Hash hash = new HashFunctions.Hash();

  @Test
  public void nullArgumentsShouldFail() {
    assertThrows(IllegalArgumentException.class, () -> listSupportedHashTypes.apply(null));
  }

  @Test
  public void getSupportedHashAlgorithmsCalledWithParametersShouldFail() {
    assertThrows(IllegalArgumentException.class, () -> listSupportedHashTypes.apply(Collections.singletonList("bogus")));
  }

  @Test
  public void listSupportedHashTypesReturnsAtMinimumTheHashingAlgorithmsThatMustBeSupported() {
    final List<String> requiredAlgorithmsByJava = Arrays.asList("MD5", "SHA", "SHA-256"); // These are required for all Java platforms (see java.security.MessageDigest). Note: SHA is SHA-1
    final Collection<String> supportedHashes = listSupportedHashTypes.apply(Collections.emptyList());
    requiredAlgorithmsByJava.forEach(a -> assertTrue(supportedHashes.contains(a)));
  }

  @Test
  public void nullArgumentListShouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> hash.apply(null));
  }

  @Test
  public void emptyArgumentListShouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> hash.apply(Collections.emptyList()));
  }

  @Test
  public void singleArgumentListShouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> hash.apply(Collections.singletonList("some value.")));
  }

  @Test
  public void argumentListWithMoreThanTwoValuesShouldThrowException3() {
    assertThrows(IllegalArgumentException.class, () -> hash.apply(Arrays.asList("1", "2", "3")));
  }

  @Test
  public void argumentListWithMoreThanTwoValuesShouldThrowException4() {
    assertThrows(IllegalArgumentException.class, () -> hash.apply(Arrays.asList("1", "2", "3", "4")));
  }

  @Test
  public void invalidAlgorithmArgumentShouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> hash.apply(Arrays.asList("value to hash", "invalidAlgorithm")));
  }

  @Test
  public void invalidNullAlgorithmArgumentShouldReturnNull() {
    assertNull(hash.apply(Arrays.asList("value to hash", null)));
  }

  @Test
  public void nullInputForValueToHashShouldReturnHashedEncodedValueOf0x00() {
    assertEquals(StringUtils.repeat('0', 32), hash.apply(Arrays.asList(null, "md5")));
  }

  @Test
  public void nullInputForValueToHashShouldReturnHashedEncodedValueOf0x00InDirectStellarCall() {
    final String algorithm = "'md5'";
    final Map<String, Object> variables = new HashMap<>();
    variables.put("toHash", null);

    assertEquals(StringUtils.repeat('0', 32), run("HASH(toHash, " + algorithm + ")", variables));
  }

  @Test
  public void allAlgorithmsForMessageDigestShouldBeAbleToHash() {
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
  public void allAlgorithmsForMessageDigestShouldBeAbleToHashDirectStellarCall() {
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
  public void aNonNullNonSerializableObjectReturnsAValueOfNull() {
    final Map<String, Object> variables = new HashMap<>();
    variables.put("toHash", new Object());
    assertNull(run("HASH(toHash, 'md5')", variables));
  }

  public static String TLSH_DATA = "The best documentation is the UNIX source. After all, this is what the "
            + "system uses for documentation when it decides what to do next! The "
            + "manuals paraphrase the source code, often having been written at "
            + "different times and by different people than who wrote the code. "
            + "Think of them as guidelines. Sometimes they are more like wishes... "
            + "Nonetheless, it is all too common to turn to the source and find "
            + "options and behaviors that are not documented in the manual. Sometimes "
            + "you find options described in the manual that are unimplemented "
            + "and ignored by the source.";
  String TLSH_EXPECTED = "6FF02BEF718027B0160B4391212923ED7F1A463D563B1549B86CF62973B197AD2731F8";

  @Test
  public void tlsh_happyPath() {
    final Map<String, Object> variables = new HashMap<>();

    variables.put("toHash", TLSH_DATA);
    variables.put("toHashBytes", TLSH_DATA.getBytes(StandardCharsets.UTF_8));
    //this value is pulled from a canonical example at  https://github.com/idealista/tlsh#how-to-calculate-a-hash
    assertEquals(TLSH_EXPECTED, run("HASH(toHash, 'tlsh')", variables));
    assertEquals(TLSH_EXPECTED, run("HASH(toHash, 'TLSH')", variables));
    assertEquals(TLSH_EXPECTED, run("HASH(toHashBytes, 'tlsh')", variables));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void tlsh_multiBin() {
    final Map<String, Object> variables = new HashMap<>();

    variables.put("toHash", TLSH_DATA);
    Map<String, String> out = (Map<String, String>)run("HASH(toHash, 'tlsh', { 'hashes' : [ 8, 16, 32 ]} )", variables);

    assertTrue(out.containsKey(TLSHHasher.TLSH_KEY));
    for(int h : ImmutableList.of(8, 16, 32)) {
      assertTrue(out.containsKey(TLSHHasher.TLSH_BIN_KEY + "_" + h));
    }
  }


  @Test
  public void tlsh_multithread() {
    //we want to ensure that everything is threadsafe, so we'll spin up some random data
    //generate some hashes and then do it all in parallel and make sure it all matches.
    Map<Map.Entry<byte[], Map<String, Object>>, String> hashes = new HashMap<>();
    Random r = new Random(0);
    for(int i = 0;i < 20;++i) {
      byte[] d = new byte[256];
      r.nextBytes(d);
      Map<String, Object> config = new HashMap<String, Object>()
      {{
          put(TLSHHasher.Config.BUCKET_SIZE.key, r.nextBoolean() ? 128 : 256);
          put(TLSHHasher.Config.CHECKSUM.key, r.nextBoolean() ? 1 : 3);
      }};
      String hash = (String)run("HASH(data, 'tlsh', config)", ImmutableMap.of("config", config, "data", d));
      assertNotNull(hash);
      hashes.put(new AbstractMap.SimpleEntry<>(d, config), hash);
    }
    ForkJoinPool forkJoinPool = new ForkJoinPool(5);

    forkJoinPool.submit(() ->
            hashes.entrySet().parallelStream().forEach(
                   kv ->  {
                     Map<String, Object> config = kv.getKey().getValue();
                     byte[] data = kv.getKey().getKey();
                     String hash = (String)run("HASH(data, 'tlsh', config)", ImmutableMap.of("config", config, "data", data));
                     assertEquals(hash, kv.getValue());
                   }
            )
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  public void tlsh_similarity() {
    for(Map.Entry<String, String> kv : ImmutableMap.of("been", "ben", "document", "dokumant", "code", "cad").entrySet()) {
      Map<String, Object> variables = ImmutableMap.of("toHash", TLSH_DATA, "toHashSimilar", TLSH_DATA.replace(kv.getKey(), kv.getValue()));
      Map<String, Object> bin1 = (Map<String, Object>) run("HASH(toHashSimilar, 'tlsh', { 'hashes' : 4, 'bucketSize' : 128 })", variables);
      Map<String, Object> bin2 = (Map<String, Object>) run("HASH(toHash, 'tlsh', { 'hashes' : [ 4 ], 'bucketSize' : 128 })", variables);
      assertEquals(bin1.get("tlsh_bin"), bin2.get("tlsh_bin"), kv.getKey() + " != " + kv.getValue() + " because " + bin1.get("tlsh") + " != " + bin2.get("tlsh"));
      assertNotEquals(bin1.get("tlsh"), bin2.get("tlsh"));
      Map<String, Object> distVariables = ImmutableMap.of("hash1", bin1.get(TLSHHasher.TLSH_KEY), "hash2", bin2.get(TLSHHasher.TLSH_KEY));
      {
        //ensure the diff is minimal
        Integer diff = (Integer) run("TLSH_DIST( hash1, hash2)", distVariables);
        Integer diffReflexive = (Integer) run("TLSH_DIST( hash2, hash1)", distVariables);
        assertTrue(diff < 100, "diff == " + diff);
        assertEquals(diff, diffReflexive);
      }

      {
        //ensure that d(x,x) == 0
        Integer diff = (Integer) run("TLSH_DIST( hash1, hash1)", distVariables);
        assertEquals((int)0, (int)diff);
      }
    }
  }

  @Test
  public void tlshDist_invalidInput() {
    final Map<String, Object> variables = new HashMap<>();
    variables.put("hash1", 1);
    variables.put("hash2", TLSH_EXPECTED);
    assertThrows(Exception.class, () -> run("TLSH_DIST( hash1, hash1)", variables));
  }

  @Test
  public void tlsh_insufficientComplexity() {
    final Map<String, Object> variables = new HashMap<>();
    String data = "Metron is the best";
    variables.put("toHash", data);
    assertNull(run("HASH(toHash, 'tlsh')", variables));
  }

  @Test
  public void tlsh_nullInput() {
    final Map<String, Object> variables = new HashMap<>();
    String data = null;
    variables.put("toHash", data);
    assertNull(run("HASH(toHash, 'tlsh')", variables));
  }

  private String expectedHexString(MessageDigest expected) {
    return new String(HEX.encode(expected.digest()), StandardCharsets.UTF_8);
  }
}
