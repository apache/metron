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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the 'PROFILE_GET' function in the {@link GetProfile} class.
 */
public class GetProfileTest {

  private StellarStatefulExecutor executor;
  private FunctionResolver functionResolver;
  private Map<String, Object> globals;
  private GetProfile function;
  private ProfilerClient profilerClient;
  private List<Integer> results;
  private ProfileMeasurement expected;
  private ProfileMeasurement defaultMeasurement;
  private Object defaultValue;

  private List run(String expression) {
    return executor.execute(expression, new HashMap<>(), List.class);
  }

  @Before
  public void setup() {
    // the mock profiler client used to feed profile measurement values to the function
    profilerClient = mock(ProfilerClient.class);

    // the PROFILE_GET function that will be tested
    function = new GetProfile.Builder()
            .withProfilerClientFactory(globals -> profilerClient)
            .build();

    // global properties
    globals = new HashMap<>();
    Context context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> globals)
            .build();

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

    defaultValue = 7777777;
    defaultMeasurement = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(System.currentTimeMillis(), 5, TimeUnit.MINUTES)
            .withProfileValue(defaultValue);
  }

  @Test
  public void testGetProfileValue() {
    // only one profile measurement exists
    when(profilerClient.fetch(
            eq(Object.class),
            eq(expected.getProfileName()),
            eq(expected.getEntity()),
            eq(expected.getGroups()),
            any(),
            any())).thenReturn(Arrays.asList(expected));

    // expect the one measurement to be returned
    results = run("PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))");
    assertEquals(1, results.size());
    assertEquals(expected.getProfileValue(), results.get(0));
  }

  @Test
  public void testGetProfileValueWithGroup() {
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
    results = run("PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), ['group1'])");
    assertEquals(1, results.size());
    assertEquals(expected.getProfileValue(), results.get(0));
  }

  @Test
  public void testGetProfileValueWithDifferentGroup() {
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
    results = run("PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), ['group999'])");
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
    results = run("PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))");
    assertEquals(0, results.size());
  }

  @Test
  public void shouldUseDefaultValueFromGlobals() {
    // set a default value
    globals.put("profiler.default.value", defaultValue);

    // the underlying profile client is responsible for returning the default value, if no profiles are found
    when(profilerClient.fetch(
            eq(Object.class),
            eq(expected.getProfileName()),
            eq(expected.getEntity()),
            eq(expected.getGroups()),
            any(),
            eq(Optional.of(defaultValue)))).thenReturn(Arrays.asList(defaultMeasurement));

    results = run("PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))");
    assertEquals(1, results.size());
    assertEquals(defaultValue, results.get(0));
  }

  @Test
  public void shouldUseDefaultValueFromOverrides() {
    // the underlying profile client is responsible for returning the default value, if no profiles are found
    when(profilerClient.fetch(
            eq(Object.class),
            eq(expected.getProfileName()),
            eq(expected.getEntity()),
            eq(expected.getGroups()),
            any(),
            eq(Optional.of(defaultValue)))).thenReturn(Arrays.asList(defaultMeasurement));

    // set the default value in the overrides map
    String overrides = String.format( "{ 'profiler.default.value': %s }", defaultValue);
    results = run("PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), [], " + overrides + ")");
    assertEquals(1, results.size());
    assertEquals(defaultValue, results.get(0));
  }
}
