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
package org.apache.metron.stellar.common.utils.hashing.tlsh;

import com.trendmicro.tlsh.BucketOption;
import com.trendmicro.tlsh.ChecksumOption;
import com.trendmicro.tlsh.Tlsh;
import com.trendmicro.tlsh.TlshCreator;

import java.util.Optional;

/**
 * The abstraction around interacting with TLSH.
 */
public class TLSH {
  TlshCreator creator;
  public TLSH(BucketOption bucketOption, ChecksumOption checksumOption) {
    creator = new TlshCreator(bucketOption, checksumOption);
  }

  public String apply(byte[] data, boolean force) {
    try {
      creator.update(data);
      return creator.getHash(force).getEncoded();
    }
    finally {
      creator.reset();
    }
  }

  public static int distance(String hash1, String hash2, Optional<Boolean> includeLength) {
    if(hash1 == null || hash2 == null && hash1 != hash2) {
      return -1;
    }
    Tlsh t1 = Tlsh.fromTlshStr(hash1);
    Tlsh t2 = Tlsh.fromTlshStr(hash2);
    return t1.totalDiff(t2, includeLength.orElse(false));
  }
}
