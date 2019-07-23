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
import org.apache.curator.framework.CuratorFramework;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessor;
import org.apache.hadoop.hbase.coprocessor.RegionObserver;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link RegionCoprocessor} responsible for instantiating the {@link EnrichmentRegionObserver} which
 * observes individual puts made to an HBase table.
 */
public class EnrichmentCoprocessor implements RegionCoprocessor {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /*
   * Must define the property 'zookeeperUrl' in the configuration options using
   * hbase-site.xml or the hbase shell.  This allows the coprocessor to retrieve
   * the global configuration from Zookeeper.
   *
   * https://hbase.apache.org/1.1/book.html#load_coprocessor_in_shell
   */
  public static final String ZOOKEEPER_URL = "zookeeperUrl";
  public static final String COLUMN_QUALIFIER = "v";

  private EnrichmentRegionObserver regionObserver;
  private GlobalConfigService globalConfigService;

  /**
   * Default constructor used when the coprocessor is loaded by HBase.
   */
  public EnrichmentCoprocessor() {
    this(defaultGlobalConfigService());
  }

  /**
   * Constructor used for testing.
   * @param globalConfigService
   */
  public EnrichmentCoprocessor(GlobalConfigService globalConfigService) {
    this.globalConfigService = globalConfigService;
  }

  @Override
  public Optional<RegionObserver> getRegionObserver() {
    if(regionObserver == null) {
      throw new IllegalStateException("The region observer has not been created, but should have been.");
    }
    return Optional.of(regionObserver);
  }

  @Override
  public void start(CoprocessorEnvironment ce) {
    LOG.info("Starting enrichment coprocessor");
    if(regionObserver == null) {
      CacheWriter<String, String> writer = createCacheWriter(ce);
      Cache<String, String> cache = Caffeine.newBuilder().writer(writer).build();
      regionObserver = new EnrichmentRegionObserver(cache);
    }

    LOG.info("Finished starting enrichment coprocessor");
  }

  private CacheWriter<String, String> createCacheWriter(CoprocessorEnvironment ce) {
    // get the global config
    String zkUrl = getZookeeperUrl(ce.getConfiguration());
    Map<String, Object> globalConfig = globalConfigService.get(zkUrl);

    // create the cache writer
    String tableName = getValue(globalConfig, EnrichmentConfigurations.TABLE_NAME);
    String columnFamily = getValue(globalConfig, EnrichmentConfigurations.COLUMN_FAMILY);
    return new HBaseCacheWriter(ce.getConfiguration(), tableName, columnFamily, COLUMN_QUALIFIER);
  }

  private String getValue(Map<String, Object> globals, String key) {
    String value = (String) globals.get(key);
    if(value == null) {
      throw new IllegalStateException(String.format("Missing required global property; '%s'", key));
    }
    return value;
  }

  private String getZookeeperUrl(Configuration config) {
    String zkUrl = config.get(ZOOKEEPER_URL);
    if (null == zkUrl) {
      String msg = String.format("Enrichment coprocessor missing required property '%s'", ZOOKEEPER_URL);
      throw new IllegalStateException(msg);
    }
    return zkUrl;
  }

  private static GlobalConfigService defaultGlobalConfigService() {
    return zookeeperUrl -> {
      LOG.debug("About to connect to Zookeeper at '{}'", zookeeperUrl);
      try (CuratorFramework client = ConfigurationsUtils.getClient(zookeeperUrl)) {
        client.start();
        return ConfigurationsUtils.readGlobalConfigFromZookeeper(client);
      } catch (Exception e) {
        throw new IllegalStateException("Unable to read global configuration from zookeeper", e);
      }
    };
  }
}
