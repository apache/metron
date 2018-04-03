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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code StellarExpressionMap} is applied to
 * a {@code Map} which contains Stellar expressions as the values, such
 * that calling .toString() on a value of this map yields a Stellar expression.
 *
 * The key is used as the name the expression.
 *
 * It is possible for a {@code Map} to contain other maps or complex objects,
 * thus this annotation contains properties that give information on evaluation of the {@code Map}
 * should it be complex and contain nested maps.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.TYPE})
public @interface StellarExpressionMap {

  /**
   * The Name to give to the map
   *
   * @return String of the name
   */
  String name() default "default";

  /**
   * A map may be a StellarExpressionMap based on the type
   * of another field, this is that field's name.
   *
   * {@code} qualifyWithFieldType} is the type of that field
   *
   * @return Field Name or empty String
   */
  String qualifyWithField() default "";

  /**
   * A map may be a StellarExpressionMap based on the type
   * of another field, this is that type of that field.
   *
   * {@code qualifyWithField} is the name of the field.
   *
   * @return Class
   */
  Class qualifyWithFieldType() default Error.class;


  /**
   * Some maps, may 'hold' the real map as a value.  In that case the outer map should still be
   * annotated, and this property used to define the 'key route' needed to obtain the map.
   * These are the key to get that map.
   * There are multiple in case there are multiple nested level
   * This does not support multiple nested 'peer' maps
   *
   * @return String key
   */
  String[] innerMapKeys() default {};

}