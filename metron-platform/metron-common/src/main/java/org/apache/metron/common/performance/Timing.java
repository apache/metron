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

import java.util.HashMap;
import java.util.Map;

public class Timing {

  Map<String, Long> startTimes;

  public Timing() {
    this.startTimes = new HashMap<>();
  }

  /**
   * Stores a starting time from current nanoTime with the provided name.
   *
   * @param name starting time mark name
   */
  public void mark(String name) {
    startTimes.put(name, System.nanoTime());
  }

  /**
   * Returns elapsed nanoTime given a stored marked time for the given name. Returns 0 for
   * non-existent mark names.
   *
   * @param name mark name to get elapsed time for.
   */
  public long getElapsed(String name) {
    if (startTimes.containsKey(name)) {
      return System.nanoTime() - startTimes.get(name);
    } else {
      return 0;
    }
  }

  /**
   * Checks existence of timing marker.
   *
   * @param name mark name to lookup.
   * @return true if mark has been called with this name, false otherwise.
   */
  public boolean exists(String name) {
    return startTimes.containsKey(name);
  }

}
