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

package org.apache.metron.bundles;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import org.apache.metron.bundles.bundle.BundleCoordinates;

/**
 * The ExtensionMapping represents a mapping of the extensions available to the system. It is the
 * product of the BundleMapper.
 * It is NOT used at runtime for loading extensions, rather it may be used by a system to have
 * details about the Extensions that exist in a system
 * Runtime extension loading happens in the {@link ExtensionManager#init}
 */
public class ExtensionMapping {

  /*
    The extensionNameMap is a map of the following
    Extension Type -> Map of Extension Class Types to a set of BundleCoordinates

    For example :
    Parser -> MessageParser -> [ bundles with parsers]

    BundleProperties files define with Property names the type and class types, such as:

    bundle.extension.type.parser=org.apache.metron.parsers.interfaces.MessageParser

    This is done to give a namespace to extensions, while supporting future extension types
    and classes.  This is different from the inspirational Nar system, which defined an explicit set
    of supported classes, and a separate map for each.

   */
  private final Map<String, Map<String, Set<BundleCoordinates>>> extensionNameMap = new HashMap<>();

  private final BiFunction<Set<BundleCoordinates>, Set<BundleCoordinates>,
      Set<BundleCoordinates>> merger = (oldValue, newValue) -> {
        final Set<BundleCoordinates> merged = new HashSet<>();
        merged.addAll(oldValue);
        merged.addAll(newValue);
        return merged;
      };

  void addExtension(final String extensionName, final BundleCoordinates coordinate,
      final String type) {
    if (!extensionNameMap.containsKey(extensionName)) {
      Map<String, Set<BundleCoordinates>> bundles = new HashMap<>();
      bundles.put(type, new HashSet<>());
      extensionNameMap.put(extensionName, bundles);
    }
    extensionNameMap.get(extensionName).computeIfAbsent(type, name -> new HashSet<>())
        .add(coordinate);
  }

  void addAllExtensions(final String extensionName, final BundleCoordinates coordinate,
      final Collection<String> types) {
    if (!extensionNameMap.containsKey(extensionName)) {
      Map<String, Set<BundleCoordinates>> bundles = new HashMap<>();
      extensionNameMap.put(extensionName, bundles);
    }
    types.forEach(name -> {
      addExtension(extensionName, coordinate, name);
    });
  }

  /**
   * Returns a Map of the extension class types to a Set of BundleCoordinates for a given extension
   * type.
   *
   * @param extensionTypeName the extension type name, such as parser, stellar, indexing
   * @return Map of extension class name to a Set of BundleCoordinates
   */
  public Map<String, Set<BundleCoordinates>> getExtensionNames(String extensionTypeName) {
    if (extensionNameMap.containsKey(extensionTypeName)) {
      return Collections.unmodifiableMap(extensionNameMap.get(extensionTypeName));
    } else {
      return new HashMap<>();
    }
  }

  /**
   * Returns all the extensions in the system, mapped by extension type.
   *
   * @return Map of extension types to a map of extension class to BundleCoordinates
   */
  public Map<String, Map<String, Set<BundleCoordinates>>> getAllExtensions() {
    return Collections.unmodifiableMap(extensionNameMap);
  }

  /**
   * Returns a Map of extension class types to a Set of BundleCoordinates for the system. This
   * merges all the extension types into one map
   *
   * @return Map of extension class name to a Set of BundleCoordinates
   */
  public Map<String, Set<BundleCoordinates>> getAllExtensionNames() {
    final Map<String, Set<BundleCoordinates>> extensionNames = new HashMap<>();
    for (final Map<String, Set<BundleCoordinates>> bundleSets : extensionNameMap.values()) {
      extensionNames.putAll(bundleSets);
    }
    return extensionNames;
  }

  void merge(final ExtensionMapping other) {
    other.getAllExtensions().forEach((ex, set) -> {
      set.forEach((name, otherCoordinates) -> {
        if (!extensionNameMap.containsKey(ex)) {
          extensionNameMap.put(ex, new HashMap<>());
        }
        extensionNameMap.get(ex).merge(name, otherCoordinates, merger);
      });
    });
  }


  /**
   * Returns the number of all the bundles mapped to types. Bundles that map multiple types will be
   * counted multiple times, once for each extension class exposed.
   *
   * @return raw count of the number of bundles
   */
  public int size() {
    int size = 0;

    for (final Map<String, Set<BundleCoordinates>> bundleSets : extensionNameMap.values()) {
      for (final Set<BundleCoordinates> coordinates : bundleSets.values()) {
        size += coordinates.size();
      }
    }
    return size;
  }


  /**
   * @return true if there are no extension types in the system.
   */
  public boolean isEmpty() {
    return extensionNameMap.isEmpty();
  }
}
