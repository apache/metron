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
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.shell.StellarExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.threatintel.triage.ThreatTriageProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;
import static org.apache.metron.management.EnrichmentConfigFunctionsTest.emptyTransformationsConfig;
import static org.apache.metron.management.EnrichmentConfigFunctionsTest.toMap;

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

  private Object run(String rule) {
    StellarProcessor processor = new StellarProcessor();
    return processor.parse(rule, new MapVariableResolver(Collections.emptyMap()), StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  /**
   * Sequentially runs a set of expressions and returns the result of the last expression.
   * @param expressions The set of expressions to execute.
   * @return The result of running the last expression.
   */
  private Object run(String... expressions) {
    Object result = null;

    for(String expression: expressions) {
      result = run(expression);
    }

   return result;
  }

  @Test
  public void testSetAggregation() {
    String newConfig = (String) run("THREAT_TRIAGE_SET_AGGREGATOR(config, 'MIN' )", toMap("config", configStr));
    SensorEnrichmentConfig sensorConfig = (SensorEnrichmentConfig) ENRICHMENT.deserialize(newConfig);
    Assert.assertEquals("MIN", sensorConfig.getThreatIntel().getTriageConfig().getAggregator().toString());
  }


  @Test
  public void testSetAggregationWithEngine() {
    // init the engine
    ThreatTriageProcessor engine = (ThreatTriageProcessor) run("THREAT_TRIAGE_INIT()");
    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);

    // set the aggregator
    String newConfig = (String) run("THREAT_TRIAGE_SET_AGGREGATOR(engine, 'MIN')", vars);

    // validate the return configuration
    SensorEnrichmentConfig sensorConfig = (SensorEnrichmentConfig) ENRICHMENT.deserialize(newConfig);
    Assert.assertEquals("MIN", sensorConfig.getThreatIntel().getTriageConfig().getAggregator().toString());

    // validate that the engine was updated
    Assert.assertEquals("MIN", engine.getSensorConfig().getThreatIntel().getTriageConfig().getAggregator().toString());
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
  public void testAddEmptyWithEngine() {
    // init the engine
    ThreatTriageProcessor engine = (ThreatTriageProcessor) run("THREAT_TRIAGE_INIT()");
    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);
    String newConfig = (String) run("THREAT_TRIAGE_ADD(engine, {'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10 } )", vars);

    // validate the returned configuration
    List<RiskLevelRule> triageRules = getTriageRules(newConfig);
    Assert.assertEquals(1, triageRules.size());

    // validate that the engine was updated
    Assert.assertEquals(1, engine.getSensorConfig().getThreatIntel().getTriageConfig().getRiskLevelRules().size());
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
  public void testAddMultiple() {

    // add a new rule
    String newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, { 'name':'rule1', 'rule':'value < 2', 'score':10 } )",
            toMap("config", configStr));

    // add another rule
    newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, { 'name':'rule2', 'rule':'value < 4', 'score':10 } )",
            toMap("config", newConfig));

    List<RiskLevelRule> triageRules = getTriageRules(newConfig);
    Assert.assertEquals(2, triageRules.size());
  }

  @Test
  public void testAddMultipleWithEngine() {

    // init the engine
    ThreatTriageProcessor engine = (ThreatTriageProcessor) run("THREAT_TRIAGE_INIT()");
    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);

    // add a new rule
    run("THREAT_TRIAGE_ADD(engine, { 'name':'rule1', 'rule':'value < 2', 'score':10 } )", vars);

    // add another rule
    run("THREAT_TRIAGE_ADD(engine, { 'name':'rule2', 'rule':'value < 4', 'score':10 } )", vars);

    List<RiskLevelRule> triageRules = engine.getRiskLevelRules();
    Assert.assertEquals(2, triageRules.size());
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
  public void testRemoveWithEngine() {
    // init the engine
    ThreatTriageProcessor engine = (ThreatTriageProcessor) run("THREAT_TRIAGE_INIT()");

    // set the aggregator
    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);

    // add 2 rules
    String newConfig = (String) run("THREAT_TRIAGE_ADD(engine, [" +
            "{ 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10 }, " +
            "{ 'rule' : SHELL_GET_EXPRESSION('greater'), 'score' : 20 } ] )", vars);

    // remove 1 rule
    newConfig = (String) run("THREAT_TRIAGE_REMOVE(engine, [ " +
            "SHELL_GET_EXPRESSION('greater')] )", vars);

    List<RiskLevelRule> triageRules = engine.getRiskLevelRules();
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
            "THREAT_TRIAGE_ADD(config, [ " +
                    "{ 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10, 'reason' : '2 + 2' }, " +
                    "{ 'rule' : SHELL_GET_EXPRESSION('greater'), 'score' : 20 } ] )"
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

  @Test
  public void testPrintWithEngine() {

    // init the engine
    ThreatTriageProcessor engine = (ThreatTriageProcessor) run("THREAT_TRIAGE_INIT()");
    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);

    // add 2 rules
    run("THREAT_TRIAGE_ADD(engine, [ " +
                    "{ 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10, 'reason' : '2 + 2' }, " +
                    "{ 'rule' : SHELL_GET_EXPRESSION('greater'), 'score' : 20 } ] )"
            , vars);

    // print
    String out = (String) run("THREAT_TRIAGE_PRINT(engine)", vars);
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

  @Test(expected = IllegalArgumentException.class)
  public void testPrintNull() {
    Map<String,Object> variables = new HashMap<String,Object>(){{
      put("config", null);
    }};
    String out = (String) run("THREAT_TRIAGE_PRINT(config)", variables);
    Assert.assertEquals(out, testPrintEmptyExpected);
  }

  /**
   * {
   *   "timestamp": 1504548045948,
   *   "source.type": "example",
   *   "value": 22
   * }
   */
  @Multiline
  private String message;

  @Test
  public void testTriageInitNoArg() {
    Object result = run("THREAT_TRIAGE_INIT()");
    Assert.assertNotNull(result);
    Assert.assertTrue(result instanceof ThreatTriageProcessor);

    // there should be no triage rules defined
    ThreatTriageProcessor engine = (ThreatTriageProcessor) result;
    Assert.assertEquals(0, engine.getRiskLevelRules().size());
  }

  @Test
  public void testTriageInitWithArg() {

    // add a triage rule
    String confWithRule = (String) run("THREAT_TRIAGE_ADD(conf, [{ 'rule': 'value > 0', 'score' : 10 } ])",
            toMap("conf", configStr));

    // initialize the engine
    Object result = run("THREAT_TRIAGE_INIT(confWithRule)",
            toMap("confWithRule", confWithRule));

    Assert.assertNotNull(result);
    Assert.assertTrue(result instanceof ThreatTriageProcessor);

    // validate that there is 1 triage rule
    ThreatTriageProcessor engine = (ThreatTriageProcessor) result;
    Assert.assertEquals(1, engine.getRiskLevelRules().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTriageInitWithBadArg() {
    run("THREAT_TRIAGE_INIT(missing)");
  }

  @Test
  public void testTriageScoreWithNoRules() {

    // init the engine
    Object engine = run("THREAT_TRIAGE_INIT()");

    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);
    vars.put("msg", message);

    // score the message
    Object result = run("THREAT_TRIAGE_SCORE(msg, engine)", vars);
    Assert.assertNotNull(result);
    Assert.assertTrue(result instanceof Map);

    // validate the rules that were scored
    Map<String, Object> score = (Map) result;
    Assert.assertEquals(0, ((List) score.get(ThreatTriageFunctions.RULES_KEY)).size());

    // validate the total score
    Object totalScore = score.get(ThreatTriageFunctions.SCORE_KEY);
    Assert.assertTrue(totalScore instanceof Double);
    Assert.assertEquals(0.0, (Double) totalScore, 0.001);
  }

  @Test
  public void testTriageScoreWithRules() {

    // add a triage rule
    String confWithRule = (String) run("THREAT_TRIAGE_ADD(conf, [{ 'rule': 'value > 0', 'score' : 10 }])",
            toMap("conf", configStr));

    // initialize the engine
    Object engine = run("THREAT_TRIAGE_INIT(confWithRule)",
            toMap("confWithRule", confWithRule));

    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);
    vars.put("msg", message);

    // score the message
    Object result = run("THREAT_TRIAGE_SCORE(msg, engine)", vars);
    Assert.assertNotNull(result);
    Assert.assertTrue(result instanceof Map);

    // validate the rules that were scored
    Map<String, Object> score = (Map) result;
    Assert.assertEquals(1, ((List) score.get(ThreatTriageFunctions.RULES_KEY)).size());

    // validate the total score
    Object totalScore = score.get(ThreatTriageFunctions.SCORE_KEY);
    Assert.assertTrue(totalScore instanceof Double);
    Assert.assertEquals(10.0, (Double) totalScore, 0.001);

    // validate the aggregator
    Assert.assertEquals("MAX", score.get(ThreatTriageFunctions.AGG_KEY));
  }

  @Test(expected = Exception.class)
  public void testTriageScoreWithNoMessage() {

    // add a triage rule
    String confWithRule = (String) run("THREAT_TRIAGE_ADD(conf, [{ 'rule': 'value > 0', 'score' : 10 }])",
            toMap("conf", configStr));

    // initialize the engine
    Object engine = run("THREAT_TRIAGE_INIT(confWithRule)",
            toMap("confWithRule", confWithRule));

    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);

    // score the message
    run("THREAT_TRIAGE_SCORE(11, engine)", vars);
  }

  @Test
  public void testTriageConfig() {

    // init the engine
    Object engine = run("THREAT_TRIAGE_INIT()");

    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);

    // score the message
    Object result = run("THREAT_TRIAGE_CONFIG(engine)", vars);
    Assert.assertNotNull(result);
    Assert.assertTrue(result instanceof String);

    // validate the configuration
    String json = (String) result;
    Assert.assertEquals(emptyTransformationsConfig(), json);
  }
}
