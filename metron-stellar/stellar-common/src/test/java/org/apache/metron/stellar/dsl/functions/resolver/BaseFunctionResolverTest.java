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

package org.apache.metron.stellar.dsl.functions.resolver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BaseFunctionResolverTest {

  public static class TestResolver extends BaseFunctionResolver {

    Set<Class<? extends StellarFunction>> classesToResolve = new HashSet<>();

    @Override
    public Set<Class<? extends StellarFunction>> resolvables() {
      return classesToResolve;
    }

    /**
     * Will attempt to resolve any Stellar functions defined within the specified class.
     * @param clazz The class which may contain a Stellar function.
     */
    public TestResolver withClass(Class<? extends StellarFunction> clazz) {
      this.classesToResolve.add(clazz);
      return this;
    }
  }

  /**
   * Often imitated, never duplicated.
   */
  @Stellar(namespace = "namespace", name = "afunction", description = "description", returns = "returns", params = {
      "param1"})
  private static class IAmAFunction extends BaseStellarFunction {

    public static boolean teardownCalled = false;

    public IAmAFunction() {
    }

    @Override
    public Object apply(List<Object> args) {
      return null;
    }

    @Override
    public void teardown() {
      teardownCalled = true;
    }
  }

  /**
   * Scratch that. I was wrong.
   */
  @Stellar(namespace = "namespace", name = "anotherfunction", description = "description", returns = "returns", params = {
      "param1"})
  private static class IAmAnotherFunction extends BaseStellarFunction {

    public static boolean teardownCalled = false;

    public IAmAnotherFunction() {
    }

    @Override
    public Object apply(List<Object> args) {
      return null;
    }

    @Override
    public void teardown() {
      teardownCalled = true;
    }
  }

  private TestResolver resolver;

  @Before
  public void setup() {
    resolver = new TestResolver();
  }

  @Test
  public void teardown_calls_all_loaded_function_teardown_methods() {
    resolver.withClass(IAmAFunction.class);
    resolver.withClass(IAmAnotherFunction.class);
    resolver.teardown();
    Assert.assertTrue(IAmAFunction.teardownCalled);
    Assert.assertTrue(IAmAnotherFunction.teardownCalled);
  }

}
