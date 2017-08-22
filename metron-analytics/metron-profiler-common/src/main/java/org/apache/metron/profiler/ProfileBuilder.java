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

package org.apache.metron.profiler;

import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.json.simple.JSONObject;

/**
 * Responsible for building and maintaining a Profile.
 *
 * One or more messages are applied to the Profile with `apply` and a profile measurement is
 * produced by calling `flush`.
 *
 * Any one instance is responsible only for building the profile for a specific [profile, entity]
 * pairing.  There will exist many instances, one for each [profile, entity] pair that exists
 * within the incoming telemetry data applied to the profile.
 */
public interface ProfileBuilder {

  /**
   * Apply a message to the profile.
   * @param message The message to apply.
   */
  void apply(JSONObject message);

  /**
   * Flush the Profile.
   *
   * Completes and emits the ProfileMeasurement.  Clears all state in preparation for
   * the next window period.
   *
   * @return Returns the completed profile measurement.
   */
  ProfileMeasurement flush();

  /**
   * Has the ProfileBuilder been initialized?
   * @return True, if initialization has occurred.  False, otherwise.
   */
  boolean isInitialized();

  /**
   * Returns the definition of the profile being built.
   * @return
   */
  ProfileConfig getDefinition();

  /**
   * Returns the value of a variable being maintained by the builder.
   * @param variable The variable name.
   * @return The value of the variable.
   */
  Object valueOf(String variable);
}
