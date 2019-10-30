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

package org.apache.metron.threatintel.triage;

import com.google.common.collect.ImmutableList;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.RuleScore;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatScore;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThreatTriageTest {

  private static final double delta = 1e-10;

  /**
   * {
   *  "threatIntel": {
   *    "triageConfig": {
   *      "riskLevelRules" : [
   *        {
   *          "name": "rule 1",
   *          "rule": "user.type in [ 'admin', 'power' ] and asset.type == 'web'",
   *          "score": 10
   *        },
   *        {
   *          "name": "rule 2",
   *          "comment": "web type!",
   *          "rule": "asset.type == 'web'",
   *          "score": 5
   *        },
   *        {
   *          "name": "rule 3",
   *          "rule": "user.type == 'normal' and asset.type == 'web'",
   *          "score": 0
   *        },
   *        {
   *          "name": "rule 4",
   *          "rule": "user.type in whitelist",
   *          "score": -1,
   *          "reason": "user.type"
   *        }
   *      ],
   *      "aggregator": "MAX"
   *    },
   *    "config": {
   *      "whitelist": [ "abnormal" ]
   *    }
   *  }
   * }
   */
  @Multiline
  public static String smokeTestProcessorConfig;

  @Test
  public void smokeTest() throws Exception {
    ThreatTriageProcessor threatTriageProcessor = getProcessor(smokeTestProcessorConfig);

    assertEquals(
        0d,
        new ThreatTriageProcessor(
                new SensorEnrichmentConfig(),
                StellarFunctions.FUNCTION_RESOLVER(),
                Context.EMPTY_CONTEXT())
            .apply(
                new HashMap<Object, Object>() {
                  {
                    put("user.type", "admin");
                    put("asset.type", "web");
                  }
                })
            .getScore(),
        1e-10,
        "Expected a score of 0");

    assertEquals(
        10d,
        threatTriageProcessor
            .apply(
                new HashMap<Object, Object>() {
                  {
                    put("user.type", "admin");
                    put("asset.type", "web");
                  }
                })
            .getScore(),
        1e-10,
        "Expected a score of 10");

    assertEquals(
        5d,
        threatTriageProcessor
            .apply(
                new HashMap<Object, Object>() {
                  {
                    put("user.type", "normal");
                    put("asset.type", "web");
                  }
                })
            .getScore(),
        1e-10,
        "Expected a score of 5");

    assertEquals(
        0d,
        threatTriageProcessor
            .apply(
                new HashMap<Object, Object>() {
                  {
                    put("user.type", "foo");
                    put("asset.type", "bar");
                  }
                })
            .getScore(),
        1e-10,
        "Expected a score of 0");

    assertEquals(
        Double.NEGATIVE_INFINITY,
        threatTriageProcessor
            .apply(
                new HashMap<Object, Object>() {
                  {
                    put("user.type", "abnormal");
                    put("asset.type", "bar");
                  }
                })
            .getScore(),
        1e-10,
        "Expected a score of -Inf");
  }

  /**
   * Each individual rule that was applied when scoring a threat should
   * be captured in the overall threat score.
   */
  @Test
  public void testThreatScoreWithMultipleRules() throws Exception {

    Map<Object, Object> message = new HashMap<Object, Object>() {{
      put("user.type", "admin");
      put("asset.type", "web");
    }};

    ThreatScore score = getProcessor(smokeTestProcessorConfig).apply(message);

    // expect rules 1 and 2 to have been applied
    List<String> expectedNames = ImmutableList.of("rule 1", "rule 2");
    assertEquals(2, score.getRuleScores().size());
    score.getRuleScores().forEach(ruleScore ->
            assertTrue(expectedNames.contains(ruleScore.getRule().getName()))
    );
  }

  /**
   * Each individual rule that was applied when scoring a threat should
   * be captured in the overall threat score.
   */
  @Test
  public void testThreatScoreWithOneRule() throws Exception {

    Map<Object, Object> message = new HashMap<Object, Object>() {{
      put("user.type", "abnormal");
      put("asset.type", "invalid");
    }};

    ThreatScore score = getProcessor(smokeTestProcessorConfig).apply(message);

    // expect rule 4 to have been applied
    List<String> expectedNames = ImmutableList.of("rule 4");
    assertEquals(1, score.getRuleScores().size());
    score.getRuleScores().forEach(ruleScore ->
            assertTrue(expectedNames.contains(ruleScore.getRule().getName()))
    );
  }

  /**
   * Each individual rule that was applied when scoring a threat should
   * be captured in the overall threat score.
   */
  @Test
  public void testThreatScoreWithNoRules() throws Exception {

    Map<Object, Object> message = new HashMap<Object, Object>() {{
      put("user.type", "foo");
      put("asset.type", "bar");
    }};

    ThreatScore score = getProcessor(smokeTestProcessorConfig).apply(message);

    // expect no rules to have been applied
    assertEquals(0, score.getRuleScores().size());
  }

  /**
   * {
   *  "threatIntel": {
   *  "triageConfig": {
   *    "riskLevelRules" : [
   *      {
   *        "rule" : "user.type in [ 'admin', 'power' ] and asset.type == 'web'",
   *        "score" : 10
   *      },
   *      {
   *        "rule" : "asset.type == 'web'",
   *        "score" : 5
   *      },
   *      {
   *        "rule" : "user.type == 'normal' and asset.type == 'web'",
   *        "score" : 0
   *      }
   *     ],
   *     "aggregator" : "POSITIVE_MEAN"
   *    }
   *  }
   * }
   */
  @Multiline
  public static String positiveMeanProcessorConfig;

  @Test
  public void testPositiveMeanAggregationScores() throws Exception {

    ThreatTriageProcessor threatTriageProcessor = getProcessor(positiveMeanProcessorConfig);
    assertEquals(
        5d,
        threatTriageProcessor
            .apply(
                new HashMap<Object, Object>() {
                  {
                    put("user.type", "normal");
                    put("asset.type", "web");
                  }
                })
            .getScore(),
        1e-10,
        "Expected a score of 0");

    assertEquals(
        (10 + 5) / 2.0,
        threatTriageProcessor
            .apply(
                new HashMap<Object, Object>() {
                  {
                    put("user.type", "admin");
                    put("asset.type", "web");
                  }
                })
            .getScore(),
        1e-10,
        "Expected a score of 7.5");

    assertEquals(
        0d,
        threatTriageProcessor
            .apply(
                new HashMap<Object, Object>() {
                  {
                    put("user.type", "foo");
                    put("asset.type", "bar");
                  }
                })
            .getScore(),
        1e-10,
        "Expected a score of 0");
  }

  /**
   * {
   *    "threatIntel" : {
   *      "triageConfig": {
   *        "riskLevelRules": [
   *          {
   *            "rule" : "not(IN_SUBNET(ip_dst_addr, '192.168.0.0/24'))",
   *            "score" : 10
   *          }
   *        ],
   *        "aggregator" : "MAX"
   *      }
   *    }
   * }
   */
  @Multiline
  private static String testWithStellarFunction;

  @Test
  public void testWithStellarFunction() throws Exception {
    ThreatTriageProcessor threatTriageProcessor = getProcessor(testWithStellarFunction);
    assertEquals(
            10d,
            threatTriageProcessor.apply(
                    new HashMap<Object, Object>() {{
                      put("ip_dst_addr", "172.2.2.2");
                    }}).getScore(),
            1e-10);
  }

  /**
   * {
   *  "threatIntel": {
   *    "triageConfig": {
   *      "riskLevelRules" : [
   *        {
   *          "name": "Rule Name",
   *          "comment": "Rule Comment",
   *          "rule": "2 == 2",
   *          "score": 10,
   *          "reason": "variable.name"
   *        }
   *      ],
   *      "aggregator": "MAX"
   *    }
   *  }
   * }
   */
  @Multiline
  public static String testReasonConfig;

  /**
   * The 'reason' field contained within a rule is a Stellar expression that is
   * executed within the context of the message that the rule is applied to.
   */
  @Test
  public void testReason() throws Exception {

    Map<Object, Object> message = new HashMap<Object, Object>() {{
      put("variable.name", "variable.value");
    }};

    ThreatScore score = getProcessor(testReasonConfig).apply(message);
    assertEquals(1, score.getRuleScores().size());
    for(RuleScore ruleScore : score.getRuleScores()) {

      // the 'reason' is the result of executing the rule's 'reason' expression
      assertEquals("variable.value", ruleScore.getReason());
    }
  }

  /**
   * If the 'reason' expression refers to a missing variable (the result
   * of a data quality issue) it should not throw an exception.
   */
  @Test
  public void testInvalidReason() throws Exception {

    Map<Object, Object> message = new HashMap<Object, Object>() {{
      // there is no 'variable.name' in the message
    }};

    ThreatScore score = getProcessor(testReasonConfig).apply(message);
    assertEquals(1, score.getRuleScores().size());
    for(RuleScore ruleScore : score.getRuleScores()) {

      // the 'reason' is the result of executing the rule's 'reason' expression
      assertEquals(null, ruleScore.getReason());
    }
  }

  /**
   * {
   *    "threatIntel" : {
   *      "triageConfig": {
   *        "riskLevelRules": [
   *          {
   *            "rule" : "true",
   *            "score" : 10
   *          }
   *        ],
   *        "aggregator" : "MAX"
   *      }
   *    }
   * }
   */
  @Multiline
  private static String shouldAllowNumericRuleScore;

  @Test
  public void shouldAllowNumericRuleScore() throws Exception {
    Map<String, Object> message = new HashMap<>();
    ThreatTriageProcessor threatTriageProcessor = getProcessor(shouldAllowNumericRuleScore);
    assertEquals(10d, threatTriageProcessor.apply(message).getScore(), 1e-10);
  }

  /**
   * {
   *    "threatIntel" : {
   *      "triageConfig": {
   *        "riskLevelRules": [
   *          {
   *            "rule" : "true",
   *            "score" : "priority * 10.1"
   *          }
   *        ],
   *        "aggregator" : "MAX"
   *      }
   *    }
   * }
   */
  @Multiline
  private static String shouldAllowScoreAsStellarExpression;

  @Test
  public void shouldAllowScoreAsStellarExpression() throws Exception {
    // the message being triaged has a field 'priority' that is referenced in the score expression
    Map<Object, Object> message = new HashMap<Object, Object>() {{
      put("priority", 100);
    }};

    ThreatTriageProcessor threatTriageProcessor = getProcessor(shouldAllowScoreAsStellarExpression);
    assertEquals(1010.0d, threatTriageProcessor.apply(message).getScore(), 1e-10);
  }

  private static ThreatTriageProcessor getProcessor(String config) throws IOException {
    SensorEnrichmentConfig c = JSONUtils.INSTANCE.load(config, SensorEnrichmentConfig.class);
    return new ThreatTriageProcessor(c, StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
  }
}
