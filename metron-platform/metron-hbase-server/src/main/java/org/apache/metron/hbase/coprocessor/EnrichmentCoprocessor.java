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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheWriter;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle collecting a list of enrichment coprocessors.
 * <p>
 * <b>Note:</b> We need to be careful of our exception handling so as not to inadvertantly get our
 * coprocessor disabled by the RegionServer. From the HBase documentation:
 * <p>
 * For all functions, exception handling is done as follows:
 * <ul>
 *   <li>Exceptions of type IOException are reported back to client.</li>
 *   <li>For any other kind of exception:</li>
 *   <ul>
 *     <li>If the configuration CoprocessorHost.ABORT_ON_ERROR_KEY is set to true, then the server aborts.</li>
 *     <li>Otherwise, coprocessor is removed from the server and DoNotRetryIOException is returned to the client.</li>
 *   </ul>
 * </ul>
 *
 * @see <a href="https://hbase.apache.org/devapidocs/org/apache/hadoop/hbase/coprocessor/RegionObserver.html">https://hbase.apache.org/devapidocs/org/apache/hadoop/hbase/coprocessor/RegionObserver.html</a>
 */
public class EnrichmentCoprocessor extends BaseRegionObserver {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Cache<String, String> cache;
  private RegionCoprocessorEnvironment coprocessorEnv;

  public EnrichmentCoprocessor() {
  }

  /**
   * Allow injecting writer used by Caffeine cache.
   *
   * @param cacheWriter
   */
  public EnrichmentCoprocessor(CacheWriter<String, String> cacheWriter) {
    this.cache = Caffeine.newBuilder().writer(cacheWriter).build();
  }

  @Override
  public void start(CoprocessorEnvironment ce) throws IOException {
    if (ce instanceof RegionCoprocessorEnvironment) {
      this.coprocessorEnv = (RegionCoprocessorEnvironment) ce;
    } else {
      throw new CoprocessorException("Enrichment coprocessor must be loaded on a table region.");
    }
    if (null == this.cache) {
      CacheWriter<String, String> cacheWriter = new HBaseCacheWriter(
          this.coprocessorEnv.getConfiguration(), c -> {
        try {
          return ConnectionFactory.createConnection(c);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
      this.cache = Caffeine.newBuilder().writer(cacheWriter).build();
    }
  }

  @Override
  public void postPut(ObserverContext<RegionCoprocessorEnvironment> e, Put put, WALEdit edit,
      Durability durability) throws IOException {
    try {
      String type = getEnrichmentType(put);
      addToCache(type);
    } catch (Throwable t) {
      LOG.warn("Exception occurred while processing Put operation in coprocessor", t);
      // Anything other than an IOException will cause the coprocessor to be disabled.
      throw new IOException("Error occurred while processing enrichment Put.", t);
    }
  }

  private String getEnrichmentType(Put put) {
    EnrichmentKey key = new EnrichmentKey();
    key.fromBytes(put.getRow());
    return key.type;
  }

  private void addToCache(String cacheKey) {
    // We don't want to invoke the cache writer unless we have a new key
    if (null == cache.getIfPresent(cacheKey)) {
      // Only the key is important to us since we're using the cache like a list.
      cache.put(cacheKey, cacheKey);
    }
  }

}
