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
package org.apache.metron.management;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.RiskLevelRule;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.shell.StellarExecutor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.management.EnrichmentConfigFunctionsTest.emptyTransformationsConfig;
import static org.apache.metron.management.EnrichmentConfigFunctionsTest.toMap;
import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;

public class ThreatTriageFunctionsTest {

  String configStr = emptyTransformationsConfig();
  Map<String, StellarExecutor.VariableResult> variables;
  Context context = null;

 @Before
  public void setup() {
    variables = ImmutableMap.of(
            "less", new StellarExecutor.VariableResult("1 < 2", true),
            "greater", new StellarExecutor.VariableResult("1 > 2", false)
    );

    context = new Context.Builder()
            .with(StellarExecutor.SHELL_VARIABLES, () -> variables)
            .build();
  }

  public static List<RiskLevelRule> getTriageRules(String config) {
    SensorEnrichmentConfig sensorConfig = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
    return sensorConfig.getThreatIntel().getTriageConfig().getRiskLevelRules();
  }

  private Object run(String rule, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    return processor.parse(rule, new DefaultVariableResolver(x -> variables.get(x),x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  @Test
  public void testSetAggregation() {

    String newConfig = (String) run(
            "THREAT_TRIAGE_SET_AGGREGATOR(config, 'MIN' )"
            , toMap("config", configStr
            )
    );

    SensorEnrichmentConfig sensorConfig = (SensorEnrichmentConfig) ENRICHMENT.deserialize(newConfig);
    Assert.assertEquals("MIN", sensorConfig.getThreatIntel().getTriageConfig().getAggregator().toString());
  }

  @Test
  public void testAddEmpty() {

    String newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, { 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10 } )"
            , toMap("config", configStr
            )
    );

    List<RiskLevelRule> triageRules = getTriageRules(newConfig);
    Assert.assertEquals(1, triageRules.size());
    RiskLevelRule rule = triageRules.get(0);
    Assert.assertEquals(variables.get("less").getExpression(), rule.getRule() );
    Assert.assertEquals(10.0, rule.getScore().doubleValue(), 1e-6 );
  }

  @Test
  public void testAddHasExisting() {

    String newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, { 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10, 'reason' : '2 + 2' } )"
            , toMap("config", configStr
            )
    );

    newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, { 'rule' : SHELL_GET_EXPRESSION('greater'), 'score' : 20 } )"
            , toMap("config",newConfig
            )
    );

    List<RiskLevelRule> triageRules = getTriageRules(newConfig);
    Assert.assertEquals(2, triageRules.size());
    RiskLevelRule less = triageRules.get(0);
    Assert.assertEquals(variables.get("less").getExpression(), less.getRule() );
    Assert.assertEquals(10.0, less.getScore().doubleValue(), 1e-6 );

    RiskLevelRule greater = triageRules.get(1);
    Assert.assertEquals(variables.get("greater").getExpression(), greater.getRule() );
    Assert.assertEquals(20.0, greater.getScore().doubleValue(), 1e-6 );
  }

  @Test(expected=IllegalStateException.class)
  public void testAddMalformed() {
    Object o = run(
            "THREAT_TRIAGE_ADD(config, { 'rule': SHELL_GET_EXPRESSION('foo'), 'score' : 10 } )"
            , toMap("config", configStr
            )
    );
    Assert.assertEquals(configStr, o);
  }

  @Test
  public void testAddDuplicate() {
    String newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, { 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10 } )"
            , toMap("config", configStr
            )
    );

    newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, { 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10 } )"
            , toMap("config",newConfig
            )
    );

    List<RiskLevelRule> triageRules = getTriageRules(newConfig);
    Assert.assertEquals(1, triageRules.size());
    RiskLevelRule rule = triageRules.get(0);
    Assert.assertEquals(variables.get("less").getExpression(), rule.getRule() );
    Assert.assertEquals(10.0, rule.getScore().doubleValue(), 1e-6 );
  }

  @Test
  public void testRemove() {
    String newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, [ { 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10 }, { 'rule' : SHELL_GET_EXPRESSION('greater'), 'score' : 20 } ] )"
            , toMap("config", configStr
            )
    );

    newConfig = (String) run(
            "THREAT_TRIAGE_REMOVE(config, [ SHELL_GET_EXPRESSION('greater')] )"
            , toMap("config",newConfig
            )
    );

    List<RiskLevelRule> triageRules = getTriageRules(newConfig);
    Assert.assertEquals(1, triageRules.size());
    RiskLevelRule rule = triageRules.get(0);
    Assert.assertEquals(variables.get("less").getExpression(), rule.getRule() );
    Assert.assertEquals(10.0, rule.getScore().doubleValue(), 1e-6 );
  }

  @Test
  public void testRemoveMultiple() {
    String newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, [ { 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10 }, { 'rule' : SHELL_GET_EXPRESSION('greater'), 'score' : 20 } ] )"
            , toMap("config", configStr )
    );

    newConfig = (String) run(
            "THREAT_TRIAGE_REMOVE(config, [ SHELL_GET_EXPRESSION('less'), SHELL_GET_EXPRESSION('greater')] )"
            , toMap("config",newConfig
            )
    );

    List<RiskLevelRule> triageRules = getTriageRules(newConfig);
    Assert.assertEquals(0, triageRules.size());
  }

  @Test
  public void testRemoveMissing() {

    String newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, [ { 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10 }, { 'rule' : SHELL_GET_EXPRESSION('greater'), 'score' : 20 } ] )"
            , toMap("config", configStr
            )
    );

    newConfig = (String) run(
            "THREAT_TRIAGE_REMOVE(config, [ SHELL_GET_EXPRESSION('foo'), SHELL_GET_EXPRESSION('bar')] )"
            , toMap("config",newConfig
            )
    );

    List<RiskLevelRule> triageRules = getTriageRules(newConfig);
    Assert.assertEquals(2, triageRules.size());
    RiskLevelRule less = triageRules.get(0);
    Assert.assertEquals(variables.get("less").getExpression(), less.getRule() );
    Assert.assertEquals(10.0, less.getScore().doubleValue(), 1e-6 );

    RiskLevelRule greater = triageRules.get(1);
    Assert.assertEquals(variables.get("greater").getExpression(), greater.getRule() );
    Assert.assertEquals(20.0, greater.getScore().doubleValue(), 1e-6 );
  }

  /**
╔══════╤═════════╤═════════════╤═══════╤════════╗
║ Name │ Comment │ Triage Rule │ Score │ Reason ║
╠══════╪═════════╪═════════════╪═══════╪════════╣
║      │         │ 1 < 2       │ 10    │ 2 + 2  ║
╟──────┼─────────┼─────────────┼───────┼────────╢
║      │         │ 1 > 2       │ 20    │        ║
╚══════╧═════════╧═════════════╧═══════╧════════╝


Aggregation: MAX*/
  @Multiline
  static String testPrintExpected;

  @Test
  public void testPrint() {

    String newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, [ { 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10, 'reason' : '2 + 2' }, { 'rule' : SHELL_GET_EXPRESSION('greater'), 'score' : 20 } ] )"
            , toMap("config", configStr
            )
    );

    String out = (String) run(
            "THREAT_TRIAGE_PRINT(config)"
            , toMap("config",newConfig
            )
    );
    Assert.assertEquals(testPrintExpected, out);
  }

  /**
╔══════╤═════════╤═════════════╤═══════╤════════╗
║ Name │ Comment │ Triage Rule │ Score │ Reason ║
╠══════╧═════════╧═════════════╧═══════╧════════╣
║ (empty)                                       ║
╚═══════════════════════════════════════════════╝
   */
  @Multiline
  static String testPrintEmptyExpected;

  @Test
  public void testPrintEmpty() {
    String out = (String) run(
            "THREAT_TRIAGE_PRINT(config)"
            , toMap("config",configStr
            )
    );
    Assert.assertEquals(testPrintEmptyExpected, out);
  }

  @Test
  public void testPrintNull() {
    Map<String,Object> variables = new HashMap<String,Object>(){{
      put("config",null);
    }};
    String out = (String) run(
            "THREAT_TRIAGE_PRINT(config)"
            , variables
    );
    Assert.assertEquals(out, testPrintEmptyExpected);
  }


}
