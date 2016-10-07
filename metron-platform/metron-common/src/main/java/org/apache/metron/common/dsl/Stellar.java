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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation used to define a Stellar function.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Stellar {

  /**
   * The namespace of the function.
   *
   * A namespace is optional.  If a namespace is defined, then function resolution requires
   * the namespace to be prepended to the function name as in 'NAMESPACE_FUNCTION'.
   */
  String namespace() default "";

  /**
   * The name of the function.
   *
   * A function name is required.
   */
  String name();

  /**
   * A description of the function.
   *
   * A description is optional.
   */
  String description() default "";

  /**
   * A description of what the function returns.
   *
   * A description is optional.
   */
  String returns() default "";

  /**
   * A description of the parameters required by the function.
   *
   * A description of the parameters is optional.
   */
  String[] params() default {};

  /**
   * The type of each required parameter.
   *
   * Allows functions to easily enforce required parameters.  If no required parameters are defined, then no
   * enforcement can occur.  The number of required parameters can be less than the total number of parameters,
   * which would indicate the remainder are optional parameters.
   *
   * The required parameters must precede the optional parameters.  The order of required parameters must match
   * the order as defined in the parameter description field; `params`.
   */
  Class[] requiredParams() default {};
}
