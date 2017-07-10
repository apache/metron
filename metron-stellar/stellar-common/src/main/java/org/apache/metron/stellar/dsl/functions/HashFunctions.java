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

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.metron.stellar.common.utils.hashing.DefaultHasher;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

public class HashFunctions {

  @Stellar(
    name = "GET_HASHES_AVAILABLE",
    description = "Will return all available hashing algorithms available to 'HASH'.",
    returns = "A list containing all supported hashing algorithms."
  )
  public static class ListSupportedHashTypes extends BaseStellarFunction {

    @Override
    public List<String> apply(final List<Object> args) {
      if (args == null || args.size() != 0) {
        throw new IllegalArgumentException("Invalid call. This function does not expect any arguments.");
      }

      return new ArrayList<>(Security.getAlgorithms("MessageDigest"));
    }
  }

  @Stellar(
    name = "HASH",
    description = "Hashes a given value using the given hashing algorithm and returns a hex encoded string.",
    params = {
      "toHash - value to hash.",
      "hashType - A valid string representation of a hashing algorithm. See 'GET_HASHES_AVAILABLE'.",
    },
    returns = "A hex encoded string of a hashed value using the given algorithm. If 'hashType' is null " +
      "then '00', padded to the necessary length, will be returned. If 'toHash' is not able to be hashed or " +
      "'hashType' is null then null is returned."
  )
  public static class Hash extends BaseStellarFunction {

    @Override
    public Object apply(final List<Object> args) {
      if (args == null || args.size() != 2) {
        throw new IllegalArgumentException("Invalid number of arguments: " + (args == null ? 0 : args.size()));
      }

      final Object toHash = args.get(0);
      final Object hashType = args.get(1);

      if (hashType == null) {
        return null;
      }

      try {
        return new DefaultHasher(hashType.toString(), new Hex(StandardCharsets.UTF_8)).getHash(toHash);
      } catch (final EncoderException e) {
        return null;
      } catch (final NoSuchAlgorithmException e) {
        throw new IllegalArgumentException("Invalid hash type: " + hashType.toString());
      }
    }
  }
}
