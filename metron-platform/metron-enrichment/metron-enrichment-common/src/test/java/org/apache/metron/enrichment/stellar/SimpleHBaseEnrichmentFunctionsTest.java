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
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.enrichment.lookup.EnrichmentLookupFactory;
import org.apache.metron.enrichment.lookup.FakeEnrichmentLookup;
import org.apache.metron.hbase.client.FakeHBaseConnectionFactory;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.common.StellarProcessor;

import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.FunctionalFunctions;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static org.apache.metron.enrichment.stellar.SimpleHBaseEnrichmentFunctions.EnrichmentExists;
import static org.apache.metron.enrichment.stellar.SimpleHBaseEnrichmentFunctions.EnrichmentGet;

public class SimpleHBaseEnrichmentFunctionsTest {
  private static final String ENRICHMENT_TYPE = "et";
  private static Context context;
  private EnrichmentExists existsFunction;
  private EnrichmentGet getFunction;

  @Before
  public void setup() throws Exception {
    // provides the enrichment data to the functions
    FakeEnrichmentLookup lookup = new FakeEnrichmentLookup()
            .withEnrichment(new EnrichmentKey(ENRICHMENT_TYPE, "indicator0"), new EnrichmentValue(of("key0", "value0")))
            .withEnrichment(new EnrichmentKey(ENRICHMENT_TYPE, "indicator1"), new EnrichmentValue(of("key1", "value1")))
            .withEnrichment(new EnrichmentKey(ENRICHMENT_TYPE, "indicator2"), new EnrichmentValue(of("key2", "value2")))
            .withEnrichment(new EnrichmentKey(ENRICHMENT_TYPE, "indicator3"), new EnrichmentValue(of("key3", "value3")))
            .withEnrichment(new EnrichmentKey(ENRICHMENT_TYPE, "indicator4"), new EnrichmentValue(of("key4", "value4")));

    context = new Context.Builder()
            .with( Context.Capabilities.GLOBAL_CONFIG
                 , () -> ImmutableMap.of( SimpleHBaseEnrichmentFunctions.CONNECTION_FACTORY_IMPL_CONF
                                        , FakeHBaseConnectionFactory.class.getName()
                                        )
                 )
            .build();

    EnrichmentLookupFactory factory = (connFact, conf, tableName, colFam, accessTracker) -> lookup;
    Configuration configuration = HBaseConfiguration.create();

    // the ENRICHMENT_EXIST function to test
    existsFunction = new EnrichmentExists(factory, configuration);
    existsFunction.initialize(context);

    // the ENRICHMENT_GET function to test
    getFunction = new EnrichmentGet(factory, configuration);
    getFunction.initialize(context);
  }

  public Object run(String rule, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule, context));

    VariableResolver variableResolver = new DefaultVariableResolver(
            x -> variables.get(x),
            x -> variables.containsKey(x));
    FunctionResolver functionResolver = new SimpleFunctionResolver()
            .withClass(EnrichmentGet.class)
            .withClass(EnrichmentExists.class)
            .withClass(FunctionalFunctions.Map.class)
            .withInstance(existsFunction)
            .withInstance(getFunction);
    return processor.parse(rule, variableResolver, functionResolver, context);
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
