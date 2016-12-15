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

package org.apache.metron.common.dsl.functions;

import com.google.common.collect.ImmutableList;
import org.apache.metron.common.utils.HyperLogLogPlus;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;

public class DataStructureFunctionsTest {

  @Test
  public void is_empty_handles_happy_path() {
    DataStructureFunctions.IsEmpty isEmpty = new DataStructureFunctions.IsEmpty();
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of("hello"));
      Assert.assertThat("should be false", empty, CoreMatchers.equalTo(false));
    }
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of(ImmutableList.of("hello", "world")));
      Assert.assertThat("should be false", empty, CoreMatchers.equalTo(false));
    }
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of(1));
      Assert.assertThat("should be false", empty, CoreMatchers.equalTo(false));
    }
  }

  @Test
  public void is_empty_handles_empty_values() {
    DataStructureFunctions.IsEmpty isEmpty = new DataStructureFunctions.IsEmpty();
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of());
      Assert.assertThat("should be true", empty, CoreMatchers.equalTo(true));
    }
    {
      boolean empty = (boolean) isEmpty.apply(null);
      Assert.assertThat("should be true", empty, CoreMatchers.equalTo(true));
    }
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of(""));
      Assert.assertThat("should be true", empty, CoreMatchers.equalTo(true));
    }
  }

  @Test
  public void hllp_init_creates_HyperLogLogPlus_set() {
    Assert.assertThat("instance types should match for constructor with sparse set disabled", new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5)), instanceOf(HyperLogLogPlus.class));
    Assert.assertThat("instance types should match for full constructor", new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6)), instanceOf(HyperLogLogPlus.class));
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void hllp_init_with_incorrect_args_throws_exception() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Normal set precision is required");
    new DataStructureFunctions.HLLPInit().apply(ImmutableList.of());
  }

  @Test
  public void hllp_offer_returns_hllp_with_item_added_to_set() {
    HyperLogLogPlus actual = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    actual = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(actual, "item-1"));
    actual = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(actual, "item-2"));
    HyperLogLogPlus expected = new HyperLogLogPlus(5, 6);
    expected.offer("item-1");
    expected.offer("item-2");
    Assert.assertThat("hllp set should have cardinality based on offered values", actual.cardinality(), equalTo(2L));
    Assert.assertThat("estimators should be equal", actual, equalTo(expected));
  }

  @Test
  public void hllp_offer_throws_exception_with_incorrect_args() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Must pass an hllp set and a value to add to the set");
    new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6))));
  }

  @Test
  public void hllp_cardinality_returns_number_of_distinct_values() {
    HyperLogLogPlus hllp = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(hllp, "item-1"));
    hllp = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(hllp, "item-2"));
    hllp = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(hllp, "item-3"));
    Assert.assertThat("cardinality not expected value", new DataStructureFunctions.HLLPCardinality().apply(ImmutableList.of(hllp)), equalTo(3L));
  }

  @Test
  public void hllp_cardinality_with_invalid_arguments_throws_exception() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Must pass an hllp set to get the cardinality for");
    new DataStructureFunctions.HLLPCardinality().apply(ImmutableList.of());
  }

  @Test
  public void hllp_merge_combines_hllp_sets() {
    HyperLogLogPlus hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(hllp1, "item-1"));
    hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(hllp1, "item-2"));

    HyperLogLogPlus hllp2 = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp2 = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(hllp2, "item-3"));
    HyperLogLogPlus merged = (HyperLogLogPlus) new DataStructureFunctions.HLLPMerge().apply(ImmutableList.of(hllp1, hllp2));

    Long actual = (Long) new DataStructureFunctions.HLLPCardinality().apply(ImmutableList.of(merged));
    Assert.assertThat("cardinality should match merged set", actual, equalTo(3L));

    HyperLogLogPlus hllp3 = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp3 = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(hllp3, "item-4"));
    merged = (HyperLogLogPlus) new DataStructureFunctions.HLLPMerge().apply(ImmutableList.of(hllp1, hllp2, hllp3));

    actual = (Long) new DataStructureFunctions.HLLPCardinality().apply(ImmutableList.of(merged));
    Assert.assertThat("cardinality should match merged set", actual, equalTo(4L));
  }

  @Test
  public void hllp_merge_with_single_estimator_acts_as_identity_function() {
    HyperLogLogPlus hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(hllp1, "item-1"));
    hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPOffer().apply(ImmutableList.of(hllp1, "item-2"));

    HyperLogLogPlus merged = (HyperLogLogPlus) new DataStructureFunctions.HLLPMerge().apply(ImmutableList.of(hllp1));

    Long actual = (Long) new DataStructureFunctions.HLLPCardinality().apply(ImmutableList.of(merged));
    Assert.assertThat("cardinality should match merged set", actual, equalTo(2L));
  }

  @Test
  public void hllp_merge_throws_exception_on_invalid_arguments() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Must pass 1..n hllp sets to merge");
    new DataStructureFunctions.HLLPMerge().apply(ImmutableList.of());
  }

}
