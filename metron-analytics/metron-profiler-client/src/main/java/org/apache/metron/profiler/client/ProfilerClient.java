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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An interface for a client capable of retrieving the profile data that has been persisted by the Profiler.
 */
public interface ProfilerClient {

  /**
   * Fetch the measurement values associated with a profile.
   *
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param durationAgo How far in the past to fetch values from.
   * @param unit The time unit of 'durationAgo'.
   * @param clazz The type of values stored by the profile.
   * @param groups The groups used to sort the profile data.
   * @param <T> The type of values stored by the Profile.
   * @return A list of values.
   */
  <T extends Number>
  List<T> fetch(String profile, String entity, long durationAgo, TimeUnit unit, Class<T> clazz, List<Object> groups);
}
