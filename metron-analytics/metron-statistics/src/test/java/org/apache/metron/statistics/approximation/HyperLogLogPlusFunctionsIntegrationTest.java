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
package org.apache.metron.statistics.approximation;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class HyperLogLogPlusFunctionsIntegrationTest {

  private Map<String, Object> values = new HashMap<String, Object>() {{
    put("val1", "mike");
    put("val2", "miklavcic");
    put("val3", "metron");
    put("val4", "batman");
    put("nullArg", null);
  }};

  /**
   *HLLP_CARDINALITY(
   *  HLLP_ADD(
   *    HLLP_ADD(
   *      HLLP_INIT(),
   *      val1
   *    ),
   *    val2
   *  )
   *)
   */
  @Multiline
  private static String hllpDefaultConstructorRule;

  @Test
  public void cardinality_gives_distinct_value_estimate_for_default_constructor() {
    Long estimate = (Long) StellarProcessorUtils.run(hllpDefaultConstructorRule, values);
    assertThat("Incorrect cardinality returned", estimate, equalTo(2L));
  }

  /**
   *HLLP_CARDINALITY(
   *  HLLP_ADD(
   *    HLLP_ADD(
   *      HLLP_INIT(5, 6),
   *      val1
   *    ),
   *    val2
   *  )
   *)
   */
  @Multiline
  private static String hllpBasicRule;

  @Test
  public void cardinality_gives_distinct_value_estimate_with_precisions_set() {
    Long estimate = (Long) StellarProcessorUtils.run(hllpBasicRule, values);
    assertThat("Incorrect cardinality returned", estimate, equalTo(2L));
  }

  /**
   *HLLP_CARDINALITY(
   *  HLLP_ADD(
   *    HLLP_INIT(5, 6),
   *    [
   *      val1,
   *      val2,
   *      val3,
   *      val4
   *    ]
   *  )
   *)
   */
  @Multiline
  private static String hllpMultipleAddItems;

  @Test
  public void hllp_add_accepts_multiple_items() {
    Long estimate = (Long) StellarProcessorUtils.run(hllpMultipleAddItems, values);
    assertThat("Incorrect cardinality returned", estimate, equalTo(4L));
  }

  /**
   *HLLP_CARDINALITY(
   *  HLLP_MERGE(
   *    [
   *      HLLP_ADD(HLLP_ADD(HLLP_INIT(5, 6), val1), val2),
   *      HLLP_ADD(HLLP_ADD(HLLP_INIT(5, 6), val3), val4)
   *    ]
   *  )
   *)
   */
  @Multiline
  private static String hllpMergeRule;

  @Test
  public void merges_estimators() {
    Long estimate = (Long) StellarProcessorUtils.run(hllpMergeRule, values);
    assertThat("Incorrect cardinality returned", estimate, equalTo(4L));
  }

  /**
   *HLLP_CARDINALITY(nullArg)
   */
  @Multiline
  private static String zeroCardinalityRule;

  @Test
  public void cardinality_of_null_value_is_0() {
    Long estimate = (Long) StellarProcessorUtils.run(zeroCardinalityRule, values);
    assertThat("Incorrect cardinality returned", estimate, equalTo(0L));
  }

}
