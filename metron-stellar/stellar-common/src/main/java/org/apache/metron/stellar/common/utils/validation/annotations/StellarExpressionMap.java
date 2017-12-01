/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.stellar.common.utils.validation.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface StellarExpressionMap {

  /**
   * The Name to give to the map
   *
   * @return String of the name
   */
  String name() default "default";

  /**
   * A map may be a StellarExpressionMap based on the type
   * of another field, this is that field's name
   *
   * @return Field Name or empty String
   */
  String qualify_with_field() default "";

  /**
   * A map may be a StellarExpressionMap based on the type
   * of another field, this is that type
   *
   * @return Class
   */
  Class qualify_with_field_type() default Error.class;


  /**
   * Some maps, may 'hold' the real map,
   * These are the key to get that map.
   * There are multiple in case there are multiple nested level
   * This does not support multiple nested 'peer' maps
   *
   * @return String key
   */
  String[] inner_map_keys() default {};
}