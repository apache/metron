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
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.apache.curator.framework.CuratorFramework;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle collecting a list of enrichment coprocessors.
 * <p>
 * Configuration supplied via Metron global config from Zookeeper. Requires one property on startup
 * - "zookeeperUrl" - which can be provided via HBase shell or hbase-site.xml. The typical installation
 * mechanism provided by Metron will leverage the HBase shell.
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
 * @see EnrichmentConfigurations Available options.
 */
public class EnrichmentCoprocessor extends BaseRegionObserver {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  // pass in via coprocessor config options - via hbase shell or hbase-site.xml
  // see more here - https://hbase.apache.org/1.1/book.html#load_coprocessor_in_shell
  public static final String ZOOKEEPER_URL = "zookeeperUrl";
  public static final String COLUMN_QUALIFIER = "v";
  private Cache<String, String> cache;
  private GlobalConfigService globalConfigService;
  private RegionCoprocessorEnvironment coprocessorEnv;
  private Map<String, Object> globalConfig;

  /**
   * HBase requires a no-arg constructor.
   */
  public EnrichmentCoprocessor() {
  }

  /**
   * Allow test dep injection.
   */
  public EnrichmentCoprocessor(CacheWriter<String, String> cacheWriter,
      GlobalConfigService globalConfigService) {
    this.cache = Caffeine.newBuilder().writer(cacheWriter).build();
    this.globalConfigService = globalConfigService;
  }

  /**
   * Allow test dep injection.
   */
  public EnrichmentCoprocessor(GlobalConfigService globalConfigService) {
    this.globalConfigService = globalConfigService;
  }

  @Override
  public void start(CoprocessorEnvironment ce) throws IOException {
    LOG.info("Starting enrichment coprocessor");
    if (ce instanceof RegionCoprocessorEnvironment) {
      this.coprocessorEnv = (RegionCoprocessorEnvironment) ce;
    } else {
      throw new CoprocessorException("Enrichment coprocessor must be loaded on a table region.");
    }
    LOG.info("Checking if internal cache initialized");
    if (null == this.cache) {
      LOG.info("Cache null, initializing");
      LOG.info("Getting global config from Zookeeper");
      String zkUrl = getZookeeperUrl(this.coprocessorEnv.getConfiguration());
      if (null == globalConfigService) {
        globalConfigService = getGlobalConfigService(zkUrl);
      }
      globalConfig = globalConfigService.get();
      Configuration config = this.coprocessorEnv.getConfiguration();
      CacheWriter<String, String> cacheWriter = null;
      try {
        String hbaseTableProviderName = (String) globalConfig
            .get(EnrichmentConfigurations.TABLE_PROVIDER);
        String tableName = (String) globalConfig.get(EnrichmentConfigurations.TABLE_NAME);
        String columnFamily = (String) globalConfig.get(EnrichmentConfigurations.COLUMN_FAMILY);
        cacheWriter = new HBaseCacheWriter(config, TableProvider
            .create(hbaseTableProviderName, HTableProvider::new), tableName, columnFamily,
            COLUMN_QUALIFIER);
      } catch (ClassNotFoundException | InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
        throw new IOException("Unable to instantiate cache writer", e);
      }
      this.cache = Caffeine.newBuilder().writer(cacheWriter).build();
      LOG.info("Finished initializing cache");
    }
    LOG.info("Finished starting enrichment coprocessor");
  }

  private String getZookeeperUrl(Configuration config) {
    String zkUrl = config.get(ZOOKEEPER_URL);
    if (null == zkUrl) {
      throw new IllegalStateException(
          "Enrichment coprocessor requires property '" + ZOOKEEPER_URL
              + "' to be provided at startup.");
    }
    return zkUrl;
  }

  private GlobalConfigService getGlobalConfigService(String zkUrl) {
    return new GlobalConfigService() {
      @Override
      public Map<String, Object> get() {
        try (CuratorFramework client = ConfigurationsUtils.getClient(zkUrl)) {
          client.start();
          return ConfigurationsUtils.readGlobalConfigFromZookeeper(client);
        } catch (Exception e) {
          throw new IllegalStateException("Unable to read global configuration from zookeeper", e);
        }
      }
    };
  }

  @Override
  public void postPut(ObserverContext<RegionCoprocessorEnvironment> e, Put put, WALEdit edit,
      Durability durability) throws IOException {
    LOG.trace("enrichment coprocessor postPut call begin");
    try {
      LOG.trace("Extracting enrichment type from rowkey");
      String type = getEnrichmentType(put);
      // Make the value json so we can add metadata at a later time, if desired.
      final String metadata = "{}";
      LOG.trace("Enrichment type '{}' extracted from rowkey", type);
      addToCache(type, metadata);
    } catch (Throwable t) {
      LOG.warn("Exception occurred while processing Put operation in coprocessor", t);
      // Anything other than an IOException will cause the coprocessor to be disabled.
      throw new IOException("Error occurred while processing enrichment Put.", t);
    }
    LOG.trace("enrichment coprocessor postPut call complete");
  }

  private String getEnrichmentType(Put put) {
    EnrichmentKey key = new EnrichmentKey();
    key.fromBytes(put.getRow());
    return key.type;
  }

  private void addToCache(String cacheKey, String value) {
    LOG.trace("Checking if cacheKey '{}'present in cache", cacheKey);
    // We don't want to invoke the cache writer unless we have a new key
    if (null == cache.getIfPresent(cacheKey)) {
      LOG.trace("cacheKey '{}' not present, adding with value='{}' to cache", cacheKey, value);
      cache.put(cacheKey, value);
      LOG.trace("Done adding cacheKey '{}' to cache with value='{}'", cacheKey, value);
    }
  }

}
