/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.stellar.dsl;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Simple VariableResolver implemenation using passed Functions
 * for implementation.
 *
 * Support for updates is optional
 */
public class DefaultVariableResolver implements VariableResolver {

  private Function<String, Object> resolveFunc;
  private Function<String, Boolean> existsFunc;
  private BiConsumer<String, Object> updateFunc;

  /**
   * DefaultVariableResolver without support for updates
   * @param resolveFunc
   * @param existsFunc
   */
  public DefaultVariableResolver(Function<String, Object> resolveFunc,
      Function<String, Boolean> existsFunc) {
    this(resolveFunc, existsFunc, null);
  }

  /**
   * DefaultVariableResolver with full support for updates
   * @param resolveFunc
   * @param existsFunc
   * @param updateFunc
   */
  public DefaultVariableResolver(Function<String, Object> resolveFunc,
      Function<String, Boolean> existsFunc, BiConsumer<String, Object> updateFunc) {
    this.resolveFunc = resolveFunc;
    this.existsFunc = existsFunc;
    this.updateFunc = updateFunc;
  }

  @Override
  public Object resolve(String variable) {
    return resolveFunc == null? null : resolveFunc.apply(variable);
  }

  @Override
  public boolean exists(String variable) {
    return existsFunc == null? false : existsFunc.apply(variable);
  }

  @Override
  public void update(String variable, Object value) {
    if (updateFunc != null) {
      updateFunc.accept(variable, value);
    }
  }

  public static DefaultVariableResolver NULL_RESOLVER = new DefaultVariableResolver(x -> null,
      x -> false, null);
}
