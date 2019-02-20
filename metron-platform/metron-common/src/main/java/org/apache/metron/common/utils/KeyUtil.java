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
package org.apache.metron.common.utils;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public enum KeyUtil {
  INSTANCE;
  private static final int SEED = 0xDEADBEEF;
  public static final int HASH_PREFIX_SIZE=16;
  ThreadLocal<HashFunction> hFunction= new ThreadLocal<HashFunction>() {
    @Override
    protected HashFunction initialValue() {
      return Hashing.murmur3_128(SEED);
    }
  };

  /**
   * Determines the prefix, given a key.
   *
   * @param key The key to get a prefix for
   * @return A 16 byte hashed prefix based on the key.
   */
  public byte[] getPrefix(byte[] key) {
    Hasher hasher = hFunction.get().newHasher();
    hasher.putBytes(key);
    return hasher.hash().asBytes();
  }

  /**
   * Merges a prefix and a key into a single byte array. Simple concatenation.
   *
   * @param prefix A byte array with the prefix
   * @param key A byte array with the key
   * @return A byte array with the prefix and key concatenated.
   */
  public byte[] merge(byte[] prefix, byte[] key) {
    byte[] val = new byte[key.length + prefix.length];
    int offset = 0;
    System.arraycopy(prefix, 0, val, offset, prefix.length);
    offset += prefix.length;
    System.arraycopy(key, 0, val, offset, key.length);
    return val;
  }

}
