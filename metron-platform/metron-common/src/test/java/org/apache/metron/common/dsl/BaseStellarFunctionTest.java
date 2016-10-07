package org.apache.metron.common.dsl;

import org.apache.storm.shade.com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the BaseStellarFunction class.
 */
public class BaseStellarFunctionTest {

  @Test
  public void testHasRequiredParams() {
    new NeedsIntegersFunction().validate(ImmutableList.of(22, 44));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingRequiredParam() {
    new NeedsIntegersFunction().validate(ImmutableList.of(22));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIncorrectParamType() {
    new NeedsIntegersFunction().validate(ImmutableList.of(22, "integer needed here... oops"));
  }

  /**
   * Nulls need to satisfy any required parameter type.
   */
  @Test
  public void testNullsAreSufficient() {
    List params = new ArrayList() {{
      add(null);
      add(null);
    }};
    new NeedsIntegersFunction().validate(params);
  }

  /**
   * A Stellar function used only for testing that required two integers.
   */
  @Stellar(
          name = "NeedsIntegersFunction",
          requiredParams = { Integer.class, Integer.class })
  public static class NeedsIntegersFunction extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      validate(args);
      return null;
    }
  }

}
