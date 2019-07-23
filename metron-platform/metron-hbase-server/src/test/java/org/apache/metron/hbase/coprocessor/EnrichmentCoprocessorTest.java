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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link EnrichmentCoprocessor}.
 */
public class EnrichmentCoprocessorTest {

  private EnrichmentCoprocessor enrichmentCoprocessor;
  private CoprocessorEnvironment environment;
  private Configuration config;
  private Map<String, Object> globalConfig;

  @Before
  public void setup() {
    // required global configuration values
    globalConfig = new HashMap<>();
    globalConfig.put(EnrichmentConfigurations.TABLE_NAME, "enrichments");
    globalConfig.put(EnrichmentConfigurations.COLUMN_FAMILY, "columnFamily");

    // the coprocessor configuration needs to define the zookeeper URL
    config = HBaseConfiguration.create();
    config.set(EnrichmentCoprocessor.ZOOKEEPER_URL, "zookeeper:2181");

    // the environment needs to return the configuration to the coprocessor
    environment = mock(CoprocessorEnvironment.class);
    when(environment.getConfiguration()).thenReturn(config);
  }

  @Test
  public void shouldCreateRegionObserver() {
    enrichmentCoprocessor = new EnrichmentCoprocessor(zookeeperUrl -> globalConfig);
    enrichmentCoprocessor.start(environment);

    // the region observer and its cache should be instantiated on start
    EnrichmentRegionObserver observer = (EnrichmentRegionObserver) enrichmentCoprocessor.getRegionObserver().get();
    assertNotNull(observer);
    assertNotNull(observer.getCache());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldErrorIfMissingZookeeperURL() {
    // do NOT define the Zookeeper URL within the HBase configuration
    config = HBaseConfiguration.create();

    // the environment needs to return the configuration to the coprocessor
    environment = mock(CoprocessorEnvironment.class);
    when(environment.getConfiguration()).thenReturn(config);

    enrichmentCoprocessor = new EnrichmentCoprocessor(zookeeperUrl -> globalConfig);
    enrichmentCoprocessor.start(environment);
    fail("expected error due to missing Zookeeper URL");
  }

  @Test(expected = IllegalStateException.class)
  public void shouldErrorIfMissingTableName() {
    // do NOT define the table name within the global config
    globalConfig = new HashMap<>();
    globalConfig.put(EnrichmentConfigurations.COLUMN_FAMILY, "columnFamily");

    enrichmentCoprocessor = new EnrichmentCoprocessor(zookeeperUrl -> globalConfig);
    enrichmentCoprocessor.start(environment);
    fail(String.format("expected error due to missing '%s' configuration", EnrichmentConfigurations.TABLE_NAME));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldErrorIfMissingColumnFamily() {
    // do NOT define the column family within the global config
    globalConfig = new HashMap<>();
    globalConfig.put(EnrichmentConfigurations.TABLE_NAME, "enrichments");

    enrichmentCoprocessor = new EnrichmentCoprocessor(zookeeperUrl -> globalConfig);
    enrichmentCoprocessor.start(environment);
    fail(String.format("expected error due to missing '%s' configuration", EnrichmentConfigurations.COLUMN_FAMILY));
  }

}
