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
package org.apache.metron.hbase.coprocessor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.wal.WALEdit;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link EnrichmentRegionObserver}.
 */
public class EnrichmentRegionObserverTest {
  private WALEdit edit;
  private ObserverContext<RegionCoprocessorEnvironment> observerContext;
  private EnrichmentRegionObserver regionObserver;

  @Before
  public void setup() {
    edit = mock(WALEdit.class);
    observerContext = mock(ObserverContext.class);
  }

  @Test
  public void shouldCacheEnrichmentTypes() throws IOException {
    Cache<String, String> cache = Caffeine.newBuilder().build();
    regionObserver = new EnrichmentRegionObserver(cache);
    {
      // should cache the "blacklist" enrichment type
      Put put = createEnrichmentPut("blacklist");
      regionObserver.postPut(observerContext, put, edit, Durability.USE_DEFAULT);
      assertNotNull(cache.getIfPresent("blacklist"));
    }
    {
      // should cache the "whitelist" enrichment type
      Put put = createEnrichmentPut("whitelist");
      regionObserver.postPut(observerContext, put, edit, Durability.USE_DEFAULT);
      assertNotNull(cache.getIfPresent("whitelist"));
    }
  }

  @Test
  public void shouldCacheEnrichmentTypeOnlyOnce() throws IOException {
    Cache<String, String> cache = mock(Cache.class);
    regionObserver = new EnrichmentRegionObserver(cache);

    final String enrichmentType = "blacklist";
    Put put = createEnrichmentPut(enrichmentType);

    // the mock cache needs to indicate that the enrichment type has already been written
    when(cache.getIfPresent(eq(enrichmentType))).thenReturn("{}");

    // the "blacklist" should not be cached a second time
    regionObserver.postPut(observerContext, put, edit, Durability.USE_DEFAULT);
    verify(cache, times(0)).put(eq(enrichmentType), any());
  }

  @Test(expected = IOException.class)
  public void shouldThrowIOExceptionWhenInvalidEnrichment() throws IOException {
    Cache<String, String> cache = Caffeine.newBuilder().build();
    regionObserver = new EnrichmentRegionObserver(cache);

    Put invalidEnrichmentPut = new Put(Bytes.toBytes("invalid"));
    regionObserver.postPut(observerContext, invalidEnrichmentPut, edit, Durability.USE_DEFAULT);
    fail("Invalid enrichment put should have triggered an exception");
  }

  /**
   * Creates a {@link Put} that represents an enrichment value being
   * written to a table of enrichment values in HBase.
   *
   * @param enrichmentType The type of enrichment.
   * @return A {@link Put}
   */
  private Put createEnrichmentPut(String enrichmentType) {
    EnrichmentKey key = new EnrichmentKey(enrichmentType, "indicator");
    return new Put(key.toBytes());
  }
}
