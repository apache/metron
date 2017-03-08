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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

public class ThreatTriageTest {
  /**
   * {
   *  "threatIntel": {
   *    "triageConfig": {
   *      "riskLevelRules" : [
   *        {
   *          "name" : "rule 1",
   *          "rule" : "user.type in [ 'admin', 'power' ] and asset.type == 'web'",
   *          "score" : 10
   *        },
   *        {
   *         "comment" : "web type!",
   *         "rule" : "asset.type == 'web'",
   *         "score" : 5
   *        },
   *        {
   *          "rule" : "user.type == 'normal'  and asset.type == 'web'",
   *          "score" : 0
   *        },
   *        {
   *          "rule" : "user.type in whitelist",
   *          "score" : -1
   *        }
   *      ],
   *      "aggregator" : "MAX"
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

    Assert.assertEquals(
            "Expected a score of 0",
            0d,
            new ThreatTriageProcessor(
                    new SensorEnrichmentConfig(),
                    StellarFunctions.FUNCTION_RESOLVER(),
                    Context.EMPTY_CONTEXT()).apply(
                            new HashMap<Object, Object>() {{
                              put("user.type", "admin");
                              put("asset.type", "web");
                            }}),
            1e-10);

    Assert.assertEquals(
            "Expected a score of 10",
            10d,
            threatTriageProcessor.apply(
                    new HashMap<Object, Object>() {{
                      put("user.type", "admin");
                      put("asset.type", "web");
                    }}
            ),
            1e-10);

    Assert.assertEquals(
            "Expected a score of 5",
            5d,
            threatTriageProcessor.apply(
                    new HashMap<Object, Object>() {{
                      put("user.type", "normal");
                      put("asset.type", "web");
                    }}
            ),
            1e-10);

    Assert.assertEquals(
            "Expected a score of 0",
            0d,
            threatTriageProcessor.apply(
                    new HashMap<Object, Object>() {{
                      put("user.type", "foo");
                      put("asset.type", "bar");
                    }}),
            1e-10);

    Assert.assertEquals(
            "Expected a score of -Inf",
            Double.NEGATIVE_INFINITY,
            threatTriageProcessor.apply(
                    new HashMap<Object, Object>() {{
                      put("user.type", "abnormal");
                      put("asset.type", "bar");
                    }}),
            1e-10);
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
  public void positiveMeanAggregationTest() throws Exception {

    ThreatTriageProcessor threatTriageProcessor = getProcessor(positiveMeanProcessorConfig);
    Assert.assertEquals(
            "Expected a score of 0",
            5d,
            threatTriageProcessor.apply(
                    new HashMap<Object, Object>() {{
                      put("user.type", "normal");
                      put("asset.type", "web");
                    }}),
            1e-10);

    Assert.assertEquals(
            "Expected a score of 7.5",
            (10 + 5)/2.0,
            threatTriageProcessor.apply(
                    new HashMap<Object, Object>() {{
                      put("user.type", "admin");
                      put("asset.type", "web");
                    }}),
            1e-10);

    Assert.assertEquals(
            "Expected a score of 0",
            0d,
            threatTriageProcessor.apply(
                    new HashMap<Object, Object>() {{
                      put("user.type", "foo");
                      put("asset.type", "bar");
                    }}),
            1e-10);
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
    Assert.assertEquals(
            10d,
            threatTriageProcessor.apply(
                    new HashMap<Object, Object>() {{
                      put("ip_dst_addr", "172.2.2.2");
                    }}),
            1e-10);
  }

  private static ThreatTriageProcessor getProcessor(String config) throws IOException {
    SensorEnrichmentConfig c = JSONUtils.INSTANCE.load(config, SensorEnrichmentConfig.class);
    return new ThreatTriageProcessor(c, StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
  }
}
