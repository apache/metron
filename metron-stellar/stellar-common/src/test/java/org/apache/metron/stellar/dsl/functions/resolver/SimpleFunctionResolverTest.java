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

import com.google.common.collect.Lists;
import org.apache.metron.stellar.dsl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the SimpleFunctionResolver class.
 */
public class SimpleFunctionResolverTest {

  private SimpleFunctionResolver resolver;

  @BeforeEach
  public void setup() {
    resolver = new SimpleFunctionResolver();
  }

  @Test
  public void testFunctionResolution() {
    resolver.withClass(IAmAFunction.class);
    List<String> functions = Lists.newArrayList(resolver.getFunctions());
    assertEquals(1, functions.size());
    assertTrue(functions.contains("namespace_function"));
  }

  /**
   * The function resolver should be able to instantiate an instance of the function's implementation.
   */
  @Test
  public void testApply() {
    resolver.withClass(IAmAFunction.class);
    final String functionName = "namespace_function";
    StellarFunction fn = resolver.apply(functionName);
    assertTrue(fn instanceof IAmAFunction);
  }

  /**
   * All Stellar functions must be annotated.
   */
  @Test
  public void testFunctionResolutionWithMissingAnnotation() {
    resolver.withClass(MissingAnnotation.class);
    List<String> functions = Lists.newArrayList(resolver.getFunctions());
    assertEquals(0, functions.size());
  }

  /**
   * If the resolver comes across the same function definition twice, nothing bad should happen.
   */
  @Test
  public void testIgnoreDuplicates() {
    resolver.withClass(IAmAFunction.class);
    resolver.withClass(IAmAFunction.class);
    List<String> functions = Lists.newArrayList(resolver.getFunctions());
    assertEquals(1, functions.size());
  }

  /**
   * I am the real deal.  I am a Stellar function.
   */
  @Stellar(namespace="namespace", name="function", description="description", returns="returns", params={"param1"})
  private static class IAmAFunction extends BaseStellarFunction {

    public IAmAFunction() {
    }

    @Override
    public Object apply(List<Object> args) {
      return null;
    }
  }

  /**
   * This is not a Stellar function.  It implements StellarFunction, but is not annotated.
   */
  private static class MissingAnnotation implements StellarFunction {

    public MissingAnnotation() {
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      return null;
    }

    @Override
    public void initialize(Context context) {
    }

    @Override
    public boolean isInitialized() {
      return false;
    }
  }
}
