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

package org.apache.metron.stellar.common;


import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.dsl.VariableResolver;

import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * The Stellar Predicate Processor is intended to allow for specific predicate transformations using the Stellar
 * domain specific language.  In contrast to the StellarProcessor, which is a general purpose transformation
 * tool, the output of the stellar statement is always a boolean.  In java parlance, this is like a
 * java.util.function.Predicate
 */

public class StellarPredicateProcessor extends BaseStellarProcessor<Boolean> {

  /**
   * Create a default stellar processor.  This processor uses the static expression cache.
   */
  public StellarPredicateProcessor() {
    super(Boolean.class);
  }

  public StellarPredicateProcessor(int cacheSize, int expiryTime, TimeUnit expiryUnit) {
    super(Boolean.class, cacheSize, expiryTime, expiryUnit);
  }
  @Override
  public Boolean parse( String rule
                      , VariableResolver variableResolver
                      , FunctionResolver functionResolver
                      , Context context
                      )
  {
    if(rule == null || isEmpty(rule.trim())) {
      return true;
    }
    try {
      return super.parse(rule, variableResolver, functionResolver, context);
    } catch (ClassCastException e) {
      // predicate must return boolean
      throw new IllegalArgumentException(String.format("The rule '%s' does not return a boolean value.", rule), e);
    }
    catch(Exception e) {
      if(e.getCause() != null && e.getCause() instanceof ClassCastException) {
        throw new IllegalArgumentException(String.format("The rule '%s' does not return a boolean value.", rule), e.getCause());
      }
      throw e;
    }
  }
}
