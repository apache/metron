/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.profiler.client.stellar;

import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.client.ProfilerClient;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the 'PROFILE_VERBOSE' function in the {@link VerboseProfile} class.
 */
public class VerboseProfileTest {

  private StellarStatefulExecutor executor;
  private FunctionResolver functionResolver;
  private Map<String, Object> globals;
  private VerboseProfile function;
  private ProfilerClient profilerClient;
  private List<Map<String, Object>> results;
  private ProfileMeasurement expected;

  private List run(String expression) {
    return executor.execute(expression, new HashMap<>(), List.class);
  }

  @Before
  public void setup() {
    // the mock profiler client used to feed profile measurement values to the function
    profilerClient = mock(ProfilerClient.class);

    // global properties
    globals = new HashMap<>();
    Context context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> globals)
            .build();

    // the VERBOSE_PROFILE function that will be tested
    function = new VerboseProfile(globals -> profilerClient);
    function.initialize(context);

    // create the stellar execution environment
    functionResolver = new SimpleFunctionResolver()
            .withClass(FixedLookback.class)
            .withInstance(function);
    executor = new DefaultStellarStatefulExecutor(functionResolver, context);

    // create a profile measurement used in the tests
    expected = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(System.currentTimeMillis(), 5, TimeUnit.MINUTES)
            .withProfileValue(1231121);
  }

  @Test
  public void shouldRenderVerboseView() {
    // only one profile measurement exists
    when(profilerClient.fetch(
            eq(Object.class),
            eq(expected.getProfileName()),
            eq(expected.getEntity()),
            eq(expected.getGroups()),
            any(),
            any())).thenReturn(Arrays.asList(expected));

    // expect the one measurement to be returned
    results = run("PROFILE_VERBOSE('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))");
    assertEquals(1, results.size());
    Map<String, Object> actual = results.get(0);

    // the measurement should be rendered as a map containing detailed information about the profile measurement
    assertEquals(expected.getProfileName(),                 actual.get("profile"));
    assertEquals(expected.getEntity(),                      actual.get("entity"));
    assertEquals(expected.getPeriod().getPeriod(),          actual.get("period"));
    assertEquals(expected.getPeriod().getStartTimeMillis(), actual.get("period.start"));
    assertEquals(expected.getPeriod().getEndTimeMillis(),   actual.get("period.end"));
    assertEquals(expected.getProfileValue(),                actual.get("value"));
    assertEquals(expected.getGroups(),                      actual.get("groups"));
  }

  @Test
  public void shouldRenderVerboseViewWithGroup() {
    // the profile measurement is part of a group
    expected.withGroups(Arrays.asList("group1"));

    // only one profile measurement exists
    when(profilerClient.fetch(
            eq(Object.class),
            eq(expected.getProfileName()),
            eq(expected.getEntity()),
            eq(expected.getGroups()),
            any(),
            any())).thenReturn(Arrays.asList(expected));

    // expect the one measurement to be returned
    results = run("PROFILE_VERBOSE('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), ['group1'])");
    assertEquals(1, results.size());
    Map<String, Object> actual = results.get(0);

    // the measurement should be rendered as a map containing detailed information about the profile measurement
    assertEquals(expected.getProfileName(),                 actual.get("profile"));
    assertEquals(expected.getEntity(),                      actual.get("entity"));
    assertEquals(expected.getPeriod().getPeriod(),          actual.get("period"));
    assertEquals(expected.getPeriod().getStartTimeMillis(), actual.get("period.start"));
    assertEquals(expected.getPeriod().getEndTimeMillis(),   actual.get("period.end"));
    assertEquals(expected.getProfileValue(),                actual.get("value"));
    assertEquals(expected.getGroups(),                      actual.get("groups"));
  }

  @Test
  public void shouldRenderVerboseViewWithDifferentGroup() {
    // the profile measurement is part of a group
    expected.withGroups(Arrays.asList("group1"));

    // only one profile measurement exists
    when(profilerClient.fetch(
            eq(Object.class),
            eq(expected.getProfileName()),
            eq(expected.getEntity()),
            eq(expected.getGroups()),
            any(),
            any())).thenReturn(Arrays.asList(expected));

    // the profile measurement is not part of 'group999'
    results = run("PROFILE_VERBOSE('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), ['group999'])");
    assertEquals(0, results.size());
  }

  @Test
  public void shouldReturnNothingWhenNoMeasurementsExist() {
    // no measurements exist
    when(profilerClient.fetch(
            eq(Object.class),
            any(),
            any(),
            any(),
            any(),
            any())).thenReturn(Collections.emptyList());

    // no measurements exist
    results = run("PROFILE_VERBOSE('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))");
    assertEquals(0, results.size());
  }

  @Test
  public void shouldReturnDefaultValue() {
    // set a default value
    globals.put("profiler.default.value", expected);

    // the underlying profile client needs to be setup to return the default
    when(profilerClient.fetch(
            eq(Object.class),
            eq(expected.getProfileName()),
            eq(expected.getEntity()),
            eq(expected.getGroups()),
            any(),
            eq(Optional.of(expected)))).thenReturn(Arrays.asList(expected));

    results = run("PROFILE_VERBOSE('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))");
    assertEquals(1, results.size());
    Map<String, Object> actual = results.get(0);

    // no measurements exist, but we expect the default value to be returned
    assertEquals(expected.getProfileName(),                 actual.get("profile"));
    assertEquals(expected.getEntity(),                      actual.get("entity"));
    assertEquals(expected.getPeriod().getPeriod(),          actual.get("period"));
    assertEquals(expected.getPeriod().getStartTimeMillis(), actual.get("period.start"));
    assertEquals(expected.getPeriod().getEndTimeMillis(),   actual.get("period.end"));
    assertEquals(expected.getProfileValue(),                actual.get("value"));
    assertEquals(expected.getGroups(),                      actual.get("groups"));
  }
}
