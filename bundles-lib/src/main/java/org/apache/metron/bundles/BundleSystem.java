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

import com.google.common.annotations.VisibleForTesting;
import java.net.URISyntaxException;
import java.util.Set;
import org.apache.commons.vfs2.FileSystemException;

/**
 * High level interface to the Bundle System.  While you may want to use the lower level classes it
 * is not required, as BundleSystem provides the base required interface for initializing the system
 * and instantiating classes
 */
public interface BundleSystem {

  /**
   * Resets the static classes which hold
   * singleton instances and contexts of loaded {@link org.apache.metron.bundles.bundle.Bundle}
   * and {@link ClassLoader}
   */
  @VisibleForTesting()
  static void reset() {
    synchronized (BundleSystem.class) {
      BundleClassLoaders.reset();
      ExtensionManager.reset();
    }
  }

  /**
   * Constructs an instance of the given type using either default no args constructor or a
   * constructor which takes a BundleProperties object.
   *
   * @param specificClassName the implementation class name
   * @param clazz the type (T) to create an instance for
   * @return an instance of specificClassName which extends T
   * @throws ClassNotFoundException if the class cannot be found
   * @throws InstantiationException if the class cannot be instantiated
   */
  <T> T createInstance(String specificClassName, Class<T> clazz)
      throws ClassNotFoundException, InstantiationException,
      NotInitializedException, IllegalAccessException;

  @SuppressWarnings("unchecked")
  /**
   * Returns the available classes registered in the system for a given extension types.
   * For example, all the {@link MessageParser} implementations.
   */
  <T> Set<Class<? extends T>> getExtensionsClassesForExtensionType(Class<T> extensionType)
      throws NotInitializedException;

  /**
   * Loads a Bundle into the system.
   *
   * @param bundleFileName the name of a Bundle file to load into the system. This file must exist
   *     in one of the library directories
   */
  void addBundle(String bundleFileName)
      throws NotInitializedException, ClassNotFoundException, FileSystemException,
      URISyntaxException;
}
