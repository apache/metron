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

package org.apache.metron.common.utils;

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.common.stellar.StellarProcessor;
import org.junit.Assert;

import java.util.Collections;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.metron.common.utils.SerDeUtils.fromBytes;
import static org.apache.metron.common.utils.SerDeUtils.toBytes;

/**
 * A Stellar executor that is useful for testing Stellar expressions.
 *
 * Performs expression validation, checks serializability, and allows for complete
 * control over the execution environment, including defining the function resolver,
 * context, and defined variables.
 */
public class StellarExecutor {

  private StellarProcessor processor;
  private FunctionResolver functionResolver;
  private Context context;
  private Map<String, Object> variables;

  public StellarExecutor() {

    // define sensible defaults that can be overridden with fluent setters
    this.processor = new StellarProcessor();
    this.functionResolver = StellarFunctions.FUNCTION_RESOLVER();
    this.variables = Collections.emptyMap();
    this.context = Context.EMPTY_CONTEXT();
  }

  public <T> T execute(String expression, Class<T> clazz) {

    // validate the expression
    Assert.assertTrue(format("invalid expression: %s", expression),
            processor.validate(expression, context));

    // execute the expression
    Object result = processor.parse(expression, x -> variables.get(x), functionResolver, context);

    // validate serialization with a full round-trip
    Assert.assertEquals(result, fromBytes(toBytes(result), Object.class));

    // type conversion
    return ConversionUtils.convert(result, clazz);
  }

  public StellarExecutor withProcessor(StellarProcessor processor) {
    this.processor = processor;
    return this;
  }

  public StellarExecutor withFunctionResolver(FunctionResolver functionResolver) {
    this.functionResolver = functionResolver;
    return this;
  }

  public StellarExecutor withContext(Context context) {
    this.context = context;
    return this;
  }

  public StellarExecutor withCapability(Context.Capabilities capability, Context.Capability value) {
    context.addCapability(capability, value);
    return this;
  }

  public StellarExecutor withVariables(Map<String, Object> variables) {
    this.variables = variables;
    return this;
  }

  public StellarExecutor withVariable(String name, Object value) {
    variables.put(name, value);
    return this;
  }
}
