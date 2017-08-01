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

package org.apache.metron.enrichment.stellar;

import com.google.common.collect.ImmutableMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.common.StellarProcessor;

import org.apache.metron.enrichment.converter.EnrichmentHelper;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.test.mock.MockHTable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleHBaseEnrichmentFunctionsTest {
  private final String hbaseTableName = "enrichments";
  private static final String ENRICHMENT_TYPE = "et";
  private String cf = "cf";
  private static Context context;

  public static class TP implements TableProvider {

    @Override
    public HTableInterface getTable(Configuration config, String tableName) throws IOException {
      return MockHTable.Provider.getFromCache(tableName);
    }
  }

  @Before
  public void setup() throws Exception {

    final MockHTable hbaseTable = (MockHTable) MockHTable.Provider.addToCache(hbaseTableName, cf);
    EnrichmentHelper.INSTANCE.load(hbaseTable, cf, new ArrayList<LookupKV<EnrichmentKey, EnrichmentValue>>() {{
      for(int i = 0;i < 5;++i) {
        add(new LookupKV<>(new EnrichmentKey(ENRICHMENT_TYPE, "indicator" + i)
                        , new EnrichmentValue(ImmutableMap.of("key" + i, "value" + i))
                )
        );
      }
    }});
    context = new Context.Builder()
            .with( Context.Capabilities.GLOBAL_CONFIG
                 , () -> ImmutableMap.of( SimpleHBaseEnrichmentFunctions.TABLE_PROVIDER_TYPE_CONF
                                        , TP.class.getName()
                                        )
                 )
            .build();
  }
  public Object run(String rule, Map<String, Object> variables) throws Exception {
    StellarProcessor processor = new StellarProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule, context));
    return processor.parse(rule, new DefaultVariableResolver(x -> variables.get(x),x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  @Test
  public void testExists() throws Exception {
    String stellar = "ENRICHMENT_EXISTS('et', indicator, 'enrichments', 'cf')";
    Object result = run(stellar, ImmutableMap.of("indicator", "indicator0"));
    Assert.assertTrue(result instanceof Boolean);
    Assert.assertTrue((Boolean)result);
  }

  @Test
  public void testNotExists() throws Exception {
    String stellar = "ENRICHMENT_EXISTS('et', indicator, 'enrichments', 'cf')";
    Object result = run(stellar, ImmutableMap.of("indicator", "indicator7"));
    Assert.assertTrue(result instanceof Boolean);
    Assert.assertFalse((Boolean)result);
  }

  @Test
  public void testSuccessfulGet() throws Exception {
    String stellar = "ENRICHMENT_GET('et', indicator, 'enrichments', 'cf')";
    Object result = run(stellar, ImmutableMap.of("indicator", "indicator0"));
    Assert.assertTrue(result instanceof Map);
    Map<String, Object> out = (Map<String, Object>) result;
    Assert.assertEquals("value0", out.get("key0"));
  }

  @Test
  public void testMultiGet() throws Exception {
    String stellar = "MAP([ 'indicator0', 'indicator1' ], indicator -> ENRICHMENT_GET('et', indicator, 'enrichments', 'cf') )";
    Object result = run(stellar, new HashMap<>());
    Assert.assertTrue(result instanceof List);
    List<Map<String, Object>> out = (List<Map<String, Object>>) result;
    Assert.assertEquals(2, out.size());
    for(int i = 0;i < 2;++i) {
      Map<String, Object> map = out.get(i);
      Assert.assertEquals("value" +i, map.get("key" + i));
    }
  }
  @Test
  public void testUnsuccessfulGet() throws Exception {
    String stellar = "ENRICHMENT_GET('et', indicator, 'enrichments', 'cf')";
    Object result = run(stellar, ImmutableMap.of("indicator", "indicator7"));
    Assert.assertTrue(result instanceof Map);
    Map<String, Object> out = (Map<String, Object>) result;
    Assert.assertTrue(out.isEmpty());
  }

  @Test(expected = ParseException.class)
  public void testProvidedParameters() throws Exception {
    String stellar = "ENRICHMENT_GET('et', indicator)";
    Object result = run(stellar, ImmutableMap.of("indicator", "indicator7"));
  }
}
