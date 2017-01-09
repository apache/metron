package org.apache.metron.statistics.approximation;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.StellarProcessorUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;

public class HyperLogLogPlusFunctionsIntegrationTest {
  private Map<String, Object> values = new HashMap<String, Object>() {{
    put("val1", "mike");
    put("val2", "miklavcic");
    put("val3", "metron");
    put("val4", "batman");
  }};

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
  public void cardinality_gives_distinct_value_estimate() {
    Long estimate = (Long) StellarProcessorUtils.run(hllpBasicRule, values);
    Assert.assertThat("Incorrect cardinality returned", estimate, equalTo(2L));
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
    Assert.assertThat("Incorrect cardinality returned", estimate, equalTo(4L));
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
    Assert.assertThat("Incorrect cardinality returned", estimate, equalTo(4L));
  }
}
