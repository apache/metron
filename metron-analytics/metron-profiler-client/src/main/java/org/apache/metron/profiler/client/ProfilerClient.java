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

package org.apache.metron.profiler.client;

import org.apache.metron.profiler.ProfilePeriod;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * An interface for a client capable of retrieving the profile data that has been persisted by the Profiler.
 */
public interface ProfilerClient {

  /**
   * Fetch the measurement values associated with a profile.
   *
   * @param clazz       The type of values stored by the profile.
   * @param profile     The name of the profile.
   * @param entity      The name of the entity.
   * @param groups      The groups used to sort the profile data.
   * @param durationAgo How far in the past to fetch values from.
   * @param unit        The time unit of 'durationAgo'.
   * @param defaultValue The default value to specify.  If empty, the result will be sparse.
   * @param <T>         The type of values stored by the Profile.
   * @return A list of values.
   */
  <T> List<T> fetch(Class<T> clazz, String profile, String entity, List<Object> groups, long durationAgo, TimeUnit unit, Optional<T> defaultValue);

  /**
   * Fetch the values stored in a profile based on a start and end timestamp.
   *
   * @param clazz   The type of values stored by the profile.
   * @param profile The name of the profile.
   * @param entity  The name of the entity.
   * @param groups  The groups used to sort the profile data.
   * @param start   The start time in epoch milliseconds.
   * @param end     The end time in epoch milliseconds.
   * @param defaultValue The default value to specify.  If empty, the result will be sparse.
   * @param <T>     The type of values stored by the profile.
   * @return A list of values.
   */
  <T> List<T> fetch(Class<T> clazz, String profile, String entity, List<Object> groups, long start, long end, Optional<T> defaultValue);

  /**
   * Fetch the values stored in a profile based on a set of period keys.
   *
   * @param clazz   The type of values stored by the profile.
   * @param profile The name of the profile.
   * @param entity  The name of the entity.
   * @param groups  The groups used to sort the profile data.
   * @param periods The set of profile period keys
   * @param defaultValue The default value to specify.  If empty, the result will be sparse.
   * @param <T>     The type of values stored by the profile.
   * @return A list of values.
   */
  <T> List<T> fetch(Class<T> clazz, String profile, String entity, List<Object> groups, Iterable<ProfilePeriod> periods, Optional<T> defaultValue);
}
