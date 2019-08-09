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
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.EnrichmentLookupFactory;
import org.apache.metron.enrichment.lookup.FakeEnrichmentLookup;
import org.apache.metron.hbase.client.FakeHBaseConnectionFactory;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static org.apache.metron.enrichment.stellar.SimpleHBaseEnrichmentFunctions.CONNECTION_FACTORY_IMPL_CONF;
import static org.apache.metron.enrichment.stellar.SimpleHBaseEnrichmentFunctions.EnrichmentExists;
import static org.apache.metron.enrichment.stellar.SimpleHBaseEnrichmentFunctions.EnrichmentGet;

public class SimpleHBaseEnrichmentFunctionsTest {
  private static final String ENRICHMENT_TYPE = "et";
  private static Context context;
  private EnrichmentExists existsFunction;
  private EnrichmentGet getFunction;

  @Before
  public void setup() throws Exception {
    Map<String, Object> globals = of(CONNECTION_FACTORY_IMPL_CONF, FakeHBaseConnectionFactory.class.getName());

    // execution context for the functions
    context = new Context.Builder()
            .with( Context.Capabilities.GLOBAL_CONFIG, () -> globals)
            .build();

    // provides the enrichment data to the functions
    FakeEnrichmentLookup lookup = new FakeEnrichmentLookup()
            .withEnrichment(new EnrichmentKey(ENRICHMENT_TYPE, "indicator0"), new EnrichmentValue(of("key0", "value0")))
            .withEnrichment(new EnrichmentKey(ENRICHMENT_TYPE, "indicator1"), new EnrichmentValue(of("key1", "value1")))
            .withEnrichment(new EnrichmentKey(ENRICHMENT_TYPE, "indicator2"), new EnrichmentValue(of("key2", "value2")))
            .withEnrichment(new EnrichmentKey(ENRICHMENT_TYPE, "indicator3"), new EnrichmentValue(of("key3", "value3")))
            .withEnrichment(new EnrichmentKey(ENRICHMENT_TYPE, "indicator4"), new EnrichmentValue(of("key4", "value4")));

    EnrichmentLookupFactory lookupCreator = (connFactory, tableName, columnFamily, accessTracker) -> lookup;

    // the ENRICHMENT_EXIST function to test
    existsFunction = new EnrichmentExists()
            .withEnrichmentLookupCreator(lookupCreator);
    existsFunction.initialize(context);

    // the ENRICHMENT_GET function to test
    getFunction = new EnrichmentGet()
            .withEnrichmentLookupCreator(lookupCreator);
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
            .withClass(EnrichmentExists.class);
    return processor.parse(rule, variableResolver, functionResolver, context);
  }

  @Test
  public void testExists() {
    // 'indicator0' exists, so ENRICHMENT_EXISTS('et','indicator0','enrichments','cf') == true
    List<Object> args = Arrays.asList("et", "indicator0", "enrichments", "cf");
    Object result = existsFunction.apply(args, context);
    Assert.assertEquals(true, result);
  }

  @Test
  public void testNotExists() {
    // 'indicator99' does not exist, so ENRICHMENT_EXISTS('et','indicator99','enrichments','cf') == false
    List<Object> args = Arrays.asList("et", "indicator99", "enrichments", "cf");
    Object result = existsFunction.apply(args, context);
    Assert.assertEquals(false, result);
  }

  @Test
  public void testSuccessfulGet() {
    // 'indicator0' exists, so ENRICHMENT_GET('et','indicator0','enrichments','cf') == { "key0": "value0" }
    List<Object> args = Arrays.asList("et", "indicator0", "enrichments", "cf");
    Object result = getFunction.apply(args, context);
    Map<String, Object> out = (Map<String, Object>) result;
    Assert.assertEquals("value0", out.get("key0"));
  }

  @Test
  public void testUnsuccessfulGet() {
    // 'indicator99' does not exist, so ENRICHMENT_GET('et','indicator99','enrichments','cf') == { }
    List<Object> args = Arrays.asList("et", "indicator99", "enrichments", "cf");
    Object result = getFunction.apply(args, context);
    Map<String, Object> out = (Map<String, Object>) result;
    Assert.assertTrue(out.isEmpty());
  }

  @Test(expected = ParseException.class)
  public void testMissingRequiredParameters() {
    run("ENRICHMENT_GET('et', indicator)", ImmutableMap.of("indicator", "indicator7"));
  }
}
