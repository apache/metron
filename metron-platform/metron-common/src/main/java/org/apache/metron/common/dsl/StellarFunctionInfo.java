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
package org.apache.metron.common.dsl;

import java.util.Arrays;
import java.util.List;

public class StellarFunctionInfo {
  String description;
  String name;
  String[] params;
  StellarFunction function;
  String returns;
  public StellarFunctionInfo(String description, String name, String[] params, String returns, StellarFunction function) {
    this.description = description;
    this.name = name;
    this.params = params;
    this.function = function;
    this.returns = returns;
  }

  public String getReturns() { return returns;}

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

    if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null)
      return false;
    if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    if (!Arrays.equals(getParams(), that.getParams())) return false;
    return getReturns() != null ? getReturns().equals(that.getReturns()) : that.getReturns() == null;

  }

  @Override
  public int hashCode() {
    int result = getDescription() != null ? getDescription().hashCode() : 0;
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + Arrays.hashCode(getParams());
    result = 31 * result + (getReturns() != null ? getReturns().hashCode() : 0);
    return result;
  }
}
