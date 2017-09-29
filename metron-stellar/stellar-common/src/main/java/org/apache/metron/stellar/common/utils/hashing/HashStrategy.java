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

import com.google.common.base.Joiner;
import org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHHasher;

import java.util.*;
import java.util.function.Function;

/**
 * This is an enum implementing a hashing strategy pattern.  Because hash types may
 * be quite different, but have a similar interface we have laid on top this abstraction
 * to allow new types of hashes to be added easily.
 *
 * In order to add a new family of hashes, simply implement the Hasher interface and register it with this
 * enum.  The search order for algorithms to their respective Hasher is in the order of the entries
 * in this enum.
 */
public enum HashStrategy {
  TLSH(a -> new TLSHHasher(), TLSHHasher.supportedHashes()),
  DEFAULT(a -> new DefaultHasher(a), DefaultHasher.supportedHashes())
  ;

  /**
   * An accumulated list of all the supported hash algorithms.
   */
  public static final Set<String> ALL_SUPPORTED_HASHES = new HashSet<>();
  static {
    for(HashStrategy factory : HashStrategy.values()) {
      ALL_SUPPORTED_HASHES.addAll(factory.supportedHashes);
    }
  }

  Function<String, Hasher> hasherCreator;
  Set<String> supportedHashes;

  HashStrategy(Function<String, Hasher> hasherCreator, Set<String> supportedHashes) {
    this.hasherCreator = hasherCreator;
    this.supportedHashes = supportedHashes;
  }

  /**
   * Return the appropriate hasher given the algorithm.
   * @param algorithm The algorithm to find a hasher handler for.  Note: this is upper-cased prior to search
   * @param config The config for the hasher
   * @return The hasher which will handle the algorithm.  If the algorithm is not supported by any registered
   *         hashers, an IllegalArgumentException is thrown.
   */
  public static Hasher getHasher(String algorithm, Optional<Map<String, Object>> config) {
    Hasher h = null;
    for(HashStrategy factory : HashStrategy.values()) {
      if(factory.getSupportedHashes().contains(algorithm.toUpperCase())) {
        h = factory.hasherCreator.apply(algorithm);
        break;
      }
    }
    if(h == null) {
      throw new IllegalArgumentException("Unsupported hash function: " + algorithm
                                        + ".  Supported algorithms are " + Joiner.on(",").join(ALL_SUPPORTED_HASHES));
    }
    h.configure(config);
    return h;
  }

  public Set<String> getSupportedHashes() {
    return supportedHashes;
  }
}
