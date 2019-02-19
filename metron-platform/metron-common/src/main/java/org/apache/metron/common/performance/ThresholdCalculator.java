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

package org.apache.metron.common.performance;

public class ThresholdCalculator {

  /**
   * Returns true roughly the provided percent of the time, false 100%-'percent' of the time.
   *
   * @param percent Desired probability to return true
   * @return true if the percent probability is true for this call.
   */
  public boolean isPast(int percent) {
    double rd = Math.random();
    if (rd <= toDecimal(percent)) {
      return true;
    }
    return false;
  }

  private double toDecimal(int percent) {
    return percent / 100.0;
  }

}
