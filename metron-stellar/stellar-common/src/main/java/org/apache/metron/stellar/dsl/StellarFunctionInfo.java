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
package org.apache.metron.stellar.dsl;

import java.util.Arrays;

/**
 * Describes a Stellar function.
 */
public class StellarFunctionInfo {

  /**
   * The name of the function.
   */
  String name;

  /**
   * A description of the function.  Used for documentation purposes.
   */
  String description;

  /**
   * A description of what the function returns.  Used for documentation purposes.
   */
  String returns;

  /**
   * The function parameters.  Used for documentation purposes.
   */
  String[] params;

  /**
   * The actual function that can be executed.
   */
  StellarFunction function;

  public StellarFunctionInfo(String description, String name, String[] params, String returns, StellarFunction function) {
    this.description = description;
    this.name = name;
    this.params = params;
    this.function = function;
    this.returns = returns;
  }

  public String getReturns() {
    return returns;
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }

  public String[] getParams() {
    return params;
  }

  public StellarFunction getFunction() {
    return function;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StellarFunctionInfo that = (StellarFunctionInfo) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (description != null ? !description.equals(that.description) : that.description != null) return false;
    if (returns != null ? !returns.equals(that.returns) : that.returns != null) return false;
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    if (!Arrays.equals(params, that.params)) return false;
    return function != null ? function.equals(that.function) : that.function == null;

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (returns != null ? returns.hashCode() : 0);
    result = 31 * result + Arrays.hashCode(params);
    result = 31 * result + (function != null ? function.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "StellarFunctionInfo{" +
            "name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", returns='" + returns + '\'' +
            ", params=" + Arrays.toString(params) +
            ", function=" + function +
            '}';
  }
}
