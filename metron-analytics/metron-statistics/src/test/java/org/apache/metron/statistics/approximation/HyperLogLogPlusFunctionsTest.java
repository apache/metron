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

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HyperLogLogPlusFunctionsTest {

  @Test
  public void hllp_init_creates_HyperLogLogPlus_set() {
    HyperLogLogPlus hllp = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of());
    assertThat(hllp.getSp(), equalTo(25));
    assertThat(hllp.getP(), equalTo(14));
    assertThat("instance types should match for constructor with default precision values", new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of(5)), instanceOf(HyperLogLogPlus.class));
    assertThat("instance types should match for constructor with sparse set disabled", new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of(5)), instanceOf(HyperLogLogPlus.class));
    assertThat("instance types should match for full constructor", new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of(5, 6)), instanceOf(HyperLogLogPlus.class));
  }

  @Test
  public void hllp_init_with_incorrect_args_throws_exception() {
    Exception e = assertThrows(IllegalArgumentException.class, () -> new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of("turkey")));
    assertEquals("Unable to get p value from 'turkey'", e.getMessage());
  }

  @Test
  public void hllp_add_returns_hllp_with_item_added_to_set() {
    HyperLogLogPlus actual = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    actual = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(actual, "item-1"));
    actual = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(actual, "item-2"));
    HyperLogLogPlus expected = new HyperLogLogPlus(5, 6);
    expected.add("item-1");
    expected.add("item-2");
    assertThat("hllp set should have cardinality based on added values", actual.cardinality(), equalTo(2L));
    assertThat("estimators should be equal", actual, equalTo(expected));
  }

  @Test
  public void hllp_add_with_null_set_inits_and_returns_new_hllp_with_item_added_to_set() {
    HyperLogLogPlus actual = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(Arrays.asList(null, "item-1"));
    assertThat(actual, notNullValue());
  }

  @Test
  public void hllp_add_throws_exception_with_incorrect_args() {
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new HyperLogLogPlusFunctions.HLLPAdd()
                    .apply(
                        ImmutableList.of(
                            new HyperLogLogPlusFunctions.HLLPInit()
                                .apply(ImmutableList.of(5, 6)))));
    assertEquals(
        "Must pass an hllp estimator set and at least one value to add to the set", e.getMessage());
  }

  @Test
  public void hllp_cardinality_returns_number_of_distinct_values() {
    HyperLogLogPlus hllp = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(hllp, "item-1"));
    hllp = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(hllp, "item-2"));
    hllp = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(hllp, "item-3"));
    assertThat("cardinality not expected value", new HyperLogLogPlusFunctions.HLLPCardinality().apply(ImmutableList.of(hllp)), equalTo(3L));
  }

  @Test
  public void hllp_cardinality_returns_0_for_null_set() {
    List<Object> nullArg = new ArrayList<Object>() {{
      add(null);
    }};
    assertThat("Cardinality should be 0", new HyperLogLogPlusFunctions.HLLPCardinality().apply(nullArg), equalTo(0L));
  }

  @Test
  public void hllp_merge_combines_hllp_sets() {
    HyperLogLogPlus hllp1 = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp1 = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(hllp1, "item-1"));
    hllp1 = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(hllp1, "item-2"));

    HyperLogLogPlus hllp2 = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp2 = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(hllp2, "item-3"));
    HyperLogLogPlus merged = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPMerge().apply(ImmutableList.of(ImmutableList.of(hllp1, hllp2)));

    Long actual = (Long) new HyperLogLogPlusFunctions.HLLPCardinality().apply(ImmutableList.of(merged));
    assertThat("cardinality should match merged set", actual, equalTo(3L));

    HyperLogLogPlus hllp3 = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp3 = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(hllp3, "item-4"));
    merged = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPMerge().apply(ImmutableList.of(ImmutableList.of(hllp1, hllp2, hllp3)));

    actual = (Long) new HyperLogLogPlusFunctions.HLLPCardinality().apply(ImmutableList.of(merged));
    assertThat("cardinality should match merged set", actual, equalTo(4L));
  }

  @Test
  public void hllp_merge_with_single_estimator_acts_as_identity_function() {
    HyperLogLogPlus hllp1 = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp1 = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(hllp1, "item-1"));
    hllp1 = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPAdd().apply(ImmutableList.of(hllp1, "item-2"));

    HyperLogLogPlus merged = (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPMerge().apply(ImmutableList.of(hllp1));

    Long actual = (Long) new HyperLogLogPlusFunctions.HLLPCardinality().apply(ImmutableList.of(merged));
    assertThat("cardinality should match merged set", actual, equalTo(2L));
  }

  @Test
  public void hllp_merge_throws_exception_with_no_arguments() {
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> new HyperLogLogPlusFunctions.HLLPMerge().apply(ImmutableList.of()));
    assertEquals("Must pass single list of hllp sets to merge", e.getMessage());
  }

  @Test
  public void hllp_merge_throws_exception_on_invalid_arguments() {
    HyperLogLogPlus hllp1 =
        (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of());
    HyperLogLogPlus hllp2 =
        (HyperLogLogPlus) new HyperLogLogPlusFunctions.HLLPInit().apply(ImmutableList.of());
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> new HyperLogLogPlusFunctions.HLLPMerge().apply(ImmutableList.of(hllp1, hllp2)));
    assertEquals("Must pass single list of hllp sets to merge", e.getMessage());
  }

  @Test
  public void merge_returns_null_if_passed_an_empty_list_to_merge() {
    List emptyList = ImmutableList.of();
    assertThat("Should be empty list", new HyperLogLogPlusFunctions.HLLPMerge().apply(ImmutableList.of(emptyList)), equalTo(null));
  }

}
