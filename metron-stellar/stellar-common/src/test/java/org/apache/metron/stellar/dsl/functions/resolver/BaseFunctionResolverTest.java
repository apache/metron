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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

    public static int closeCallCount;
    public static boolean throwException = false; // init here bc of reflection in resolver.

    public IAmAFunction() {
      closeCallCount = 0;
    }

    @Override
    public Object apply(List<Object> args) {
      return null;
    }

    @Override
    public void close() throws IOException {
      closeCallCount++;
      if (throwException) {
        Throwable cause = new Throwable("Some nasty nasty cause.");
        throw new IOException("Bad things happened", cause);
      }
    }
  }

  /**
   * Scratch that. I was wrong.
   */
  @Stellar(namespace = "namespace", name = "anotherfunction", description = "description", returns = "returns", params = {
      "param1"})
  private static class IAmAnotherFunction extends BaseStellarFunction {

    public static int closeCallCount;
    public static boolean throwException = false; // init here bc of reflection in resolver.

    public IAmAnotherFunction() {
      closeCallCount = 0;
    }

    @Override
    public Object apply(List<Object> args) {
      return null;
    }

    @Override
    public void close() throws IOException {
      closeCallCount++;
      if (throwException) {
        throw new NullPointerException("A most annoying exception.");
      }
    }
  }

  private TestResolver resolver;

  @Before
  public void setup() {
    resolver = new TestResolver();
    IAmAFunction.throwException = false;
    IAmAnotherFunction.throwException = false;
  }

  @Test
  public void close_calls_all_loaded_function_close_methods() throws IOException {
    resolver.withClass(IAmAFunction.class);
    resolver.withClass(IAmAnotherFunction.class);
    resolver.close();
    assertThat(IAmAFunction.closeCallCount, equalTo(1));
    assertThat(IAmAnotherFunction.closeCallCount, equalTo(1));
  }

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void close_collects_all_exceptions_thrown_on_loaded_function_close_methods()
      throws IOException {
    IAmAFunction.throwException = true;
    IAmAnotherFunction.throwException = true;
    resolver.withClass(IAmAFunction.class);
    resolver.withClass(IAmAnotherFunction.class);
    exception.expect(IOException.class);
    resolver.close();
  }

  @Test
  public void close_only_throws_exceptions_on_first_invocation()
      throws IOException {
    IAmAFunction.throwException = true;
    IAmAnotherFunction.throwException = true;
    resolver.withClass(IAmAFunction.class);
    resolver.withClass(IAmAnotherFunction.class);
    try {
      resolver.close();
      Assert.fail("Should have thrown an exception.");
    } catch (IOException e) {
      // intentionally empty
    }
    assertThat(IAmAFunction.closeCallCount, equalTo(1));
    assertThat(IAmAnotherFunction.closeCallCount, equalTo(1));
    // should not throw exceptions or call any function's close again.
    resolver.close();
    resolver.close();
    resolver.close();
    assertThat(IAmAFunction.closeCallCount, equalTo(1));
    assertThat(IAmAnotherFunction.closeCallCount, equalTo(1));
  }
}
