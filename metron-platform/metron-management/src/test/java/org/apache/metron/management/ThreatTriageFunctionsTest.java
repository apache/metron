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
import org.apache.metron.stellar.common.shell.VariableResult;
import org.apache.metron.stellar.dsl.*;
import org.apache.metron.threatintel.triage.ThreatTriageProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;
import static org.apache.metron.management.EnrichmentConfigFunctionsTest.emptyTransformationsConfig;
import static org.apache.metron.management.EnrichmentConfigFunctionsTest.toMap;
import static org.junit.jupiter.api.Assertions.*;

public class ThreatTriageFunctionsTest {

  String configStr = emptyTransformationsConfig();
  Map<String, VariableResult> variables;
  Context context = null;

 @BeforeEach
  public void setup() {
    variables = ImmutableMap.of(
            "less", VariableResult.withExpression(true, "1 < 2"),
            "greater", VariableResult.withExpression(false, "1 > 2")
    );

    context = new Context.Builder()
            .with(Context.Capabilities.SHELL_VARIABLES, () -> variables)
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
    assertEquals("MIN", sensorConfig.getThreatIntel().getTriageConfig().getAggregator().toString());
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
    assertEquals("MIN", sensorConfig.getThreatIntel().getTriageConfig().getAggregator().toString());

    // validate that the engine was updated
    assertEquals("MIN", engine.getSensorConfig().getThreatIntel().getTriageConfig().getAggregator().toString());
  }

  @Test
  public void testAddEmpty() {

    String newConfig = (String) run(
            "THREAT_TRIAGE_ADD(config, { 'rule' : SHELL_GET_EXPRESSION('less'), 'score' : 10 } )"
            , toMap("config", configStr
            )
    );

    List<RiskLevelRule> triageRules = getTriageRules(newConfig);
    assertEquals(1, triageRules.size());
    RiskLevelRule rule = triageRules.get(0);
    assertEquals(variables.get("less").getExpression().get(), rule.getRule() );
    assertEquals("10", rule.getScoreExpression());
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
    assertEquals(1, triageRules.size());

    // validate that the engine was updated
    assertEquals(1, engine.getSensorConfig().getThreatIntel().getTriageConfig().getRiskLevelRules().size());
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
            , toMap("config", newConfig
            )
    );

    List<RiskLevelRule> triageRules = getTriageRules(newConfig);
    assertEquals(2, triageRules.size());
    RiskLevelRule less = triageRules.get(0);
    assertEquals(variables.get("less").getExpression().get(), less.getRule());
    assertEquals("10", less.getScoreExpression());

    RiskLevelRule greater = triageRules.get(1);
    assertEquals(variables.get("greater").getExpression().get(), greater.getRule());
    assertEquals("20", greater.getScoreExpression());
  }

  @Test
  public void testAddMalformed() {
    assertThrows(
        ParseException.class,
        () ->
            run(
                "THREAT_TRIAGE_ADD(config, { 'rule': SHELL_GET_EXPRESSION('foo'), 'score' : 10 } )",
                toMap("config", configStr)));
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
    assertEquals(1, triageRules.size());
    RiskLevelRule rule = triageRules.get(0);
    assertEquals(variables.get("less").getExpression().get(), rule.getRule() );
    assertEquals("10", rule.getScoreExpression());
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
    assertEquals(2, triageRules.size());
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
    assertEquals(2, triageRules.size());
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
    assertEquals(1, triageRules.size());
    RiskLevelRule rule = triageRules.get(0);
    assertEquals(variables.get("less").getExpression().get(), rule.getRule() );
    assertEquals("10", rule.getScoreExpression());
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
    assertEquals(1, triageRules.size());
    RiskLevelRule rule = triageRules.get(0);
    assertEquals(variables.get("less").getExpression().get(), rule.getRule() );
    assertEquals("10", rule.getScoreExpression());
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
    assertEquals(0, triageRules.size());
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
    assertEquals(2, triageRules.size());
    RiskLevelRule less = triageRules.get(0);
    assertEquals(variables.get("less").getExpression().get(), less.getRule() );
    assertEquals("10", less.getScoreExpression());

    RiskLevelRule greater = triageRules.get(1);
    assertEquals(variables.get("greater").getExpression().get(), greater.getRule() );
    assertEquals("20", greater.getScoreExpression());
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
    assertEquals(testPrintExpected, out);
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
    assertEquals(testPrintExpected, out);
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
    assertEquals(testPrintEmptyExpected, out);
  }

  @Test
  public void testPrintNull() {
    Map<String,Object> variables = new HashMap<String,Object>(){{
      put("config", null);
    }};
    assertThrows(ParseException.class, () -> run("THREAT_TRIAGE_PRINT(config)", variables));
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
    assertNotNull(result);
    assertTrue(result instanceof ThreatTriageProcessor);

    // there should be no triage rules defined
    ThreatTriageProcessor engine = (ThreatTriageProcessor) result;
    assertEquals(0, engine.getRiskLevelRules().size());
  }

  @Test
  public void testTriageInitWithArg() {

    // add a triage rule
    String confWithRule = (String) run("THREAT_TRIAGE_ADD(conf, [{ 'rule': 'value > 0', 'score' : 10 } ])",
            toMap("conf", configStr));

    // initialize the engine
    Object result = run("THREAT_TRIAGE_INIT(confWithRule)",
            toMap("confWithRule", confWithRule));

    assertNotNull(result);
    assertTrue(result instanceof ThreatTriageProcessor);

    // validate that there is 1 triage rule
    ThreatTriageProcessor engine = (ThreatTriageProcessor) result;
    assertEquals(1, engine.getRiskLevelRules().size());
  }

  @Test
  public void testTriageInitWithBadArg() {
      assertThrows(ParseException.class, () -> run("THREAT_TRIAGE_INIT(missing)"));
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
    assertNotNull(result);
    assertTrue(result instanceof Map);

    // validate the rules that were scored
    Map<String, Object> score = (Map) result;
    assertEquals(0, ((List) score.get(ThreatTriageFunctions.RULES_KEY)).size());

    // validate the total score
    Object totalScore = score.get(ThreatTriageFunctions.SCORE_KEY);
    assertTrue(totalScore instanceof Double);
    assertEquals(0.0, (Double) totalScore, 0.001);
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
    assertNotNull(result);
    assertTrue(result instanceof Map);

    // validate the rules that were scored
    Map<String, Object> score = (Map) result;
    assertEquals(1, ((List) score.get(ThreatTriageFunctions.RULES_KEY)).size());

    // validate the total score
    Object totalScore = score.get(ThreatTriageFunctions.SCORE_KEY);
    assertTrue(totalScore instanceof Double);
    assertEquals(10.0, (Double) totalScore, 0.001);

    // validate the aggregator
    assertEquals("MAX", score.get(ThreatTriageFunctions.AGG_KEY));
  }

  @Test
  public void testTriageWithScoreExpression() {

    // add a triage rule that uses an expression for the score
    String confWithRule = (String) run("THREAT_TRIAGE_ADD(conf, [{ 'rule': 'value > 0', 'score' : 'value * 10' }])",
            toMap("conf", configStr));

    // initialize the engine
    Object engine = run("THREAT_TRIAGE_INIT(confWithRule)",
            toMap("confWithRule", confWithRule));

    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);
    vars.put("msg", message);

    // score the message
    Object result = run("THREAT_TRIAGE_SCORE(msg, engine)", vars);
    assertNotNull(result);
    assertTrue(result instanceof Map);

    // validate the rules that were scored
    Map<String, Object> score = (Map) result;
    assertEquals(1, ((List) score.get(ThreatTriageFunctions.RULES_KEY)).size());

    // validate the total score
    Object totalScore = score.get(ThreatTriageFunctions.SCORE_KEY);
    assertTrue(totalScore instanceof Double);
    assertEquals(220.0, (Double) totalScore, 0.001);

    // validate the aggregator
    assertEquals("MAX", score.get(ThreatTriageFunctions.AGG_KEY));
  }

  @Test
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
    assertThrows(Exception.class, () -> run("THREAT_TRIAGE_SCORE(11, engine)", vars));
  }

  @Test
  public void testTriageConfig() {

    // init the engine
    Object engine = run("THREAT_TRIAGE_INIT()");

    Map<String, Object> vars = new HashMap<>();
    vars.put("engine", engine);

    // score the message
    Object result = run("THREAT_TRIAGE_CONFIG(engine)", vars);
    assertNotNull(result);
    assertTrue(result instanceof String);

    // validate the configuration
    String json = (String) result;
    assertEquals(emptyTransformationsConfig(), json);
  }
}
