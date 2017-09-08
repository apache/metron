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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.bundle.BundleCoordinates;
import org.apache.metron.bundles.bundle.BundleDetails;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.BundleSelector;
import org.apache.metron.bundles.util.BundleUtil;
import org.apache.metron.bundles.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Context object used by the BundleClassLoaders
 */
public class BundleClassLoadersContext {

  private static final Logger logger = LoggerFactory
      .getLogger(MethodHandles.lookup().lookupClass());

  public static class Builder {

    FileSystemManager fileSystemManager;
    List<FileObject> extensionsDirs;
    FileObject bundleFile;
    BundleProperties properties;

    public Builder() {
    }

    public Builder withFileSystemManager(FileSystemManager fileSystemManager) {
      this.fileSystemManager = fileSystemManager;
      return this;
    }

    public Builder withExtensionDirs(List<FileObject> extensionDirs) {
      this.extensionsDirs = extensionDirs;
      return this;
    }

    public Builder withBundleProperties(BundleProperties properties) {
      this.properties = properties;
      return this;
    }

    public BundleClassLoadersContext build()
        throws FileSystemException, ClassNotFoundException, URISyntaxException {
      return build(null);
    }

    public BundleClassLoadersContext build(String explicitBundleToLoad)
        throws FileSystemException, ClassNotFoundException, URISyntaxException {

      if(extensionsDirs == null || extensionsDirs.size() == 0) {
        throw new IllegalArgumentException("missing extensionDirs");
      }

      if(properties == null) {
        throw new IllegalArgumentException("properties are required");
      }

      if (fileSystemManager == null) {
        throw new IllegalArgumentException("fileSystemManager is required");
      }

      // get the system classloader
      final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

      // find all bundle files and create class loaders for them.
      final Map<String, Bundle> directoryBundleLookup = new LinkedHashMap<>();
      final Map<String, ClassLoader> coordinateClassLoaderLookup = new HashMap<>();
      final Map<String, Set<BundleCoordinates>> idBundleLookup = new HashMap<>();
      boolean foundExplicitLoadBundle = false;
      boolean explicitBundleIsNotFile = false;
      for (FileObject extensionsDir : extensionsDirs) {
        // make sure the bundle directory is there and accessible
        FileUtils.ensureDirectoryExistAndCanRead(extensionsDir);

        final List<FileObject> bundleDirContents = new ArrayList<>();
        FileObject[] dirFiles = null;

        // are we loading all bundles into this context or one explicit bundle?
        // if it is explicit, we need to flag finding it, since for explict loads
        // a bundle that doesn't exist or is not a file is an error
        if (explicitBundleToLoad == null) {
          dirFiles = extensionsDir
              .findFiles(new BundleSelector(properties.getArchiveExtension()));
        } else {
          FileObject explicitBundleFileObject = extensionsDir.resolveFile(explicitBundleToLoad);
          if (explicitBundleFileObject.exists()) {
            foundExplicitLoadBundle = true;
            if (!explicitBundleFileObject.isFile()) {
              explicitBundleIsNotFile = true;
            }
            dirFiles = new FileObject[]{explicitBundleFileObject};
          }
        }

        if (dirFiles != null) {
          List<FileObject> fileList = Arrays.asList(dirFiles);
          bundleDirContents.addAll(fileList);
        }

        if (!bundleDirContents.isEmpty()) {
          final List<BundleDetails> bundleDetails = new ArrayList<>();
          final Map<String, String> bundleCoordinatesToBundleFile = new HashMap<>();

          // load the bundle details which includes bundle dependencies
          for (final FileObject bundleFile : bundleDirContents) {
            if (!bundleFile.exists() || !bundleFile.isFile()) {
              continue;
            }
            BundleDetails bundleDetail = null;
            try {
              bundleDetail = getBundleDetails(bundleFile, properties);
            } catch (IllegalStateException e) {
              logger.warn("Unable to load BUNDLE {} due to {}, skipping...",
                  new Object[]{bundleFile.getURL(), e.getMessage()});
            }

            // prevent the application from starting when there are two BUNDLEs with same group, id, and version
            final String bundleCoordinate = bundleDetail.getCoordinates().getCoordinates();
            if (bundleCoordinatesToBundleFile.containsKey(bundleCoordinate)) {
              final String existingBundleWorkingDir = bundleCoordinatesToBundleFile
                  .get(bundleCoordinate);
              throw new IllegalStateException(
                  "Unable to load BUNDLE with coordinates " + bundleCoordinate
                      + " and bundle file " + bundleDetail.getBundleFile()
                      + " because another BUNDLE with the same coordinates already exists at "
                      + existingBundleWorkingDir);
            }

            bundleDetails.add(bundleDetail);
            bundleCoordinatesToBundleFile.put(bundleCoordinate,
                bundleDetail.getBundleFile().getURL().toURI().toString());
          }

          for (final Iterator<BundleDetails> bundleDetailsIter = bundleDetails.iterator();
              bundleDetailsIter.hasNext(); ) {
            final BundleDetails bundleDetail = bundleDetailsIter.next();
            // populate bundle lookup
            idBundleLookup.computeIfAbsent(bundleDetail.getCoordinates().getId(),
                id -> new HashSet<>()).add(bundleDetail.getCoordinates());
          }

          int bundleCount;
          do {
            // record the number of bundles to be loaded
            bundleCount = bundleDetails.size();

            // attempt to create each bundle class loader
            for (final Iterator<BundleDetails> bundleDetailsIter = bundleDetails.iterator();
                bundleDetailsIter.hasNext(); ) {
              final BundleDetails bundleDetail = bundleDetailsIter.next();
              final BundleCoordinates bundleDependencyCoordinate = bundleDetail
                  .getDependencyCoordinates();

              // see if this class loader is eligible for loading
              ClassLoader potentialBundleClassLoader = null;
              if (bundleDependencyCoordinate == null) {
                potentialBundleClassLoader = createBundleClassLoader(fileSystemManager,
                    bundleDetail.getBundleFile(), ClassLoader.getSystemClassLoader());
              } else {
                final String dependencyCoordinateStr = bundleDependencyCoordinate
                    .getCoordinates();

                // if the declared dependency has already been loaded
                if (coordinateClassLoaderLookup.containsKey(dependencyCoordinateStr)) {
                  final ClassLoader bundleDependencyClassLoader = coordinateClassLoaderLookup
                      .get(dependencyCoordinateStr);
                  potentialBundleClassLoader = createBundleClassLoader(
                      fileSystemManager, bundleDetail.getBundleFile(),
                      bundleDependencyClassLoader);
                } else {
                  // get all bundles that match the declared dependency id
                  final Set<BundleCoordinates> coordinates = idBundleLookup
                      .get(bundleDependencyCoordinate.getId());

                  // ensure there are known bundles that match the declared dependency id
                  if (coordinates != null && !coordinates
                      .contains(bundleDependencyCoordinate)) {
                    // ensure the declared dependency only has one possible bundle
                    if (coordinates.size() == 1) {
                      // get the bundle with the matching id
                      final BundleCoordinates coordinate = coordinates.stream()
                          .findFirst().get();

                      // if that bundle is loaded, use it
                      if (coordinateClassLoaderLookup
                          .containsKey(coordinate.getCoordinates())) {
                        logger.warn(String.format(
                            "While loading '%s' unable to locate exact BUNDLE dependency '%s'. Only found one possible match '%s'. Continuing...",
                            bundleDetail.getCoordinates().getCoordinates(),
                            dependencyCoordinateStr,
                            coordinate.getCoordinates()));

                        final ClassLoader bundleDependencyClassLoader = coordinateClassLoaderLookup
                            .get(coordinate.getCoordinates());
                        potentialBundleClassLoader = createBundleClassLoader(
                            fileSystemManager, bundleDetail.getBundleFile(),
                            bundleDependencyClassLoader);
                      }
                    }
                  }
                }
              }

              // if we were able to create the bundle class loader, store it and remove the details
              final ClassLoader bundleClassLoader = potentialBundleClassLoader;
              if (bundleClassLoader != null) {
                directoryBundleLookup
                    .put(bundleDetail.getBundleFile().getURL().toURI().toString(),
                        new Bundle(bundleDetail, bundleClassLoader));
                coordinateClassLoaderLookup
                    .put(bundleDetail.getCoordinates().getCoordinates(),
                        bundleClassLoader);
                bundleDetailsIter.remove();
              }
            }

            // attempt to load more if some were successfully loaded this iteration
          } while (bundleCount != bundleDetails.size());

          // see if any bundle couldn't be loaded
          for (final BundleDetails bundleDetail : bundleDetails) {
            logger.warn(String
                .format("Unable to resolve required dependency '%s'. Skipping BUNDLE '%s'",
                    bundleDetail.getDependencyCoordinates().getId(),
                    bundleDetail.getBundleFile().getURL().toURI().toString()));
          }
        }
      }
      // did we find it, and if we did was it a file?
      if (StringUtils.isNotEmpty(explicitBundleToLoad)) {
        if (!foundExplicitLoadBundle) {
          StringBuilder builder = new StringBuilder();
          builder.append(String.format("Bundle File %s does not exist in ", explicitBundleToLoad));
          for (FileObject extDir : extensionsDirs) {
            builder.append(extDir.getURL()).append(" ");
          }
          throw new IllegalArgumentException(builder.toString());
        } else if (explicitBundleIsNotFile) {
          throw new IllegalArgumentException(
              String.format("%s was found, but is not a file", explicitBundleToLoad));
        }
      }
      return new BundleClassLoadersContext(fileSystemManager, extensionsDirs,
          new LinkedHashMap<>(directoryBundleLookup), properties);
    }

    /**
     * Loads the details for the specified BUNDLE. The details will be extracted from the manifest
     * file.
     *
     * @param bundleFile the bundle file
     * @return details about the Bundle
     * @throws FileSystemException ioe
     */
    private BundleDetails getBundleDetails(final FileObject bundleFile, BundleProperties props)
        throws FileSystemException {
      return BundleUtil.fromBundleFile(bundleFile, props);
    }

    /**
     * Creates a new BundleClassLoader. The parentClassLoader may be null.
     *
     * @param bundleFile the Bundle File
     * @param parentClassLoader parent classloader of bundle
     * @return the bundle classloader
     * @throws FileSystemException ioe
     * @throws ClassNotFoundException cfne
     */
    private ClassLoader createBundleClassLoader(final FileSystemManager fileSystemManager,
        final FileObject bundleFile, final ClassLoader parentClassLoader)
        throws FileSystemException, ClassNotFoundException {
      logger.debug("Loading Bundle file: " + bundleFile.getURL());
      final ClassLoader bundleClassLoader = new VFSBundleClassLoader.Builder()
          .withFileSystemManager(fileSystemManager)
          .withBundleFile(bundleFile)
          .withParentClassloader(parentClassLoader).build();
      logger.info(
          "Loaded Bundle file: " + bundleFile.getURL() + " as class loader " + bundleClassLoader);
      return bundleClassLoader;
    }
  }

  private List<FileObject> extensionDirs;
  private Map<String, Bundle> bundles;
  private final BundleProperties properties;
  private final FileSystemManager fileSystemManager;

  private BundleClassLoadersContext(
      final FileSystemManager fileSystemManager,
      final List<FileObject> extensionDirs,
      final Map<String, Bundle> bundles,
      final BundleProperties properties) {
    this.extensionDirs = ImmutableList.copyOf(extensionDirs);
    this.bundles = ImmutableMap.copyOf(bundles);
    this.properties = properties;
    this.fileSystemManager = fileSystemManager;
  }

  /**
   * Merges another BundleClassLoadersContext into this one,
   * creating a union of the two.
   * Responsibility for synchronization of access to this context is up to
   * the holder of it's reference
   * @param other a BundleClassLoadersContext instance to merge into this one
   */
  public void merge(BundleClassLoadersContext other) {

    extensionDirs = ImmutableList.copyOf(Stream.concat(extensionDirs.stream(), other.extensionDirs.stream().filter((x)-> !extensionDirs.contains(x))).collect(
        Collectors.toList()));
    bundles = ImmutableMap.copyOf(Stream.of(bundles,other.bundles).map(Map::entrySet).flatMap(
        Collection::stream).collect(
        Collectors.toMap(Entry::getKey, Entry::getValue, (s,a) -> s)));
  }

  public List<FileObject> getExtensionDirs() {
    return extensionDirs;
  }

  public Map<String, Bundle> getBundles() {
    return bundles;
  }

  public BundleProperties getProperties() {
    return properties;
  }

  public FileSystemManager getFileSystemManager() {
    return fileSystemManager;
  }
}
