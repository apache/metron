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

import java.net.URISyntaxException;
import java.util.*;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.bundle.BundleCoordinate;
import org.apache.metron.bundles.bundle.BundleDetails;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileUtils;
import org.apache.metron.bundles.util.BundleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton class used to initialize the extension and framework
 * classloaders.
 */
public final class BundleClassLoaders {

    private static volatile BundleClassLoaders ncl;
    private volatile InitContext initContext;
    private static final Logger logger = LoggerFactory.getLogger(BundleClassLoaders.class);

    private final static class InitContext {

        private final FileObject frameworkWorkingDir;
        private final FileObject extensionWorkingDir;
        private final Map<String, Bundle> bundles;

        private InitContext(
                final FileObject frameworkDir,
                final FileObject extensionDir,
                final Map<String, Bundle> bundles) {
            this.frameworkWorkingDir = frameworkDir;
            this.extensionWorkingDir = extensionDir;
            this.bundles = bundles;
        }
    }

    private BundleClassLoaders() {
    }

    /**
     * @return The singleton instance of the BundleClassLoaders
     */
    public static BundleClassLoaders getInstance() {
        BundleClassLoaders result = ncl;
        if (result == null) {
            synchronized (BundleClassLoaders.class) {
                result = ncl;
                if (result == null) {
                    ncl = result = new BundleClassLoaders();
                }
            }
        }
        return result;
    }

    public static void reset(){
        getInstance().unInit();
    }

    private void unInit(){
     synchronized (this){
         initContext = null;
     }
    }

    /**
     * Initializes and loads the BundleClassLoaders. This method must be called
     * before the rest of the methods to access the classloaders are called and
     * it can be safely called any number of times provided the same framework
     * and extension working dirs are used.
     * @param fileSystemManager the FileSystemManager
     * @param frameworkWorkingDir where to find framework artifacts
     * @param extensionsWorkingDir where to find extension artifacts
     * @param props BundleProperties
     * @throws FileSystemException if any issue occurs while exploding bundle working directories.
     * @throws java.lang.ClassNotFoundException if unable to load class definition
     * @throws IllegalStateException already initialized with a given pair of
     * directories cannot reinitialize or use a different pair of directories.
     */
    public void init(final FileSystemManager fileSystemManager, final FileObject frameworkWorkingDir, final FileObject extensionsWorkingDir, BundleProperties props) throws FileSystemException, ClassNotFoundException, URISyntaxException {
        if (frameworkWorkingDir == null || extensionsWorkingDir == null || fileSystemManager == null) {
            throw new NullPointerException("cannot have empty arguments");
        }
        InitContext ic = initContext;
        if (ic == null) {
            synchronized (this) {
                ic = initContext;
                if (ic == null) {
                    initContext = ic = load(fileSystemManager, frameworkWorkingDir, extensionsWorkingDir, props);
                }
            }
        }
        boolean matching = initContext.extensionWorkingDir.equals(extensionsWorkingDir)
                && initContext.frameworkWorkingDir.equals(frameworkWorkingDir);
        if (!matching) {
            throw new IllegalStateException("Cannot reinitialize and extension/framework directories cannot change");
        }
    }

    /**
     * Should be called at most once.
     */
    private InitContext load(final FileSystemManager fileSystemManager, final FileObject frameworkWorkingDir, final FileObject extensionsWorkingDir, BundleProperties props) throws FileSystemException, ClassNotFoundException, URISyntaxException {
        // get the system classloader
        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        // find all bundle files and create class loaders for them.
        final Map<String, Bundle> directoryBundleLookup = new LinkedHashMap<>();
        final Map<String, ClassLoader> coordinateClassLoaderLookup = new HashMap<>();
        final Map<String, Set<BundleCoordinate>> idBundleLookup = new HashMap<>();

        // make sure the bundle directory is there and accessible
        FileUtils.ensureDirectoryExistAndCanReadAndWrite(frameworkWorkingDir);
        FileUtils.ensureDirectoryExistAndCanReadAndWrite(extensionsWorkingDir);

        final List<FileObject> bundleWorkingDirContents = new ArrayList<>();
        final FileObject[] frameworkWorkingDirContents = frameworkWorkingDir.getChildren();
        if (frameworkWorkingDirContents != null) {
            bundleWorkingDirContents.addAll(Arrays.asList(frameworkWorkingDirContents));
        }
        final FileObject[] extensionsWorkingDirContents = extensionsWorkingDir.getChildren();
        if (extensionsWorkingDirContents != null) {
            bundleWorkingDirContents.addAll(Arrays.asList(extensionsWorkingDirContents));
        }

        if (!bundleWorkingDirContents.isEmpty()) {
            final List<BundleDetails> bundleDetails = new ArrayList<>();
            final Map<String,String> bundleCoordinatesToWorkingDir = new HashMap<>();

            // load the bundle details which includes and nar dependencies
            for (final FileObject unpackedBundle : bundleWorkingDirContents) {
                BundleDetails bundleDetail = null;
                try {
                     bundleDetail = getBundleDetails(unpackedBundle, props);
                } catch (IllegalStateException e) {
                    logger.warn("Unable to load BUNDLE {} due to {}, skipping...",
                            new Object[] {unpackedBundle.getURL(), e.getMessage()});
                }

                // prevent the application from starting when there are two BUNDLEs with same group, id, and version
                final String bundleCoordinate = bundleDetail.getCoordinate().getCoordinate();
                if (bundleCoordinatesToWorkingDir.containsKey(bundleCoordinate)) {
                    final String existingBundleWorkingDir = bundleCoordinatesToWorkingDir.get(bundleCoordinate);
                    throw new IllegalStateException("Unable to load BUNDLE with coordinates " + bundleCoordinate
                            + " and working directory " + bundleDetail.getWorkingDirectory()
                            + " because another BUNDLE with the same coordinates already exists at " + existingBundleWorkingDir);
                }

                bundleDetails.add(bundleDetail);
                bundleCoordinatesToWorkingDir.put(bundleCoordinate, bundleDetail.getWorkingDirectory().getURL().toURI().toString());
            }

            // attempt to locate the jetty bundle
            ClassLoader jettyClassLoader = null;
            for (final Iterator<BundleDetails> bundleDetailsIter = bundleDetails.iterator(); bundleDetailsIter.hasNext();) {
                final BundleDetails bundleDetail = bundleDetailsIter.next();
                // populate bundle lookup
                idBundleLookup.computeIfAbsent(bundleDetail.getCoordinate().getId(), id -> new HashSet<>()).add(bundleDetail.getCoordinate());
            }

            int bundleCount;
            do {
                // record the number of bundles to be loaded
                bundleCount = bundleDetails.size();

                // attempt to create each bundle class loader
                for (final Iterator<BundleDetails> bundleDetailsIter = bundleDetails.iterator(); bundleDetailsIter.hasNext();) {
                    final BundleDetails bundleDetail = bundleDetailsIter.next();
                    final BundleCoordinate bundleDependencyCoordinate = bundleDetail.getDependencyCoordinate();

                    // see if this class loader is eligible for loading
                    ClassLoader potentialBundleClassLoader = null;
                    if (bundleDependencyCoordinate == null) {
                        potentialBundleClassLoader = createBundleClassLoader(fileSystemManager, bundleDetail.getWorkingDirectory(), ClassLoader.getSystemClassLoader() );
                    } else {
                        final String dependencyCoordinateStr = bundleDependencyCoordinate.getCoordinate();

                        // if the declared dependency has already been loaded
                        if (coordinateClassLoaderLookup.containsKey(dependencyCoordinateStr)) {
                            final ClassLoader bundleDependencyClassLoader = coordinateClassLoaderLookup.get(dependencyCoordinateStr);
                            potentialBundleClassLoader = createBundleClassLoader(fileSystemManager, bundleDetail.getWorkingDirectory(), bundleDependencyClassLoader);
                        } else {
                            // get all bundles that match the declared dependency id
                            final Set<BundleCoordinate> coordinates = idBundleLookup.get(bundleDependencyCoordinate.getId());

                            // ensure there are known bundles that match the declared dependency id
                            if (coordinates != null && !coordinates.contains(bundleDependencyCoordinate)) {
                                // ensure the declared dependency only has one possible bundle
                                if (coordinates.size() == 1) {
                                    // get the bundle with the matching id
                                    final BundleCoordinate coordinate = coordinates.stream().findFirst().get();

                                    // if that bundle is loaded, use it
                                    if (coordinateClassLoaderLookup.containsKey(coordinate.getCoordinate())) {
                                        logger.warn(String.format("While loading '%s' unable to locate exact NAR dependency '%s'. Only found one possible match '%s'. Continuing...",
                                                bundleDetail.getCoordinate().getCoordinate(), dependencyCoordinateStr, coordinate.getCoordinate()));

                                        final ClassLoader bundleDependencyClassLoader = coordinateClassLoaderLookup.get(coordinate.getCoordinate());
                                        potentialBundleClassLoader = createBundleClassLoader(fileSystemManager, bundleDetail.getWorkingDirectory(), bundleDependencyClassLoader);
                                    }
                                }
                            }
                        }
                    }

                    // if we were able to create the bundle class loader, store it and remove the details
                    final ClassLoader bundleClassLoader = potentialBundleClassLoader;
                    if (bundleClassLoader != null) {
                        directoryBundleLookup.put(bundleDetail.getWorkingDirectory().getURL().toURI().toString(), new Bundle(bundleDetail, bundleClassLoader));
                        coordinateClassLoaderLookup.put(bundleDetail.getCoordinate().getCoordinate(), bundleClassLoader);
                        bundleDetailsIter.remove();
                    }
                }

                // attempt to load more if some were successfully loaded this iteration
            } while (bundleCount != bundleDetails.size());

            // see if any bundle couldn't be loaded
            for (final BundleDetails bundleDetail : bundleDetails) {
                logger.warn(String.format("Unable to resolve required dependency '%s'. Skipping BUNDLE '%s'",
                        bundleDetail.getDependencyCoordinate().getId(), bundleDetail.getWorkingDirectory().getURL().toURI().toString()));
            }
        }

        return new InitContext(frameworkWorkingDir, extensionsWorkingDir, new LinkedHashMap<>(directoryBundleLookup));
    }

    /**
     * Creates a new BundleClassLoader. The parentClassLoader may be null.
     *
     * @param bundleDirectory root directory of bundle
     * @param parentClassLoader parent classloader of bundle
     * @return the bundle classloader
     * @throws FileSystemException ioe
     * @throws ClassNotFoundException cfne
     */
    private static ClassLoader createBundleClassLoader(final FileSystemManager fileSystemManager, final FileObject bundleDirectory, final ClassLoader parentClassLoader) throws FileSystemException, ClassNotFoundException {
        logger.debug("Loading Bundle file: " + bundleDirectory.getURL());
        BundleClassLoader.Builder builder = new BundleClassLoader.Builder()
                .withFileSystemManager(fileSystemManager)
                .withBundleWorkingDirectory(bundleDirectory)
                .withParentClassloader(parentClassLoader);
        final ClassLoader bundleClassLoader = builder.build();
        logger.info("Loaded Bundle file: " + bundleDirectory.getURL() + " as class loader " + bundleClassLoader);
        return bundleClassLoader;
    }

    /**
     * Loads the details for the specified BUNDLE. The details will be extracted
     * from the manifest file.
     *
     * @param bundleDirectory the bundle directory
     * @return details about the Bundle
     * @throws FileSystemException ioe
     */
    private static BundleDetails getBundleDetails(final FileObject bundleDirectory, BundleProperties props) throws FileSystemException {
        return BundleUtil.fromBundleDirectory(bundleDirectory, props);
    }

    /**
     * @param extensionWorkingDirectory the directory
     * @return the bundle for the specified working directory. Returns
     * null when no bundle exists for the specified working directory
     * @throws IllegalStateException if the bundles have not been loaded
     */
    public Bundle getBundle(final FileObject extensionWorkingDirectory) {
        if (initContext == null) {
            throw new IllegalStateException("Extensions class loaders have not been loaded.");
        }

        try {
           return initContext.bundles.get(extensionWorkingDirectory.getURL().toURI().toString());
        } catch (URISyntaxException | FileSystemException e) {
            if(logger.isDebugEnabled()){
                logger.debug("Unable to get extension classloader for working directory '{}'", extensionWorkingDirectory.getName().toString());
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

        return new LinkedHashSet<>(initContext.bundles.values());
    }

}
