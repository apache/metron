/*
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

package org.apache.metron.profiler.storm;

/**
 * Signals when it is time to flush a profile.
 */
public interface FlushSignal {

  /**
   * Returns true, if it is time to flush.
   *
   * @return True if time to flush.  Otherwise, false.
   */
  boolean isTimeToFlush();

  /**
   * Update the signaller with a known timestamp.
   *
   * @param timestamp A timestamp expected to be epoch milliseconds
   */
  void update(long timestamp);

  /**
   * Reset the signaller.
   */
  void reset();

  /**
   * Returns the current time in epoch milliseconds.
   * @return The current time in epoch milliseconds.
   */
  long currentTimeMillis();
}
