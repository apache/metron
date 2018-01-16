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
package org.apache.metron.statistics.informationtheory;

import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

import java.util.List;
import java.util.Map;

public class InformationTheoryFunctions {
  @Stellar( namespace="IT"
          , name="ENTROPY"
          , description = "Computes the base-2 entropy of a multiset"
          , params = { "input - a multiset (a map of objects to counts)" }
          , returns = "The [base-2 entropy](https://en.wikipedia.org/wiki/Entropy_(information_theory)#Definition) of the count .  The unit of this is bits."
  )
  public static class Entropy extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if(args.isEmpty()) {
        throw new IllegalArgumentException("IT_ENTROPY expects exactly one argument.");
      }
      Object inputObj = args.get(0);
      if(inputObj == null) {
        return null;
      }
      if(!(inputObj instanceof Map)) {
        throw new IllegalArgumentException("IT_ENTROPY expects exactly one argument and expects it to be a map of counts (e.g. Map<?, Integer>)");
      }
      Map<?, Integer> countMap = (Map<?, Integer>) inputObj;
      return InformationTheoryUtil.INSTANCE.bitEntropy(countMap);
    }
  }
}
