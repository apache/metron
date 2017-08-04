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
package org.apache.metron.stellar.common.utils.math;

import java.util.function.Function;

public class MathOperation {
  private int maxArgs;
  private int minArgs;
  private Function<Number[], Number> operation;
  public MathOperation(Function<Number[], Number> operation, int minArgs, int maxArgs) {
    this.operation = operation;
    this.maxArgs = maxArgs;
    this.minArgs = minArgs;
  }

  public int getMaxArgs() {
    return maxArgs;
  }

  public int getMinArgs() {
    return minArgs;
  }

  public Function<Number[], Number> getOperation() {
    return operation;
  }
}
