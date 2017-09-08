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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.annotation.behavior.RequiresInstanceClassLoading;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.bundle.BundleCoordinates;
import org.apache.metron.bundles.util.ImmutableCollectionUtils;
import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context object for the {@link ExtensionManager}.
 */
public class ExtensionManagerContext {

  private static final Logger logger = LoggerFactory
      .getLogger(MethodHandles.lookup().lookupClass());
  /**
   * Builder class for ExtensionManagerContext
   */
  public static class Builder {

    List<Class> classes;
    Bundle systemBundle;
    Set<Bundle> bundles;

    /**
     * Provides the {@link Class} definitions that will specify what extensions
     * are to be loaded
     * @param classes
     * @return
     */
    public Builder withClasses(List<Class> classes) {
      this.classes = classes;
      return this;
    }

    /**
     * Provides the SystemBundle.
     * This bundle represents the system or main classloader
     * @param systemBundle
     * @return
     */
    public Builder withSystemBundle(Bundle systemBundle) {
      this.systemBundle = systemBundle;
      return this;
    }

    /**
     * Provides the Bundles used to load the extensions
     * @param bundles
     * @return
     */
    public Builder withBundles(Set<Bundle> bundles) {
      this.bundles = bundles;
      return this;
    }

    public Builder() {
    }

    /**
     *  * Builds a BundleClassLoaderContext.
     * When built the context will be loaded from the provided
     * explicitBundleToLoad, using the {@link FileSystemManager} and {@BundleProperties}.
     *
     * If the explicteBundleToLoad is null or empty, then the extensionDirs will be used.
     *
     * This method can be used as a means to build a context for a single bundle.
     *
     * An IllegalArgumentException will be thrown if any of the SystemBundle,
     * Classes, or Bundles parameters are missing or invalid
     * @return
     */
    public ExtensionManagerContext build() {
      if (systemBundle == null) {
        throw new IllegalArgumentException("systemBundle must be defined");
      }
      if (classes == null || classes.size() == 0) {
        throw new IllegalArgumentException("classes must be defined");
      }
      if (bundles == null) {
        throw new IllegalArgumentException("bundles must be defined");
      }

      // get the current context class loader
      ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();

      final Map<Class, Set<Class>> definitionMap = new HashMap<>();
      final Map<String, List<Bundle>> classNameBundleLookup = new HashMap<>();
      final Map<BundleCoordinates, Bundle> bundleCoordinateBundleLookup = new HashMap<>();
      final Map<ClassLoader, Bundle> classLoaderBundleLookup = new HashMap<>();
      final Set<String> requiresInstanceClassLoading = new HashSet<>();
      final Map<String, ClassLoader> instanceClassloaderLookup = new HashMap<>();

      for (Class c : classes) {
        definitionMap.put(c, new HashSet<>());
      }
      // load the system bundle first so that any extensions found in JARs directly in lib will be registered as
      // being from the system bundle and not from all the other Bundles
      loadExtensions(systemBundle, definitionMap, classNameBundleLookup,
          requiresInstanceClassLoading);
      bundleCoordinateBundleLookup
          .put(systemBundle.getBundleDetails().getCoordinates(), systemBundle);
      classLoaderBundleLookup.put(systemBundle.getClassLoader(), systemBundle);
      // consider each bundle class loader
      for (final Bundle bundle : bundles) {
        // Must set the context class loader to the bundle classloader itself
        // so that static initialization techniques that depend on the context class loader will work properly
        final ClassLoader bcl = bundle.getClassLoader();
        // store in the lookup
        classLoaderBundleLookup.put(bcl, bundle);

        Thread.currentThread().setContextClassLoader(bcl);
        loadExtensions(bundle, definitionMap, classNameBundleLookup, requiresInstanceClassLoading);

        // Create a look-up from withCoordinates to bundle
        bundleCoordinateBundleLookup.put(bundle.getBundleDetails().getCoordinates(), bundle);
      }

      // restore the current context class loader if appropriate
      if (currentContextClassLoader != null) {
        Thread.currentThread().setContextClassLoader(currentContextClassLoader);
      }
      return new ExtensionManagerContext(systemBundle, definitionMap, classNameBundleLookup,
          bundleCoordinateBundleLookup,
          classLoaderBundleLookup, requiresInstanceClassLoading, instanceClassloaderLookup);
    }

    /**
     * Loads extensions from the specified bundle.
     *
     * @param bundle from which to load extensions
     */
    @SuppressWarnings("unchecked")
    private static void loadExtensions(final Bundle bundle,
        Map<Class, Set<Class>> definitionMap,
        Map<String, List<Bundle>> classNameBundleLookup,
        Set<String> requiresInstanceClassLoading) {

      for (final Entry<Class, Set<Class>> entry : definitionMap.entrySet()) {
        // this is another extention point
        // what we care about here is getting the right classes from the classloader for the bundle
        // this *could* be as a 'service' itself with different implementations
        // The NAR system uses the ServiceLoader, but this chokes on abstract classes, because for some
        // reason it feels compelled to instantiate the class,
        // which there may be in the system.
        // Changed to ClassIndex
        Class clazz = entry.getKey();
        ClassLoader cl = bundle.getClassLoader();
        Iterable<Class<?>> it = ClassIndex.getSubclasses(clazz, cl);
        for (Class<?> c : it) {
          if (cl.equals(c.getClassLoader())) {
            // check for abstract
            if (!Modifier.isAbstract(c.getModifiers())) {
              registerServiceClass(c, classNameBundleLookup, requiresInstanceClassLoading, bundle,
                  entry.getValue());
            }
          }
        }
        it = ClassIndex.getAnnotated(clazz, cl);
        for (Class<?> c : it) {
          if (cl.equals(clazz.getClassLoader())) {
            // check for abstract
            if (!Modifier.isAbstract(c.getModifiers())) {
              registerServiceClass(c, classNameBundleLookup, requiresInstanceClassLoading, bundle,
                  entry.getValue());
            }
          }
        }
      }
    }

    /**
     * Registers extension for the specified type from the specified Bundle.
     *
     * @param type the extension type
     * @param classNameBundleMap mapping of classname to Bundle
     * @param bundle the Bundle being mapped to
     * @param classes to map to this classloader but which come from its ancestors
     */
    private static void registerServiceClass(final Class<?> type,
        final Map<String, List<Bundle>> classNameBundleMap,
        final Set<String> requiresInstanceClassLoading,
        final Bundle bundle,
        final Set<Class> classes) {
      final String className = type.getName();

      // get the bundles that have already been registered for the class name
      List<Bundle> registeredBundles = classNameBundleMap
          .computeIfAbsent(className, (x) -> new ArrayList<>());

      boolean alreadyRegistered = false;
      for (final Bundle registeredBundle : registeredBundles) {
        final BundleCoordinates registeredCoordinate = registeredBundle.getBundleDetails()
            .getCoordinates();

        // if the incoming bundle has the same withCoordinates as one of the registered bundles
        // then consider it already registered
        if (registeredCoordinate.equals(bundle.getBundleDetails().getCoordinates())) {
          alreadyRegistered = true;
          break;
        }

        // if the type wasn't loaded from an ancestor, and the type isn't a parsers, cs, or reporting task, then
        // fail registration because we don't support multiple versions of any other types
        if (!multipleVersionsAllowed(type)) {
          throw new IllegalStateException("Attempt was made to load " + className + " from "
              + bundle.getBundleDetails().getCoordinates().getCoordinates()
              + " but that class name is already loaded/registered from " + registeredBundle
              .getBundleDetails().getCoordinates()
              + " and multiple versions are not supported for this type"
          );
        }
      }

      // if none of the above was true then register the new bundle
      if (!alreadyRegistered) {
        registeredBundles.add(bundle);
        classes.add(type);

        if (type.isAnnotationPresent(RequiresInstanceClassLoading.class)) {
          requiresInstanceClassLoading.add(className);
        }
      }
    }

    /**
     * @param type a Class that we found from a service loader
     * @return true if the given class is a parsers, controller service, or reporting task
     */
    private static boolean multipleVersionsAllowed(Class<?> type) {
      // we don't really need to support multiple versions at this time
      return false;
    }
  }

  // Maps a service definition (interface) to those classes that implement the interface
  private Map<Class, Set<Class>> definitionMap;
  private Map<String, List<Bundle>> classNameBundleLookup;
  private Map<BundleCoordinates, Bundle> bundleCoordinateBundleLookup;
  private Map<ClassLoader, Bundle> classLoaderBundleLookup;
  private Set<String> requiresInstanceClassLoading;
  private Map<String, ClassLoader> instanceClassloaderLookup;
  private Bundle systemBundle;


  private ExtensionManagerContext(Bundle systemBundle, Map<Class, Set<Class>> definitionMap,
      Map<String, List<Bundle>> classNameBundleLookup,
      Map<BundleCoordinates, Bundle> bundleCoordinateBundleLookup,
      Map<ClassLoader, Bundle> classLoaderBundleLookup,
      Set<String> requiresInstanceClassLoading,
      Map<String, ClassLoader> instanceClassloaderLookup) {
    this.systemBundle = systemBundle;
    this.definitionMap = ImmutableCollectionUtils.immutableMapOfSets(definitionMap);
    this.classNameBundleLookup = ImmutableCollectionUtils
        .immutableMapOfLists(classNameBundleLookup);
    this.bundleCoordinateBundleLookup = ImmutableMap.copyOf(bundleCoordinateBundleLookup);
    this.classLoaderBundleLookup = ImmutableMap.copyOf(classLoaderBundleLookup);
    this.requiresInstanceClassLoading = ImmutableSet.copyOf(requiresInstanceClassLoading);
    this.instanceClassloaderLookup = new ConcurrentHashMap<>(instanceClassloaderLookup);
  }

  /**
   * Merges another ExtensionManagerContext into this one, creating a union of the two.
   * Responsibility for synchronization of access to this context is up to the holder of it's
   * reference
   *
   * @param other a ExtensionManagerContext instance to merge into this one
   */
  public void merge(ExtensionManagerContext other) {

    // merge everything together
    // not on key matches, we merge the collection values
    this.classNameBundleLookup = ImmutableCollectionUtils.immutableMapOfLists(
        Stream.of(this.classNameBundleLookup, other.classNameBundleLookup)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (entry1, entry2) -> {
              return Stream.concat(((List<Bundle>)entry1).stream(),((List<Bundle>)entry2).stream()).filter((x) ->!((List<Bundle>)entry1).contains(x))
              .collect(Collectors.toList());
            })));
    this.definitionMap = ImmutableCollectionUtils.immutableMapOfSets(
        Stream.of(this.definitionMap, other.definitionMap)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (entry1, entry2) -> {
              return Stream.concat(((Set<Class>)entry1).stream(),((Set<Class>)entry2).stream()).filter((x) -> !((Set<Class>)entry2).contains(x))
                  .collect(Collectors.toSet());
            })));

    this.bundleCoordinateBundleLookup = ImmutableMap.copyOf(Stream.of(bundleCoordinateBundleLookup, other.bundleCoordinateBundleLookup).map(Map::entrySet).flatMap(
        Collection::stream).collect(
        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (s, a) -> s)));

    this.classLoaderBundleLookup = ImmutableMap.copyOf(Stream.of(classLoaderBundleLookup, other.classLoaderBundleLookup).map(Map::entrySet).flatMap(
        Collection::stream).collect(
        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (s, a) -> s)));

    this.requiresInstanceClassLoading = ImmutableSet.copyOf(
        Stream.concat(requiresInstanceClassLoading.stream(),
            other.requiresInstanceClassLoading.stream().filter((x) -> !requiresInstanceClassLoading.contains(x))).collect(
            Collectors.toSet()));

    this.instanceClassloaderLookup = new ConcurrentHashMap<>(Stream.of(instanceClassloaderLookup, other.instanceClassloaderLookup).map(Map::entrySet).flatMap(
        Collection::stream).collect(
        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (s, a) -> s)));
  }


  public Map<Class, Set<Class>> getDefinitionMap() {
    return definitionMap;
  }

  public Map<String, List<Bundle>> getClassNameBundleLookup() {
    return classNameBundleLookup;
  }

  public Map<BundleCoordinates, Bundle> getBundleCoordinateBundleLookup() {
    return bundleCoordinateBundleLookup;
  }

  public Map<ClassLoader, Bundle> getClassLoaderBundleLookup() {
    return classLoaderBundleLookup;
  }

  public Set<String> getRequiresInstanceClassLoading() {
    return requiresInstanceClassLoading;
  }

  public Map<String, ClassLoader> getInstanceClassloaderLookup() {
    return instanceClassloaderLookup;
  }

  public Bundle getSystemBundle() {
    return systemBundle;
  }

}
