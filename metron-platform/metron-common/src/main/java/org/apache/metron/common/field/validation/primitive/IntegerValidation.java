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

package org.apache.metron.common.field.validation.primitive;

import org.apache.commons.validator.routines.LongValidator;
import org.apache.metron.stellar.dsl.Predicate2StellarFunction;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.common.field.validation.SimpleValidation;

import java.util.function.Predicate;

public class IntegerValidation extends SimpleValidation{
  @Stellar(name="IS_INTEGER"
          , description = "Determines whether or not an object is an integer."
          , params = {
              "x - The object to test"
                     }
          , returns = "True if the object can be converted to an integer and false if otherwise."
          )
  public static class IS_INTEGER extends Predicate2StellarFunction {

    public IS_INTEGER() {
      super(new IntegerValidation());
    }
  }
  @Override
  public Predicate<Object> getPredicate() {
    return x -> LongValidator.getInstance().isValid(x == null?null:x.toString());
  }
}
