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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Create a threadlocal cache of TLSH handlers.
 */
public class TLSHCache {
  public static ThreadLocal<TLSHCache> INSTANCE = ThreadLocal.withInitial(() -> new TLSHCache());
  private Map<Map.Entry<BucketOption, ChecksumOption>, TLSH> cache = new HashMap<>();
  private TLSHCache() {}

  public TLSH getTLSH(BucketOption bo, ChecksumOption co) {
    return cache.computeIfAbsent( new AbstractMap.SimpleEntry<>(bo, co)
                                , kv -> new TLSH(kv.getKey(), kv.getValue())
                                );
  }
}
