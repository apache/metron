/*
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

package org.apache.metron.stellar.common;

import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.Token;
import org.apache.metron.stellar.dsl.VariableResolver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


public class LambdaExpression extends StellarCompiler.Expression {
  StellarCompiler.ExpressionState state;
  List<String> variables;
  public LambdaExpression(List<String> variables, Deque<Token<?>> tokenDeque, StellarCompiler.ExpressionState state) {
    super(tokenDeque);
    this.state = state;
    this.variables = variables;
  }

  @Override
  public Deque<Token<?>> getTokenDeque() {
    Deque<Token<?>> ret = new ArrayDeque<>(super.getTokenDeque().size());
    for(Token<?> token : super.getTokenDeque()) {
      ret.add(token);
    }
    return ret;
  }

  public Object apply(List<Object> variableArgs) {
    Map<String, Object> lambdaVariables = new HashMap<>();
    int i = 0;
    for(;i < Math.min(variables.size(),variableArgs.size()) ;++i) {
      lambdaVariables.put(variables.get(i), variableArgs.get(i));
    }
    for(;i < variables.size();++i) {
      lambdaVariables.put(variables.get(i), null);
    }

    VariableResolver variableResolver = new DefaultVariableResolver(variable -> lambdaVariables.getOrDefault(variable
                                                                                , state.variableResolver.resolve(variable)
                                                                                ), variable -> true);
    StellarCompiler.ExpressionState localState = new StellarCompiler.ExpressionState(
            state.context
          , state.functionResolver
          , variableResolver);
    return apply(localState);
  }
}
