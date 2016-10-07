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
          description = "A function used only for testing BaseStellarFunction",
          params = { "First is Integer", "Second is Integer" },
          requiredParams = { Integer.class, Integer.class },
          returns = "No one cares" )
  public static class NeedsIntegersFunction extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      validate(args);
      return null;
    }
  }

}
