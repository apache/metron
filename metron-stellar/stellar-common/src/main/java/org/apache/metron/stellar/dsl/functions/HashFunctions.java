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
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.common.utils.hashing.HashStrategy;
import org.apache.metron.stellar.common.utils.hashing.tlsh.TLSH;
import org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHHasher;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

      List<String> ret = new ArrayList<>();
      ret.addAll(HashStrategy.ALL_SUPPORTED_HASHES);
      return ret;
    }
  }


  @Stellar(
    name = "HASH",
    description = "Hashes a given value using the given hashing algorithm and returns a hex encoded string.",
    params = {
      "toHash - value to hash.",
      "hashType - A valid string representation of a hashing algorithm. See 'GET_HASHES_AVAILABLE'.",
      "config? - Configuration for the hash function in the form of a String to object map.\n"
    + "          For forensic hash TLSH (see https://github.com/trendmicro/tlsh and Jonathan Oliver, Chun Cheng, and Yanggui Chen, TLSH - A Locality Sensitive Hash. 4th Cybercrime and Trustworthy Computing Workshop, Sydney, November 2013):\n"
    + "          - bucketSize : This defines the size of the hash created.  Valid values are 128 (default) or 256 (the former results in a 70 character hash and latter results in 134 characters) \n"
    + "          - checksumBytes : This defines how many bytes are used to capture the checksum.  Valid values are 1 (default) and 3\n"
    + "          - force : If true (the default) then a hash can be generated from as few as 50 bytes.  If false, then at least 256 bytes are required.  Insufficient variation or size in the bytes result in a null being returned.\n"
    + "          - hashes : You can compute a second hash for use in fuzzy clustering TLSH signatures.  The number of hashes is the lever to adjust the size of those clusters and \"fuzzy\" the clusters are.  If this is specified, then one or more bins are created based on the specified size and the function will return a Map containing the bins.\n"
    + "          For all other hashes:\n"
    + "          - charset : The character set to use (UTF8 is default). \n"
    },
    returns = "A hex encoded string of a hashed value using the given algorithm. If 'hashType' is null " +
      "then '00', padded to the necessary length, will be returned. If 'toHash' is not able to be hashed or " +
      "'hashType' is null then null is returned."
  )
  public static class Hash extends BaseStellarFunction {

    @Override
    public Object apply(final List<Object> args) {
      if (args == null || args.size() < 2) {
        throw new IllegalArgumentException("Invalid number of arguments: " + (args == null ? 0 : args.size()));
      }

      final Object toHash = args.get(0);
      final Object hashType = args.get(1);
      if (hashType == null) {
        return null;
      }

      Map<String, Object> config = null;
      if(args.size() > 2) {
        Object configObj = args.get(2);
        if(configObj instanceof Map && configObj != null) {
          config = (Map<String, Object>)configObj;
        }
      }
      try {
        return HashStrategy.getHasher(hashType.toString(), Optional.ofNullable(config)).getHash(toHash);
      } catch (final EncoderException e) {
        return null;
      } catch (final NoSuchAlgorithmException e) {
        throw new IllegalArgumentException("Invalid hash type: " + hashType.toString());
      }
    }
  }

  @Stellar(
    name = "DIST",
    namespace="TLSH",
    params = {
          "hash1 - The first TLSH hash",
          "hash2 - The first TLSH hash",
          "includeLength? - Include the length in the distance calculation or not?",
          },
    description = "Will return the hamming distance between two TLSH hashes (note: must be computed with the same params).  " +
            "For more information, see https://github.com/trendmicro/tlsh and Jonathan Oliver, Chun Cheng, and Yanggui Chen, TLSH - A Locality Sensitive Hash. 4th Cybercrime and Trustworthy Computing Workshop, Sydney, November 2013. " +
            "For a discussion of tradeoffs, see Table II on page 5 of https://github.com/trendmicro/tlsh/blob/master/TLSH_CTC_final.pdf",
    returns = "An integer representing the distance between hash1 and hash2.  The distance is roughly hamming distance, so 0 is very similar."
  )
  public static class TlshDist extends BaseStellarFunction {

    @Override
    public Integer apply(final List<Object> args) {
      if (args == null || args.size() < 2) {
        throw new IllegalArgumentException("Invalid call. This function requires at least 2 arguments: the two TLSH hashes.");
      }
      Object h1Obj = args.get(0);
      Object h2Obj = args.get(1);
      if(h1Obj != null && !(h1Obj instanceof String) ) {
        throw new IllegalArgumentException(h1Obj + " must be strings");
      }
      if(h2Obj != null && !(h2Obj instanceof String) ) {
        throw new IllegalArgumentException(h2Obj + " must be strings");
      }

      Optional<Boolean> includeLength = Optional.empty();
      if(args.size() > 2) {
        Object includeLengthArg = args.get(2);
        if(includeLengthArg != null) {
          includeLength = Optional.ofNullable(ConversionUtils.convert(includeLengthArg, Boolean.class));
        }
      }
      return TLSH.distance(h1Obj == null?null:h1Obj.toString(), h2Obj == null?null:h2Obj.toString(), includeLength);
    }
  }
}
