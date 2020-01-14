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
package org.apache.metron.stellar.dsl.functions;

import com.google.common.collect.ImmutableList;
import org.apache.metron.stellar.common.system.Environment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SystemFunctionsTest {

  @Test
  public void smoke_test_non_mocked_env() {
    SystemFunctions.EnvGet envGet = new SystemFunctions.EnvGet();
    String envVal = (String) envGet.apply(ImmutableList.of("ENV_GET_VAR"));
    assertThat("Value should not exist", envVal, equalTo(null));
  }

  @Test
  public void env_get_returns_value() {
    Environment env = mock(Environment.class);
    when(env.get("ENV_GET_VAR")).thenReturn("ENV_GET_VALUE");
    SystemFunctions.EnvGet envGet = new SystemFunctions.EnvGet(env);
    String envVal = (String) envGet.apply(ImmutableList.of("ENV_GET_VAR"));
    assertThat("Value should match", envVal, equalTo("ENV_GET_VALUE"));
  }

  @Test
  public void env_get_returns_null_if_key_is_not_string() {
    SystemFunctions.EnvGet envGet = new SystemFunctions.EnvGet();
    String envVal = (String) envGet.apply(ImmutableList.of(new ArrayList()));
    assertThat("Value should be null", envVal, equalTo(null));
  }

  @Test
  public void property_get_returns_value() {
    System.getProperties().put("ENV_GET_VAR", "ENV_GET_VALUE");
    SystemFunctions.PropertyGet propertyGet = new SystemFunctions.PropertyGet();
    String propertyVal = (String) propertyGet.apply(ImmutableList.of("ENV_GET_VAR"));
    assertThat("Value should match", propertyVal, equalTo("ENV_GET_VALUE"));
  }

  @Test
  public void property_get_nonexistent_returns_null() {
    SystemFunctions.PropertyGet propertyGet = new SystemFunctions.PropertyGet();
    String propertyVal = (String) propertyGet.apply(ImmutableList.of("PROPERTY_MISSING"));
    assertThat("Value should not exist", propertyVal, equalTo(null));
  }

  @Test
  public void property_get_returns_null_if_key_is_not_string() {
    SystemFunctions.PropertyGet propertyGet = new SystemFunctions.PropertyGet();
    String propertyVal = (String) propertyGet.apply(ImmutableList.of(new ArrayList()));
    assertThat("Value should be null", propertyVal, equalTo(null));
  }

}
