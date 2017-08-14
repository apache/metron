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

import org.apache.metron.bundles.bundle.BundleCoordinates;

import java.util.*;
import java.util.function.BiFunction;

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

  private final BiFunction<Set<BundleCoordinates>, Set<BundleCoordinates>, Set<BundleCoordinates>> merger = (oldValue, newValue) -> {
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

  public Map<String, Set<BundleCoordinates>> getExtensionNames(String extensionName) {
    if (extensionNameMap.containsKey(extensionName)) {
      return Collections.unmodifiableMap(extensionNameMap.get(extensionName));
    } else {
      return new HashMap<>();
    }
  }

  public Map<String, Map<String, Set<BundleCoordinates>>> getAllExtensions() {
    return Collections.unmodifiableMap(extensionNameMap);
  }

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


  public int size() {
    int size = 0;

    for (final Map<String, Set<BundleCoordinates>> bundleSets : extensionNameMap.values()) {
      for (final Set<BundleCoordinates> coordinates : bundleSets.values()) {
        size += coordinates.size();
      }
    }
    return size;
  }


  public boolean isEmpty() {
    return extensionNameMap.isEmpty();
  }
}
