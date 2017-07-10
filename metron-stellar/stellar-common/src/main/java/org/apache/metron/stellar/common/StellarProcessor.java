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

import java.util.concurrent.TimeUnit;

/**
 * The Stellar Processor is intended to allow for general transformations using the Stellar
 * domain specific language.  In contrast to the StellarPredicateProcessor where
 * the output of the stellar statement is always a boolean, this is intended for use when you
 * need non-predicate transformation.  In java parlance, this is similar to a java.util.function.Function
 * rather than a java.util.function.Predicate
 */
public class StellarProcessor extends BaseStellarProcessor<Object> {

  /**
   * Create a default stellar processor.  This processor uses the static expression cache.
   */
  public StellarProcessor() {
    super(Object.class);
  }

  /**
   * Create a stellar processor with a new expression cache.  NOTE: This object should be reused to prevent
   * performance regressions.
   * @param cacheSize
   * @param expiryTime
   * @param expiryUnit
   */
  public StellarProcessor(int cacheSize, int expiryTime, TimeUnit expiryUnit) {
    super(Object.class, cacheSize, expiryTime, expiryUnit);
  }
}
