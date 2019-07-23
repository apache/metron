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
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.RegionObserver;
import org.apache.hadoop.hbase.filter.ByteArrayComparable;
import org.apache.hadoop.hbase.wal.WALEdit;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * A {@link RegionObserver} that tracks the unique types of enrichments that are
 * written to an enrichments table in HBase.
 */
public class EnrichmentRegionObserver implements RegionObserver {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Cache<String, String> cache;

  public EnrichmentRegionObserver(Cache<String, String> cache) {
    this.cache = cache;
  }

  @Override
  public void prePut(ObserverContext<RegionCoprocessorEnvironment> c, Put put, WALEdit edit, Durability durability) throws IOException {
    LOG.error("IN PRE_PUT");
  }

  @Override
  public boolean preCheckAndPut(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row, byte[] family, byte[] qualifier, CompareOperator op, ByteArrayComparable comparator, Put put, boolean result) throws IOException {
    return false;
  }

  @Override
  public boolean preCheckAndPutAfterRowLock(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row, byte[] family, byte[] qualifier, CompareOperator op, ByteArrayComparable comparator, Put put, boolean result) throws IOException {
    return false;
  }

  @Override
  public boolean postCheckAndPut(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row, byte[] family, byte[] qualifier, CompareOperator op, ByteArrayComparable comparator, Put put, boolean result) throws IOException {
    return false;
  }

  @Override
  public void preClose(ObserverContext<RegionCoprocessorEnvironment> c, boolean abortRequested) throws IOException {
    LOG.error("IN PRE_CLOSE");
  }

  @Override
  public void postClose(ObserverContext<RegionCoprocessorEnvironment> c, boolean abortRequested) {
    cache.cleanUp();
  }

  @Override
  public void postPut(ObserverContext<RegionCoprocessorEnvironment> e,
                      Put put,
                      WALEdit edit,
                      Durability durability)
          throws IOException {
    LOG.trace("enrichment coprocessor postPut call begin");
    try {
      String cacheKey = getEnrichmentType(put);
      addToCache(cacheKey, getCacheValue());

    } catch (Throwable t) {
      LOG.warn("Exception occurred while processing Put operation in coprocessor", t);
      // Anything other than an IOException will cause the coprocessor to be disabled.
      throw new IOException("Error occurred while processing enrichment Put.", t);
    }
    LOG.trace("enrichment coprocessor postPut call complete");
  }

  protected Cache<String, String> getCache() {
    return cache;
  }

  private String getEnrichmentType(Put put) {
    EnrichmentKey key = new EnrichmentKey();
    key.fromBytes(put.getRow());

    LOG.trace("Enrichment type '{}' extracted from row key", key.getType());
    return key.getType();
  }

  private String getCacheValue() {
    // the value stored in the cache is not used for anything. storing a JSON map so we can
    // add metadata at a later time, if desired.
    return "{}";
  }

  private void addToCache(String cacheKey, String value) {
    LOG.trace("Checking if cacheKey '{}' is present in cache", cacheKey);
    // We don't want to invoke the cache writer unless we have a new key
    if (cache.getIfPresent(cacheKey) == null) {
      LOG.trace("cache key '{}' not present, adding it to cache", cacheKey);
      cache.put(cacheKey, value);
    }
  }
}
