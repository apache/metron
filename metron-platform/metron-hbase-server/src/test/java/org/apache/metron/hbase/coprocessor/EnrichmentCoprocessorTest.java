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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.wal.WALEdit;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EnrichmentCoprocessorTest {

  @Mock
  private CacheWriter<String, String> cacheWriter;
  @Mock
  private RegionCoprocessorEnvironment copEnv;
  @Mock
  private ObserverContext<RegionCoprocessorEnvironment> observerContext;
  private EnrichmentCoprocessor cop;
  @Mock
  private GlobalConfigService globalConfigService;
  private Configuration config;
  private CoprocessorEnvironment environment;
  private Map<String, Object> globalConfig;
  @Mock
  private WALEdit edit;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(copEnv.getConfiguration()).thenReturn(config);

    // required global configuration values
    globalConfig = new HashMap<>();
    globalConfig.put(EnrichmentConfigurations.TABLE_NAME, "enrichments");
    globalConfig.put(EnrichmentConfigurations.COLUMN_FAMILY, "columnFamily");

    // the coprocessor configuration needs to define the zookeeper URL
    config = HBaseConfiguration.create();
    config.set(EnrichmentCoprocessor.ZOOKEEPER_URL, "zookeeper:2181");

    // the environment needs to return the configuration to the coprocessor
    environment = mock(RegionCoprocessorEnvironment.class);
    when(environment.getConfiguration()).thenReturn(config);

    cop = new EnrichmentCoprocessor(cacheWriter, globalConfigService);
  }

  @Test
  public void cache_writes_only_on_first_cache_miss() throws Exception {
    cop.start(copEnv);
    String[] enrichTypes = new String[]{"foo", "bar", "baz", "metron"};
    final int putsPerType = 3;
    Map<String, List<Put>> putsByType = simulateMultiplePutsPerType(putsPerType, enrichTypes);
    int totalPuts = 0;
    for (Map.Entry<String, List<Put>> entry : putsByType.entrySet()) {
      String type = entry.getKey();
      List<Put> puts = entry.getValue();
      for (Put put : puts) {
        cop.postPut(observerContext, put, null, null);
        verify(cacheWriter, times(1)).write(eq(type), eq("{}"));
        totalPuts++;
      }
    }
    assertThat(totalPuts, equalTo(enrichTypes.length * putsPerType));
  }

  /**
   * Generate a list of 'count' puts for each type in 'types'.
   *
   * @param count Number of puts to create per type
   * @param types List of types to create the puts for.
   * @return Map of types to a List of size 'count' puts
   */
  private Map<String, List<Put>> simulateMultiplePutsPerType(int count, String... types) {
    Map<String, List<Put>> putsByType = new HashMap<>();
    for (String type : types) {
      List<Put> puts = putsByType.getOrDefault(type, new ArrayList<>());
      for (int i = 0; i < count; i++) {
        EnrichmentKey ek = new EnrichmentKey(type, String.valueOf(i));
        puts.add(new Put(ek.toBytes()));
        putsByType.put(type, puts);
      }
    }
    return putsByType;
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void bad_enrichment_key_exceptions_thrown_as_IOException() throws Exception {
    thrown.expect(IOException.class);
    thrown.expectMessage("Error occurred while processing enrichment Put.");
    thrown.expectCause(instanceOf(RuntimeException.class));
    cop.start(copEnv);
    cop.postPut(observerContext, new Put("foo".getBytes()), null, null);
  }

  @Test
  public void general_exceptions_thrown_as_IOException() throws Exception {
    Throwable cause = new Throwable("Bad things happened.");
    thrown.expect(IOException.class);
    thrown.expectMessage("Error occurred while processing enrichment Put.");
    thrown.expectCause(equalTo(cause));
    // strictly speaking, this is a checked exception not in the api for CacheWriter, but it allows
    // us to test catching all Throwable types
    willAnswer(i -> {
      throw cause;
    }).given(cacheWriter).write(any(), any());
    cop.start(copEnv);
    EnrichmentKey ek = new EnrichmentKey("foo", "bar");
    cop.postPut(observerContext, new Put(ek.toBytes()), null, null);
  }

  @Test
  public void should_cache_enrichment_type_only_once() throws IOException {
    Cache<String, String> cache = mock(Cache.class);

    final String enrichmentType = "blacklist";
    Put put = createEnrichmentPut(enrichmentType);

    // the mock cache needs to indicate that the enrichment type has already been written
    when(cache.getIfPresent(eq(enrichmentType))).thenReturn("{}");

    // the "blacklist" should not be cached a second time
    cop.postPut(observerContext, put, edit, Durability.USE_DEFAULT);
    verify(cache, times(0)).put(eq(enrichmentType), any());
  }

  @Test(expected = IOException.class)
  public void should_throw_IOException_when_invalid_enrichment() throws IOException {
    Put invalidEnrichmentPut = new Put(Bytes.toBytes("invalid"));
    cop.postPut(observerContext, invalidEnrichmentPut, edit, Durability.USE_DEFAULT);
    fail("Invalid enrichment put should have triggered an exception");
  }

  @Test(expected = IllegalStateException.class)
  public void should_error_if_missing_zookeeper_URL() throws IOException {
    // do NOT define the Zookeeper URL within the HBase configuration
    config = HBaseConfiguration.create();

    // the environment needs to return the configuration to the coprocessor
    environment = mock(RegionCoprocessorEnvironment.class);
    when(environment.getConfiguration()).thenReturn(config);

    cop = new EnrichmentCoprocessor(zookeeperUrl -> globalConfig);
    cop.start(environment);
    fail("expected error due to missing Zookeeper URL");
  }

  @Test(expected = IllegalStateException.class)
  public void should_error_if_missing_table_name() throws IOException {
    // do NOT define the table name within the global config
    globalConfig = new HashMap<>();
    globalConfig.put(EnrichmentConfigurations.COLUMN_FAMILY, "columnFamily");

    cop = new EnrichmentCoprocessor(zookeeperUrl -> globalConfig);
    cop.start(environment);
    fail(String.format("expected error due to missing '%s' configuration", EnrichmentConfigurations.TABLE_NAME));
  }

  @Test(expected = IllegalStateException.class)
  public void should_error_if_missing_column_family() throws IOException {
    // do NOT define the column family within the global config
    globalConfig = new HashMap<>();
    globalConfig.put(EnrichmentConfigurations.TABLE_NAME, "enrichments");

    cop = new EnrichmentCoprocessor(zookeeperUrl -> globalConfig);
    cop.start(environment);
    fail(String.format("expected error due to missing '%s' configuration", EnrichmentConfigurations.COLUMN_FAMILY));
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
