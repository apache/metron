/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.bundles.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for creating immutable versions of collections, where the inner collection needs to
 * be immutable as well.
 *
 * {@see com.google.common.collect}
 */
public class ImmutableCollectionUtils {

  /**
   * Creates an {@link ImmutableMap} of the passed {@link Map}. This includes {@link ImmutableSet}
   * versions of the value {@link Set}
   * @param map The {@link Map} to make immutable
   * @param <T> The type of the key
   * @param <K> The type of the {@link Set} elements
   * @return An {@link ImmutableMap} of {@link ImmutableSet}
   */
  public static <T, K> Map<T, Set<K>> immutableMapOfSets(Map<T, Set<K>> map) {
    for (T key : map.keySet()) {
      map.put(key, ImmutableSet.copyOf(map.get(key)));
    }
    return ImmutableMap.copyOf(map);
  }

  /**
   * Creates an {@link ImmutableMap} of the passed {@link Map}.  This includes
   * {@link ImmutableList} instances of the value {@link List}
   * @param map The {@link Map} to make immutable
   * @param <T> The type of the key
   * @param <K> The type of the {@link List} elements
   * @return An {@link ImmutableMap} of {@link ImmutableList}
   */
  public static <T, K> Map<T, List<K>> immutableMapOfLists(Map<T, List<K>> map) {
    for (T key : map.keySet()) {
      map.put(key, ImmutableList.copyOf(map.get(key)));
    }
    return ImmutableMap.copyOf(map);
  }

}
