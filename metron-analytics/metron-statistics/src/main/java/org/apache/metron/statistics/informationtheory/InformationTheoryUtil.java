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

import java.util.Map;

public enum InformationTheoryUtil {
  INSTANCE;
  private static final double LOG2 = Math.log(2);

  public double entropy(Map<?, Integer> counts, double logOfBase) {
    double ret = 0.0;
    int n = 0;
    if(counts == null || counts.isEmpty()) {
      return ret;
    }
    for(Integer f : counts.values()) {
      n+=f;
    }

    for(Integer f : counts.values()) {
      double p = f.doubleValue()/n;
      ret -= p * Math.log(p) / logOfBase;
    }
    return ret;
  }

  public double entropy(Map<?, Integer> counts, int base) {
    return entropy(counts, Math.log(base));
  }

  public double bitEntropy(Map<?, Integer> counts) {
    return entropy(counts, LOG2);
  }
}
