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

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.*;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.bundle.BundleCoordinate;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.StringUtils;
import org.apache.metron.bundles.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class BundleUnpacker {

    private static final Logger logger = LoggerFactory.getLogger(BundleUnpacker.class);
    private static String HASH_FILENAME = "bundle-md5sum";
    private static String archiveExtension = BundleProperties.DEFAULT_ARCHIVE_EXTENSION;
    private static final FileSelector BUNDLE_FILTER = new FileSelector() {
        @Override
        public boolean includeFile(FileSelectInfo fileSelectInfo) throws Exception {
            final String nameToTest = fileSelectInfo.getFile().getName().getExtension();
            return nameToTest.equals(archiveExtension) && fileSelectInfo.getFile().isFile();
        }

        @Override
        public boolean traverseDescendents(FileSelectInfo fileSelectInfo) throws Exception {
            return true;
        }
    };

    public static ExtensionMapping unpackBundles(final FileSystemManager fileSystemManager, final Bundle systemBundle, BundleProperties props){
        try{
            final List<URI> bundleLibraryDirs = props.getBundleLibraryDirectories();
            final URI frameworkWorkingDir = props.getFrameworkWorkingDirectory();
            final URI extensionsWorkingDir = props.getExtensionsWorkingDirectory();
            final URI docsWorkingDir = props.getComponentDocumentationWorkingDirectory();
            final Map<FileObject, BundleCoordinate> unpackedBundles = new HashMap<>();
            archiveExtension = props.getArchiveExtension();

            FileObject unpackedFramework = null;
            final FileObject frameworkWorkingDirFO = fileSystemManager.resolveFile(frameworkWorkingDir);
            final FileObject extensionsWorkingDirFO = fileSystemManager.resolveFile(extensionsWorkingDir);
            final FileObject docsWorkingDirFO = fileSystemManager.resolveFile(docsWorkingDir);
            final Set<FileObject> unpackedExtensions = new HashSet<>();
            final List<FileObject> bundleFiles = new ArrayList<>();

            // make sure the bundle directories are there and accessible
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(frameworkWorkingDirFO);
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(extensionsWorkingDirFO);
            FileUtils.ensureDirectoryExistAndCanReadAndWrite(docsWorkingDirFO);

            for (URI bundleLibraryDir : bundleLibraryDirs) {

                FileObject bundleDir = fileSystemManager.resolveFile(bundleLibraryDir);

                // Test if the source BUNDLEs can be read
                FileUtils.ensureDirectoryExistAndCanRead(bundleDir);

                FileObject[] dirFiles = bundleDir.findFiles(BUNDLE_FILTER);
                if (dirFiles != null) {
                    List<FileObject> fileList = Arrays.asList(dirFiles);
                    bundleFiles.addAll(fileList);
                }
            }

            if (!bundleFiles.isEmpty()) {
                final long startTime = System.nanoTime();
                logger.info("Expanding " + bundleFiles.size() + " Bundle files with all processors...");
                for (FileObject bundleFile : bundleFiles) {
                    logger.debug("Expanding Bundle file: " + bundleFile.getURL().toString());

                    // get the manifest for this bundle
                    Manifest manifest = null;
                    try(JarInputStream jis = new JarInputStream(bundleFile.getContent().getInputStream())) {
                        JarEntry je;
                        manifest = jis.getManifest();
                        if(manifest == null){
                            while((je=jis.getNextJarEntry())!=null){
                                if(je.getName().equals("META-INF/MANIFEST.MF")){
                                    manifest = new Manifest(jis);
                                    break;
                                }
                            }
                        }
                    }
                    final String bundleId = manifest.getMainAttributes().getValue(props.getMetaIdPrefix() + BundleManifestEntry.PRE_ID.getManifestName());
                    final String groupId = manifest.getMainAttributes().getValue(props.getMetaIdPrefix() + BundleManifestEntry.PRE_GROUP.getManifestName());
                    final String version = manifest.getMainAttributes().getValue(props.getMetaIdPrefix() + BundleManifestEntry.PRE_VERSION.getManifestName());

                    final FileObject unpackedExtension = unpackBundle(bundleFile, extensionsWorkingDirFO);

                    // record the current bundle
                    unpackedBundles.put(unpackedExtension, new BundleCoordinate(groupId, bundleId, version));

                    // unpack the extension bundle
                    unpackedExtensions.add(unpackedExtension);

                }

                // Determine if any bundles no longer exist and delete their
                // working directories. This happens
                // if a new version of a bundle is dropped into the lib dir.
                // ensure no old framework are present
                final FileObject[] frameworkWorkingDirContents = frameworkWorkingDirFO.getChildren();
                if (frameworkWorkingDirContents != null) {
                    for (final FileObject unpackedBundle : frameworkWorkingDirContents) {
                            FileUtils.deleteFile(unpackedBundle, true);
                    }
                }

                // ensure no old extensions are present
                final FileObject[] extensionsWorkingDirContents = extensionsWorkingDirFO.getChildren();
                if (extensionsWorkingDirContents != null) {
                    for (final FileObject unpackedBundle : extensionsWorkingDirContents) {
                        if (!unpackedExtensions.contains(unpackedBundle)) {
                            FileUtils.deleteFile(unpackedBundle, true);
                        }
                    }
                }
                final long endTime = System.nanoTime();
                logger.info("BUNDLE loading process took " + (endTime - startTime) + " nanoseconds.");
            }

            // attempt to delete any docs files that exist so that any
            // components that have been removed
            // will no longer have entries in the docs folder
            final FileObject[] docsFiles = docsWorkingDirFO.getChildren();
            if (docsFiles != null) {
                for (final FileObject file : docsFiles) {
                    FileUtils.deleteFile(file, true);
                }
            }
            final ExtensionMapping extensionMapping = new ExtensionMapping();
            mapExtensions(unpackedBundles, docsWorkingDirFO, extensionMapping, props);

            // unpack docs for the system bundle which will catch any JARs directly in the lib directory that might have docs
            unpackBundleDocs(docsWorkingDirFO, extensionMapping, systemBundle.getBundleDetails().getCoordinate(), systemBundle.getBundleDetails().getWorkingDirectory(), props);

            return extensionMapping;
        } catch (IOException | URISyntaxException | NotInitializedException e) {
            logger.warn("Unable to load BUNDLE library bundles due to " + e
                    + " Will proceed without loading any further bundles");
            if (logger.isDebugEnabled()) {
                logger.warn("", e);
            }
        }

        return null;
    }

    private static void mapExtensions(final Map<FileObject, BundleCoordinate> unpackedBundles, final FileObject docsDirectory, final ExtensionMapping mapping, BundleProperties props) throws IOException {
        for (final Map.Entry<FileObject, BundleCoordinate> entry : unpackedBundles.entrySet()) {
            final FileObject unpackedBundle = entry.getKey();
            final BundleCoordinate bundleCoordinate = entry.getValue();

            final FileObject bundledDependencies = unpackedBundle.resolveFile("META-INF/bundled-dependencies");

            unpackBundleDocs(docsDirectory, mapping, bundleCoordinate, bundledDependencies, props);
        }
    }

    private static void unpackBundleDocs(final FileObject docsDirectory, final ExtensionMapping mapping, final BundleCoordinate bundleCoordinate, final FileObject bundledDirectory, BundleProperties props) throws IOException {
        final FileObject[] directoryContents = bundledDirectory.getChildren();
        if (directoryContents != null) {
            for (final FileObject file : directoryContents) {
                if (file.getName().getExtension().equals("jar")) {
                    unpackDocumentation(bundleCoordinate, file, docsDirectory, mapping, props);
                }
            }
        }
    }

    /**
     * Unpacks the specified bundle into the specified base working directory.
     *
     * @param bundle
     *            the bundle to unpack
     * @param baseWorkingDirectory
     *            the directory to unpack to
     * @return the directory to the unpacked bundle
     * @throws IOException
     *             if unable to explode bundle
     * @throws NotInitializedException
     *             if system was not initialized
     */
    private static FileObject unpackBundle(final FileObject bundle, final FileObject baseWorkingDirectory)
            throws IOException, NotInitializedException {

        final FileObject bundleWorkingDirectory = baseWorkingDirectory.resolveFile(bundle.getName().getBaseName() + "-unpacked");

        // if the working directory doesn't exist, unpack the bundle
        if (!bundleWorkingDirectory.exists()) {
            unpack(bundle, bundleWorkingDirectory, calculateMd5sum(bundle));
        } else {
            // the working directory does exist. Run MD5 sum against the bundle
            // file and check if the bundle has changed since it was deployed.
            final byte[] bundleMd5 = calculateMd5sum(bundle);
            final FileObject workingHashFile = bundleWorkingDirectory.getChild(HASH_FILENAME);
            if (!workingHashFile.exists()) {
                FileUtils.deleteFile(bundleWorkingDirectory, true);
                unpack(bundle, bundleWorkingDirectory, bundleMd5);
            } else {
                final byte[] hashFileContents = IOUtils.toByteArray(workingHashFile.getContent().getInputStream());
                if (!Arrays.equals(hashFileContents, bundleMd5)) {
                    logger.info("Contents of bundle {} have changed. Reloading.",
                            new Object[] { bundle.getURL() });
                    FileUtils.deleteFile(bundleWorkingDirectory, true);
                    unpack(bundle, bundleWorkingDirectory, bundleMd5);
                }
            }
        }

        return bundleWorkingDirectory;
    }

    /**
     * Unpacks the BUNDLE to the specified directory. Creates a checksum file that
     * used to determine if future expansion is necessary.
     *
     * @param workingDirectory
     *            the root directory to which the BUNDLE should be unpacked.
     * @throws IOException
     *             if the BUNDLE could not be unpacked.
     */
    private static void unpack(final FileObject bundle, final FileObject workingDirectory, final byte[] hash)
            throws IOException {
        try(ZipInputStream jarFile = new ZipInputStream(bundle.getContent().getInputStream())){
            ZipEntry je;
            while((je=jarFile.getNextEntry())!=null){
                String name = je.getName();
                FileObject f = workingDirectory.resolveFile(name);
                if (je.isDirectory()) {
                    FileUtils.ensureDirectoryExistAndCanAccess(f);
                } else {
                    makeFile(jarFile, f);
                }
            }
        }

        final FileObject hashFile = workingDirectory.resolveFile(HASH_FILENAME);
        FileUtils.createFile(hashFile, hash);
    }

    private static void unpackDocumentation(final BundleCoordinate coordinate, final FileObject jar, final FileObject docsDirectory, final ExtensionMapping extensionMapping, final BundleProperties props) throws IOException {
        final ExtensionMapping jarExtensionMapping = determineDocumentedComponents(coordinate, jar, props);

        // skip if there are not components to document
        if (jarExtensionMapping.isEmpty()) {
            return;
        }

        // merge the extension mapping found in this jar
        extensionMapping.merge(jarExtensionMapping);

        // look for all documentation related to each component
        try (final JarInputStream jarFile = new JarInputStream(jar.getContent().getInputStream())) {
            for (final String componentName : extensionMapping.getAllExtensionNames().keySet()) {
                final String entryName = "docs/" + componentName;

                // go through each entry in this jar
                JarEntry jarEntry;
                while ((jarEntry = jarFile.getNextJarEntry()) != null) {

                    // if this entry is documentation for this component
                    if (jarEntry.getName().startsWith(entryName)) {
                        final String name = StringUtils.substringAfter(jarEntry.getName(), "docs/");
                        final String path = coordinate.getGroup() + "/" + coordinate.getId() + "/" + coordinate.getVersion() + "/" + name;

                        // if this is a directory create it
                        if (jarEntry.isDirectory()) {
                            final FileObject componentDocsDirectory = docsDirectory.getChild(name);

                            // ensure the documentation directory can be created
                            if (!componentDocsDirectory.exists()) {
                                componentDocsDirectory.createFolder();
                                if (!componentDocsDirectory.exists()) {
                                    logger.warn("Unable to create docs directory "
                                            + componentDocsDirectory.getURL());

                                    break;
                                }
                            } else {
                                // if this is a file, write to it
                                final FileObject componentDoc = docsDirectory.getChild(name);
                                makeFile(jarFile, componentDoc);
                            }
                        }
                    }

                }
            }
        }
    }

    final static String META_FMT = "META-INF/services/%s";

    /*
     * Returns true if this jar file contains a bundle component
     */
    private static ExtensionMapping determineDocumentedComponents(final BundleCoordinate coordinate, final FileObject jar, final BundleProperties props) throws IOException {
        final ExtensionMapping mapping = new ExtensionMapping();

            // The BundleProperties has configuration for the extension names and classnames
            final Map<String,String> extensions = props.getBundleExtensionTypes();
            if(extensions.isEmpty()){
                logger.info("No Extensions configured in properties");
                return mapping;
            }
            JarEntry jarEntry;
        try (final JarInputStream jarFile = new JarInputStream(jar.getContent().getInputStream())) {

            while ((jarEntry = jarFile.getNextJarEntry()) != null) {
                for (Map.Entry<String, String> extensionEntry : extensions.entrySet()) {
                    if (jarEntry.getName().equals(String.format(META_FMT, extensionEntry.getValue()))) {
                        mapping.addAllExtensions(extensionEntry.getKey(),coordinate, determineDocumentedComponents(jarFile, jarEntry));
                    }
                }
            }
        }
            return mapping;

    }

    private static List<String> determineDocumentedComponents(final JarInputStream jarFile,
                                                              final JarEntry jarEntry) throws IOException {
        final List<String> componentNames = new ArrayList<>();

        if (jarEntry == null) {
            return componentNames;
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(
                jarFile));
            String line;
            while ((line = reader.readLine()) != null) {
                final String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                    final int indexOfPound = trimmedLine.indexOf("#");
                    final String effectiveLine = (indexOfPound > 0) ? trimmedLine.substring(0,
                            indexOfPound) : trimmedLine;
                    componentNames.add(effectiveLine);
                }
            }
        return componentNames;
    }

    /**
     * Creates the specified file, whose contents will come from the
     * <tt>InputStream</tt>.
     *
     * @param inputStream
     *            the contents of the file to create.
     * @param file
     *            the file to create.
     * @throws FileSystemException
     *             if the file could not be created.
     */
    private static void makeFile(final InputStream inputStream, final FileObject file) throws FileSystemException{
        FileUtils.createFile(file, inputStream);
    }

    /**
     * Calculates an md5 sum of the specified file.
     *
     * @param file
     *            to calculate the md5sum of
     * @return the md5sum bytes
     * @throws IOException
     *             if cannot read file
     */
    private static byte[] calculateMd5sum(final FileObject file) throws IOException {
        try (final InputStream inputStream = file.getContent().getInputStream()) {
            final MessageDigest md5 = MessageDigest.getInstance("md5");

            final byte[] buffer = new byte[1024];
            int read = inputStream.read(buffer);

            while (read > -1) {
                md5.update(buffer, 0, read);
                read = inputStream.read(buffer);
            }

            return md5.digest();
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalArgumentException(nsae);
        }
    }

    private BundleUnpacker() {
    }
}
