package org.apache.metron.statistics.approximation;

import com.google.common.collect.ImmutableList;
import org.apache.metron.common.dsl.functions.DataStructureFunctions;
import org.apache.metron.common.utils.HyperLogLogPlus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;

public class HyperLogLogPlusTest {
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
  public void hllp_add_returns_hllp_with_item_added_to_set() {
    HyperLogLogPlus actual = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    actual = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(actual, "item-1"));
    actual = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(actual, "item-2"));
    HyperLogLogPlus expected = new HyperLogLogPlus(5, 6);
    expected.add("item-1");
    expected.add("item-2");
    Assert.assertThat("hllp set should have cardinality based on added values", actual.cardinality(), equalTo(2L));
    Assert.assertThat("estimators should be equal", actual, equalTo(expected));
  }

  @Test
  public void hllp_add_throws_exception_with_incorrect_args() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Must pass an hllp estimator set and at least one value to add to the set");
    new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6))));
  }

  @Test
  public void hllp_cardinality_returns_number_of_distinct_values() {
    HyperLogLogPlus hllp = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(hllp, "item-1"));
    hllp = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(hllp, "item-2"));
    hllp = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(hllp, "item-3"));
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
    hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(hllp1, "item-1"));
    hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(hllp1, "item-2"));

    HyperLogLogPlus hllp2 = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp2 = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(hllp2, "item-3"));
    HyperLogLogPlus merged = (HyperLogLogPlus) new DataStructureFunctions.HLLPMerge().apply(ImmutableList.of(ImmutableList.of(hllp1, hllp2)));

    Long actual = (Long) new DataStructureFunctions.HLLPCardinality().apply(ImmutableList.of(merged));
    Assert.assertThat("cardinality should match merged set", actual, equalTo(3L));

    HyperLogLogPlus hllp3 = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp3 = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(hllp3, "item-4"));
    merged = (HyperLogLogPlus) new DataStructureFunctions.HLLPMerge().apply(ImmutableList.of(ImmutableList.of(hllp1, hllp2, hllp3)));

    actual = (Long) new DataStructureFunctions.HLLPCardinality().apply(ImmutableList.of(merged));
    Assert.assertThat("cardinality should match merged set", actual, equalTo(4L));
  }

  @Test
  public void hllp_merge_with_single_estimator_acts_as_identity_function() {
    HyperLogLogPlus hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPInit().apply(ImmutableList.of(5, 6));
    hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(hllp1, "item-1"));
    hllp1 = (HyperLogLogPlus) new DataStructureFunctions.HLLPAdd().apply(ImmutableList.of(hllp1, "item-2"));

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
