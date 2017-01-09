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
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

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

}
