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

import org.apache.commons.lang.StringUtils;

import java.util.List;

import static java.lang.String.format;

public abstract class BaseStellarFunction implements StellarFunction {

  public abstract Object apply(List<Object> args);

  /**
   * Validates that the parameters received by a function match the parameters
   * that have been declared as required.
   *
   * @param args The actual parameters.
   */
  public void validate(List<Object> args) {

    Stellar annotation = this.getClass().getDeclaredAnnotation(Stellar.class);
    if(annotation != null) {

      // validate the require parameters
      Class[] requiredParams = annotation.requiredParams();
      for(int i=0; i<requiredParams.length; i++) {

        // the type of required parameter
        Class required = requiredParams[i];

        // is a required parameter missing?
        if(args.size() <= i) {
          String msg = format("%s: missing parameter [%d]: expected %s", functionName(annotation), i+1, required);
          throw new IllegalArgumentException(msg);
        }

        // is the parameter of the expected type?  NULL is acceptable
        Object param = args.get(i);
        if(param != null && !required.isAssignableFrom(param.getClass())) {
          String msg = format("%s: unexpected parameter [%d]: expected %s, actual %s", functionName(annotation), i+1, required, param.getClass());
          throw new IllegalArgumentException(msg);
        }
      }
    }
  }

  /**
   * Returns the name of the function for a given Stellar annotation.
   *
   * @param annotation The Stellar annotation.
   * @return The function name.
   */
  private String functionName(Stellar annotation) {
    String functionName;

    if(StringUtils.isNotBlank(annotation.namespace())) {
      functionName = annotation.namespace() + "_" + annotation.name();
    } else {
      functionName = annotation.name();
    }

    return functionName;
  }

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    return apply(args);
  }

  @Override
  public void initialize(Context context) {

  }

  @Override
  public boolean isInitialized() {
    return true;
  }
}
