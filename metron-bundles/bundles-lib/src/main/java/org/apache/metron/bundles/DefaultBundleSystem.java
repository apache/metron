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

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileSystemManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High level interface to the Bundle System.  While you may want to use the lower level classes it
 * is not required, as BundleSystem provides the base required interface for initializing the system
 * and instantiating classes.
 */
public class DefaultBundleSystem implements BundleSystem {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final BundleProperties properties;
  private final FileSystemManager fileSystemManager;
  private final List<Class> extensionClasses;
  private final List<FileObject> extensionDirectories;
  private final Bundle systemBundle;

  protected DefaultBundleSystem(FileSystemManager fileSystemManager, List<Class> extensionClasses,
      List<FileObject> extensionDirectories, Bundle systemBundle, BundleProperties properties) {
    this.properties = properties;
    this.fileSystemManager = fileSystemManager;
    this.extensionClasses = extensionClasses;
    this.systemBundle = systemBundle;
    this.extensionDirectories = extensionDirectories;
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
  @Override
  public <T> T createInstance(final String specificClassName, final Class<T> clazz)
      throws ClassNotFoundException, InstantiationException, NotInitializedException,
      IllegalAccessException {
    synchronized (DefaultBundleSystem.class) {
      return BundleThreadContextClassLoader
          .createInstance(specificClassName, clazz, this.properties);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Set<Class<? extends T>> getExtensionsClassesForExtensionType(
      final Class<T> extensionType) throws NotInitializedException {
    Set<Class<? extends T>> set = new HashSet<Class<? extends T>>();
    synchronized (DefaultBundleSystem.class) {
      ExtensionManager.getInstance().getExtensions(extensionType).forEach((x) -> {
        set.add((Class<T>) x);
      });
    }
    return set;
  }

  /**
   * Loads a Bundle into the system.
   *
   * @param bundleFileName the name of a Bundle file to load into the system. This file must exist
   *     in one of the library directories
   */
  @Override
  public void addBundle(String bundleFileName)
      throws NotInitializedException, ClassNotFoundException,
      FileSystemException, URISyntaxException {
    if (StringUtils.isEmpty(bundleFileName)) {
      throw new IllegalArgumentException("bundleFileName cannot be null or empty");
    }
    synchronized (DefaultBundleSystem.class) {
      LOG.debug("Adding bundle " + bundleFileName + " to BundleClassLoaders");
      Bundle bundle = BundleClassLoaders.getInstance().addBundle(bundleFileName);
      LOG.debug("Adding bundle " + bundle.getBundleDetails().getBundleFile().getName().toString()
          + " to ExtensionManager");
      ExtensionManager.getInstance().addBundle(bundle);
    }
  }

}
