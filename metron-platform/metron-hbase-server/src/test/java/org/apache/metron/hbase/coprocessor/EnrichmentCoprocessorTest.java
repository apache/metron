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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.CacheWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.hbase.TableProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
  private static boolean instantiatedCustomTableProvider;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    cop = new EnrichmentCoprocessor(cacheWriter, globalConfigService);
    config = HBaseConfiguration.create();
    config.set(EnrichmentCoprocessor.ZOOKEEPER_URL, "foobar");
    when(copEnv.getConfiguration()).thenReturn(config);
    instantiatedCustomTableProvider = false;
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

  public static class TestTableProvider implements TableProvider {

    public TestTableProvider() {
      instantiatedCustomTableProvider = true;
    }

    @Override
    public Table getTable(Configuration config, String tableName) throws IOException {
      return null; // not used for instantiation test
    }
    
  }

  @Test
  public void creates_tableprovider_from_config_property() throws Exception {
    cop = new EnrichmentCoprocessor(globalConfigService);
    Map<String, Object> globalConfig = new HashMap<String, Object>() {{
      put(EnrichmentConfigurations.TABLE_PROVIDER, TestTableProvider.class.getName());
    }};
    when(globalConfigService.get()).thenReturn(globalConfig);
    cop.start(copEnv);
    assertThat(instantiatedCustomTableProvider, equalTo(true));
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void bad_enrichment_key_exceptions_thrown_as_IOException() throws Exception {
    thrown.expect(IOException.class);
    thrown.expectMessage("Error occurred while processing enrichment Put.");
    thrown.expectCause(instanceOf(RuntimeException.class));
    cop.start(copEnv);
    cop.postPut(observerContext, new Put("foo".getBytes(StandardCharsets.UTF_8)), null, null);
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

}
