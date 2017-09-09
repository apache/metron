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
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.*;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton class used to initialize the extension and framework classloaders.
 */
public final class BundleClassLoaders {

  private static volatile BundleClassLoaders bundleClassLoaders;
  private static volatile BundleClassLoadersContext initContext;
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private BundleClassLoaders() {
  }

  /**
   * @return The singleton instance of the BundleClassLoaders
   * @throws NotInitializedException if BundleClassLoaders has not been init'd
   */
  public static BundleClassLoaders getInstance() throws NotInitializedException {
    BundleClassLoaders result = bundleClassLoaders;
    if (result == null) {
      throw new NotInitializedException("BundleClassLoaders not initialized");
    }
    return result;
  }

  /**
   * Uninitializes the BundleClassloaders.  After calling this <code>init</code> must be called
   * before the rest of the methods are called afterwards.
   * This is for TESTING ONLY at this time.  Reset does not unload or clear any loaded classloaders.
   */
  @VisibleForTesting
  public static void reset() {
    synchronized (BundleClassLoaders.class) {
      initContext = null;
      bundleClassLoaders = null;
    }
  }

  /**
   * Initializes and loads the BundleClassLoaders. This method must be called before the rest of the
   * methods to access the classloaders are called.  Multiple calls to this method will have no effect,
   * unless a different set of extension directories is passed, which will result in an <code>IllegaStateException</code>
   *
   * @param fileSystemManager the FileSystemManager
   * @param extensionsDirs where to find extension artifacts
   * @param props BundleProperties
   * @throws FileSystemException if any issue occurs while working with the bundle files.
   * @throws java.lang.ClassNotFoundException if unable to load class definition
   * @throws IllegalStateException when already initialized with a given set of extension directories
   * and extensionDirs does not match
   */
  public static void init(final FileSystemManager fileSystemManager, final List<FileObject> extensionsDirs,
      BundleProperties props)
      throws FileSystemException, ClassNotFoundException, URISyntaxException {
    if (extensionsDirs == null || fileSystemManager == null) {
      throw new NullPointerException("cannot have empty arguments");
    }
    synchronized (BundleClassLoaders.class) {
      if (bundleClassLoaders != null) {
        throw new IllegalStateException("BundleClassloader already exists");
      }
      BundleClassLoaders b = new BundleClassLoaders();
      BundleClassLoadersContext ic = b.load(fileSystemManager, extensionsDirs, props);
      initContext = ic;
      bundleClassLoaders = b;
    }
  }

  private BundleClassLoadersContext load(final FileSystemManager fileSystemManager,
      final List<FileObject> extensionsDirs, BundleProperties properties)
      throws FileSystemException, ClassNotFoundException, URISyntaxException {
    return new BundleClassLoadersContext.Builder().withFileSystemManager(fileSystemManager)
        .withExtensionDirs(extensionsDirs)
        .withBundleProperties(properties).build();
  }

  /**
   * @param extensionFile the bundle file
   * @return the bundle for the specified bundle file. Returns null when no bundle exists
   * @throws IllegalStateException if the bundles have not been loaded
   */
  public Bundle getBundle(final FileObject extensionFile) {
    if (initContext == null) {
      throw new IllegalStateException("Extensions class loaders have not been loaded.");
    }

    try {
      return initContext.getBundles().get(extensionFile.getURL().toURI().toString());
    } catch (URISyntaxException | FileSystemException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Unable to get extension classloader for bundle '{}'",
            extensionFile.getName().toString());
      }
      return null;
    }
  }

  /**
   * @return the extensions that have been loaded
   * @throws IllegalStateException if the extensions have not been loaded
   */
  public Set<Bundle> getBundles() {
    if (initContext == null) {
      throw new IllegalStateException("Bundles have not been loaded.");
    }
    return new LinkedHashSet<>(initContext.getBundles().values());
  }

  /**
   * Add a bundle to the BundleClassLoaders.
   * Post initialization with will load a bundle and merge
   * it's information into the context.
   *
   * This method has limited access, only package classes that
   * can ensure thread saftey and control should call.
   * @param bundleName the file name of the bundle.  This is the name not the path, and the file
   * should exist in the configured library directories
   * @return The {@link Bundle} that is created
   * @throws FileSystemException
   * @throws URISyntaxException
   * @throws ClassNotFoundException
   */
  protected Bundle addBundle(String bundleName)
      throws FileSystemException, URISyntaxException, ClassNotFoundException {
    if (initContext == null) {
      throw new IllegalStateException("Bundles have not been loaded.");
    }
      BundleClassLoadersContext newContext = new BundleClassLoadersContext.Builder()
          .withBundleProperties(initContext.getProperties())
          .withExtensionDirs(initContext.getExtensionDirs())
          .withFileSystemManager(initContext.getFileSystemManager()).build(bundleName);

      initContext.merge(newContext);
      return initContext.getBundles().values().stream().findFirst().get();
  }
}
