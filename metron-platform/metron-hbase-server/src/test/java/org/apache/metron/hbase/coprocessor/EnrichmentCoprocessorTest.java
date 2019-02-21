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

package org.apache.metron.hbase.coprocessor;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.benmanes.caffeine.cache.CacheWriter;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EnrichmentCoprocessorTest {

  @Mock
  CacheWriter<String, String> cacheWriter;
  @Mock
  RegionCoprocessorEnvironment copEnv;
  @Mock
  ObserverContext<RegionCoprocessorEnvironment> observerContext;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void cache_writes_only_on_first_cache_miss() throws Exception {
    EnrichmentCoprocessor cop = new EnrichmentCoprocessor(cacheWriter);
    cop.start(copEnv);
    for (Map.Entry<String, Put> entry : generatePutsFromEnrichmentType("foo", "foo", "bar", "bar", "baz",
        "baz").entrySet()) {
      String type = entry.getKey();
      Put put = entry.getValue();
      cop.postPut(observerContext, put, null, null);
      verify(cacheWriter, times(1)).write(type, type);
    }
  }

  private Map<String, Put> generatePutsFromEnrichmentType(String... types) {
    Map<String, Put> puts = new HashMap<>();
    for (String type : types) {
      EnrichmentKey ek = new EnrichmentKey(type, "123");
      Put put = new Put(ek.toBytes());
      put.addColumn(Bytes.toBytes("c"), Bytes.toBytes("q"), Bytes.toBytes("blah"));
      puts.put(type, put);
    }
    return puts;
  }

}
