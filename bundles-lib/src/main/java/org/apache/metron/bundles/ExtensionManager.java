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
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.bundle.BundleCoordinates;
import org.apache.metron.bundles.bundle.BundleDetails;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.DummyFileObject;
import org.apache.metron.bundles.util.FileUtils;
import org.apache.metron.bundles.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Singleton class for scanning through the classpath to load all extension components using
 * the ClassIndex and running through all classloaders (root, BUNDLEs).
 *
 *
 *
 */
@SuppressWarnings("rawtypes")
public class ExtensionManager {

  private static volatile ExtensionManager extensionManager;
  private static volatile ExtensionManagerContext initContext;

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final BundleCoordinates SYSTEM_BUNDLE_COORDINATE = new BundleCoordinates(
      BundleCoordinates.DEFAULT_GROUP, "system", BundleCoordinates.DEFAULT_VERSION);

  private ExtensionManager(){}

  /**
   * @return The singleton instance of the ExtensionManager
   */
  public static ExtensionManager getInstance() throws NotInitializedException {
    ExtensionManager result = extensionManager;
    if (result == null) {
      throw new NotInitializedException("ExtensionManager not initialized");
    }
    return result;
  }

  /**
   * Uninitializes the ExtensionManager.
   * TESTING ONLY
   */
  @VisibleForTesting
  public static void reset() {
    synchronized (ExtensionManager.class) {
      initContext = null;
      extensionManager = null;
    }
  }

  /**
   * Loads all extension class types that can be found on the bootstrap classloader and by creating
   * classloaders for all BUNDLES found within the classpath.
   *
   * @param bundles the bundles to scan through in search of extensions
   */
  public static void init(final List<Class> classes, final Bundle systemBundle, final Set<Bundle> bundles)
      throws NotInitializedException {

    if (systemBundle == null) {
      throw new IllegalArgumentException("systemBundle is required");
    }

    synchronized (ExtensionManager.class) {
      if (extensionManager != null) {
        throw new IllegalStateException("ExtensionManager already exists");
      }
      ExtensionManager em = new ExtensionManager();
      ExtensionManagerContext ic = new ExtensionManagerContext.Builder()
          .withClasses(classes)
          .withSystemBundle(systemBundle)
          .withBundles(bundles).build();
      initContext = ic;
      extensionManager = em;
    }
  }


  /**
   * Returns a bundle representing the system class loader.
   *
   * @param bundleProperties a BundleProperties instance which will be used to obtain the default
   * Bundle library path, which will become the working directory of the returned bundle
   * @return a bundle for the system class loader
   */
  public static Bundle createSystemBundle(final FileSystemManager fileSystemManager,
      final BundleProperties bundleProperties) throws FileSystemException, URISyntaxException {
    final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

    final String bundlesLibraryDirectory = bundleProperties
        .getProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY);
    if (StringUtils.isBlank(bundlesLibraryDirectory)) {
      throw new IllegalStateException(
          "Unable to create system bundle because " + BundleProperties.BUNDLE_LIBRARY_DIRECTORY
              + " was null or empty");
    }
    final URI bundlesLibraryDirURI = bundleProperties.getBundleLibraryDirectory();
    FileObject bundleDir = fileSystemManager.resolveFile(bundlesLibraryDirURI);

    // Test if the source Bundles can be read
    FileUtils.ensureDirectoryExistAndCanRead(bundleDir);

    // the system bundle file object is never accessed, we use a dummy to stand in
    final BundleDetails systemBundleDetails = new BundleDetails.Builder()
        .withBundleFile(new DummyFileObject())
        .withCoordinates(SYSTEM_BUNDLE_COORDINATE)
        .build();

    return new Bundle(systemBundleDetails, systemClassLoader);
  }

  /**
   * Determines the effective ClassLoader for the instance of the given type.
   *
   * @param classType the type of class to lookup the ClassLoader for
   * @param instanceIdentifier the identifier of the specific instance of the classType to look up
   * the ClassLoader for
   * @param bundle the bundle where the classType exists
   * @return the ClassLoader for the given instance of the given type, or null if the type is not a
   * detected extension type
   */
  public ClassLoader createInstanceClassLoader(final String classType,
      final String instanceIdentifier, final Bundle bundle) throws NotInitializedException{
    if (StringUtils.isEmpty(classType)) {
      throw new IllegalArgumentException("Class-Type is required");
    }

    if (StringUtils.isEmpty(instanceIdentifier)) {
      throw new IllegalArgumentException("Instance Identifier is required");
    }

    if (bundle == null) {
      throw new IllegalArgumentException("Bundle is required");
    }

    checkInitialized();

    final ClassLoader bundleClassLoader = bundle.getClassLoader();

    // If the class is annotated with @RequiresInstanceClassLoading and the registered ClassLoader is a URLClassLoader
    // then make a new InstanceClassLoader that is a full copy of the BUNDLE Class Loader, otherwise create an empty
    // InstanceClassLoader that has the Bundle ClassLoader as a parent
    ClassLoader instanceClassLoader;
    if (initContext.getRequiresInstanceClassLoading().contains(classType)
        && (bundleClassLoader instanceof URLClassLoader)) {
      final URLClassLoader registeredUrlClassLoader = (URLClassLoader) bundleClassLoader;
      instanceClassLoader = new InstanceClassLoader(instanceIdentifier, classType,
          registeredUrlClassLoader.getURLs(), registeredUrlClassLoader.getParent());
    } else {
      instanceClassLoader = new InstanceClassLoader(instanceIdentifier, classType, new URL[0],
          bundleClassLoader);
    }

    initContext.getInstanceClassloaderLookup().put(instanceIdentifier, instanceClassLoader);
    return instanceClassLoader;
  }

  /**
   * Retrieves the InstanceClassLoader for the component with the given identifier.
   *
   * @param instanceIdentifier the identifier of a component
   * @return the instance class loader for the component
   */
  public ClassLoader getInstanceClassLoader(final String instanceIdentifier)
      throws NotInitializedException {
    checkInitialized();
    return initContext.getInstanceClassloaderLookup().get(instanceIdentifier);
  }

  /**
   * Retrieves the Set of Classes registered with the ExtensionManager
   * @return Set of Class
   * @throws NotInitializedException
   */
  public Set<Class> getExtensionClasses() throws NotInitializedException {
    checkInitialized();
    return ImmutableSet.copyOf(initContext.getDefinitionMap().keySet());
  }

  /**
   * Removes the ClassLoader for the given instance and closes it if necessary.
   *
   * @param instanceIdentifier the identifier of a component to remove the ClassLoader for
   * @return the removed ClassLoader for the given instance, or null if not found
   */
  public ClassLoader removeInstanceClassLoaderIfExists(final String instanceIdentifier)
      throws NotInitializedException {
    if (instanceIdentifier == null) {
      return null;
    }
    checkInitialized();
    final ClassLoader classLoader = initContext.getInstanceClassloaderLookup().remove(instanceIdentifier);
    if (classLoader != null && (classLoader instanceof URLClassLoader)) {
      final URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
      try {
        urlClassLoader.close();
      } catch (IOException e) {
        logger.warn("Unable to class URLClassLoader for " + instanceIdentifier);
      }
    }
    return classLoader;
  }

  /**
   * Checks if the given class type requires per-instance class loading (i.e. contains the
   * @RequiresInstanceClassLoading annotation)
   *
   * @param classType the class to check
   * @return true if the class is found in the set of classes requiring instance level class
   * loading, false otherwise
   */
  public boolean requiresInstanceClassLoading(final String classType) throws NotInitializedException {
    if (classType == null) {
      throw new IllegalArgumentException("Class type cannot be null");
    }
    checkInitialized();
    return initContext.getRequiresInstanceClassLoading().contains(classType);
  }

  /**
   * Retrieves the bundles that have a class with the given name.
   *
   * @param classType the class name of an extension
   * @return the list of bundles that contain an extension with the given class name
   */
  public List<Bundle> getBundles(final String classType) throws NotInitializedException{
    if (classType == null) {
      throw new IllegalArgumentException("Class type cannot be null");
    }
    checkInitialized();
    final List<Bundle> bundles = initContext.getClassNameBundleLookup().get(classType);
    return bundles == null ? Collections.emptyList() : new ArrayList<>(bundles);
  }

  /**
   * Retrieves the bundle with the given withCoordinates.
   *
   * @param bundleCoordinates a withCoordinates to look up
   * @return the bundle with the given withCoordinates, or null if none exists
   */
  public Bundle getBundle(final BundleCoordinates bundleCoordinates) throws NotInitializedException {
    if (bundleCoordinates == null) {
      throw new IllegalArgumentException("BundleCoordinates cannot be null");
    }
    checkInitialized();
    return initContext.getBundleCoordinateBundleLookup().get(bundleCoordinates);
  }

  /**
   * Retrieves the bundle for the given class loader.
   *
   * @param classLoader the class loader to look up the bundle for
   * @return the bundle for the given class loader
   */
  public Bundle getBundle(final ClassLoader classLoader) throws NotInitializedException {
    if (classLoader == null) {
      throw new IllegalArgumentException("ClassLoader cannot be null");
    }
    checkInitialized();
    return initContext.getClassLoaderBundleLookup().get(classLoader);
  }

  public Set<Class> getExtensions(final Class<?> definition) throws NotInitializedException {
    if (definition == null) {
      throw new IllegalArgumentException("Class cannot be null");
    }
    checkInitialized();
    final Set<Class> extensions = initContext.getDefinitionMap().get(definition);
    return (extensions == null) ? Collections.<Class>emptySet() : extensions;
  }

  public void logClassLoaderMapping() throws NotInitializedException {
    checkInitialized();
    final StringBuilder builder = new StringBuilder();

    builder.append("Extension Type Mapping to Bundle:");
    for (final Map.Entry<Class, Set<Class>> entry : initContext.getDefinitionMap().entrySet()) {
      builder.append("\n\t=== ").append(entry.getKey().getSimpleName()).append(" Type ===");

      for (final Class type : entry.getValue()) {
        final List<Bundle> bundles = initContext.getClassNameBundleLookup().containsKey(type.getName())
            ? initContext.getClassNameBundleLookup().get(type.getName()) : Collections.emptyList();

        builder.append("\n\t").append(type.getName());

        for (final Bundle bundle : bundles) {
          final String coordinate = bundle.getBundleDetails().getCoordinates().getCoordinates();
          final String workingDir = bundle.getBundleDetails().getBundleFile().getName().toString();
          builder.append("\n\t\t").append(coordinate).append(" || ").append(workingDir);
        }
      }

      builder.append("\n\t=== End ").append(entry.getKey().getSimpleName()).append(" types ===");
    }

    logger.info(builder.toString());
  }

  /**
   * Add a new {@link Bundle} and it's extensions to the system
   * This is an operation that would happen after initialization.
    * This method has limited access, only package classes that
   * can ensure thread saftey and control should call.
   *
   *
   * @param bundle the {@link Bundle} to load
   * @throws NotInitializedException If we are not initialized yet
   */
  protected void addBundle(Bundle bundle) throws NotInitializedException {
    checkInitialized();

    Set<Bundle> bundles = new HashSet<>();
    bundles.add(bundle);
    ExtensionManagerContext newContext = new ExtensionManagerContext.Builder().withBundles(bundles)
        .withClasses(new ArrayList<Class>(initContext.getDefinitionMap().keySet()))
        .withSystemBundle(initContext.getSystemBundle())
        .build();

      initContext.merge(newContext);
  }

  public void checkInitialized() throws NotInitializedException {
    ExtensionManagerContext ic = initContext;
    if (ic == null) {
      throw new NotInitializedException();
    }
  }

}
