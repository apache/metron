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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.BundleCoordinates;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.BundleSelector;
import org.apache.metron.bundles.util.BundleUtil;
import org.apache.metron.bundles.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The BundleMapper loads all the Bundles available to the system and maps their extensions
 *
 */
public final class BundleMapper {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static String archiveExtension = BundleProperties.DEFAULT_ARCHIVE_EXTENSION;
  private final static String META_FMT = "META-INF/services/%s";

  public static ExtensionMapping mapBundles(final FileSystemManager fileSystemManager,
       BundleProperties props) {
    try {
      final List<URI> bundleLibraryDirs = props.getBundleLibraryDirectories();
      final Map<FileObject, BundleCoordinates> bundlesToCoordinates = new HashMap<>();
      archiveExtension = props.getArchiveExtension();

      final List<FileObject> bundleFiles = new ArrayList<>();

      for (URI bundleLibraryDir : bundleLibraryDirs) {

        FileObject bundleDir = fileSystemManager.resolveFile(bundleLibraryDir);

        if(bundleDir.exists() == false) {
          continue;
        }
        // Test if the source BUNDLEs can be read
        FileUtils.ensureDirectoryExistAndCanRead(bundleDir);

        FileObject[] dirFiles = bundleDir.findFiles(new BundleSelector(archiveExtension));
        if (dirFiles != null) {
          List<FileObject> fileList = Arrays.asList(dirFiles);
          bundleFiles.addAll(fileList);
        }
      }

      if (!bundleFiles.isEmpty()) {
        for (FileObject bundleFile : bundleFiles) {
          bundlesToCoordinates
              .put(bundleFile, BundleUtil.coordinateFromBundleFile(bundleFile, props));
        }
      }

      final ExtensionMapping extensionMapping = new ExtensionMapping();
      mapExtensions(bundlesToCoordinates, extensionMapping, props);
      return extensionMapping;
    } catch (IOException | URISyntaxException e) {
      logger.warn("Unable to load BUNDLE library bundles due to " + e
          + " Will proceed without loading any further bundles");
      if (logger.isDebugEnabled()) {
        logger.warn("", e);
      }
    }

    return null;
  }

  private static void mapExtensions(final Map<FileObject, BundleCoordinates> bundlesToCoordinates,
      final ExtensionMapping mapping, BundleProperties props) throws IOException {
    for (final Map.Entry<FileObject, BundleCoordinates> entry : bundlesToCoordinates.entrySet()) {
      final FileObject bundle = entry.getKey();
      final BundleCoordinates bundleCoordinates = entry.getValue();

      mapExtentionsForCoordinate(mapping, bundleCoordinates, bundle, props);
    }
  }

  private static void mapExtentionsForCoordinate(final ExtensionMapping mapping,
      final BundleCoordinates bundleCoordinates, final FileObject bundle, BundleProperties props)
      throws IOException {
    final FileObject bundleFileSystem = bundle.getFileSystem().getFileSystemManager()
        .createFileSystem(bundle);
    final FileObject deps = bundleFileSystem.resolveFile(VFSBundleClassLoader.DEPENDENCY_PATH);
    final FileObject[] directoryContents = deps.getChildren();
    if (directoryContents != null) {
      for (final FileObject file : directoryContents) {
        if (file.getName().getExtension().equals("jar")) {
          mapExtensionsForJarFileObject(bundleCoordinates, file, mapping, props);
        }
      }
    }
  }

  private static void mapExtensionsForJarFileObject(final BundleCoordinates coordinate, final FileObject jar,
    final ExtensionMapping extensionMapping, final BundleProperties props) throws IOException {
    final ExtensionMapping jarExtensionMapping = buildExtensionMappingForJar(coordinate, jar,
        props);

    // skip if there are not components to document
    if (jarExtensionMapping.isEmpty()) {
      return;
    }

    // merge the extension mapping found in this jar
    extensionMapping.merge(jarExtensionMapping);
  }


  private static ExtensionMapping buildExtensionMappingForJar(final BundleCoordinates coordinate,
    final FileObject jar, final BundleProperties props) throws IOException {
    final ExtensionMapping mapping = new ExtensionMapping();

    // The BundleProperties has configuration for the extension names and classnames
    final Map<String, String> extensions = props.getBundleExtensionTypes();
    if (extensions.isEmpty()) {
      logger.info("No Extensions configured in properties");
      return mapping;
    }
    JarEntry jarEntry;
    try (final JarInputStream jarFile = new JarInputStream(jar.getContent().getInputStream())) {

      while ((jarEntry = jarFile.getNextJarEntry()) != null) {
        for (Map.Entry<String, String> extensionEntry : extensions.entrySet()) {
          if (jarEntry.getName().equals(String.format(META_FMT, extensionEntry.getValue()))) {
            mapping.addAllExtensions(extensionEntry.getKey(), coordinate,
                buildExtensionMappingForJar(jarFile, jarEntry));
          }
        }
      }
    }
    return mapping;

  }

  private static List<String> buildExtensionMappingForJar(final JarInputStream jarFile,
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

  private BundleMapper() {
  }
}
