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

package org.apache.metron.common.configuration.enrichment.threatintel;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.junit.Test;

import java.util.List;

import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ThreatTriageConfigTest {

  /**
   * {
   *   "enrichment": {
   *   },
   *   "threatIntel": {
   *     "triageConfig": {
   *       "riskLevelRules": [
   *         {
   *           "name": "Rule Name",
   *           "comment": "Rule Comment",
   *           "reason": "'Rule Reason'",
   *           "rule": "ip_src_addr == '10.0.2.3'",
   *           "score": 10
   *         }
   *       ]
   *     }
   *   }
   * }
   */
  @Multiline
  private String triageRuleWithNumericScore;

  @Test
  public void shouldAllowNumericRuleScore() throws Exception {
    // deserialize
    SensorEnrichmentConfig enrichment = (SensorEnrichmentConfig) ENRICHMENT.deserialize(triageRuleWithNumericScore);

    ThreatTriageConfig threatTriage = enrichment.getThreatIntel().getTriageConfig();
    assertNotNull(threatTriage);

    List<RiskLevelRule> rules = threatTriage.getRiskLevelRules();
    assertEquals(1, rules.size());

    RiskLevelRule rule = rules.get(0);
    assertEquals("Rule Name", rule.getName());
    assertEquals("Rule Comment", rule.getComment());
    assertEquals("ip_src_addr == '10.0.2.3'", rule.getRule());
    assertEquals("'Rule Reason'", rule.getReason());
    assertEquals("10", rule.getScoreExpression());
  }

  /**
   * {
   *   "enrichment": {
   *   },
   *   "threatIntel": {
   *     "triageConfig": {
   *       "riskLevelRules": [
   *         {
   *           "name": "Rule Name",
   *           "comment": "Rule Comment",
   *           "reason": "'Rule Reason'",
   *           "rule": "ip_src_addr == '10.0.2.3'",
   *           "score": "10 + 10"
   *         }
   *       ]
   *     }
   *   }
   * }
   */
  @Multiline
  private String triageRuleWithScoreExpression;

  @Test
  public void shouldAllowScoreAsStellarExpression() throws Exception {
    // deserialize the enrichment configuration
    SensorEnrichmentConfig enrichment = (SensorEnrichmentConfig) ENRICHMENT.deserialize(triageRuleWithScoreExpression);

    ThreatTriageConfig threatTriage = enrichment.getThreatIntel().getTriageConfig();
    assertNotNull(threatTriage);

    List<RiskLevelRule> rules = threatTriage.getRiskLevelRules();
    assertEquals(1, rules.size());

    RiskLevelRule rule = rules.get(0);
    assertEquals("Rule Name", rule.getName());
    assertEquals("Rule Comment", rule.getComment());
    assertEquals("'Rule Reason'", rule.getReason());
    assertEquals("ip_src_addr == '10.0.2.3'", rule.getRule());
    assertEquals("10 + 10", rule.getScoreExpression());
  }
}
