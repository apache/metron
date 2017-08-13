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

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileSystemManagerFactory;
import org.apache.metron.bundles.util.ResourceCopier;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import static org.apache.metron.bundles.util.TestUtil.loadSpecifiedProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BundleMapperTest {

  static final Map<String, String> EMPTY_MAP = new HashMap<String, String>();

  @AfterClass
  public static void after() {
    ExtensionManager.reset();
    BundleClassLoaders.reset();
  }

  @BeforeClass
  public static void copyResources() throws IOException {
    ResourceCopier.copyResources(Paths.get("./src/test/resources"),Paths.get("./target"));
  }

  @Test
  public void testUnpackBundles()
      throws FileSystemException, URISyntaxException, NotInitializedException {

    BundleProperties properties = loadSpecifiedProperties("/BundleMapper/conf/bundle.properties",
        EMPTY_MAP);

    assertEquals("./target/BundleMapper/lib/",
        properties.getProperty("bundle.library.directory"));
    assertEquals("./target/BundleMapper/lib2/",
        properties.getProperty("bundle.library.directory.alt"));

    FileSystemManager fileSystemManager = FileSystemManagerFactory.createFileSystemManager(new String[] {properties.getArchiveExtension()});
    ArrayList<Class> classes = new ArrayList<>();
    classes.add(MessageParser.class);
    final ExtensionMapping extensionMapping = BundleMapper
        .mapBundles(fileSystemManager,
            properties);

    assertEquals(2, extensionMapping.getAllExtensionNames().size());

    assertTrue(extensionMapping.getAllExtensionNames().keySet().contains(
        "org.apache.metron.foo.FooParser"));
    assertTrue(extensionMapping.getAllExtensionNames().keySet().contains(
        "org.apache.metron.bar.BarParser"));
  }

  @Test
  public void testUnpackBundlesFromEmptyDir()
      throws IOException, FileSystemException, URISyntaxException, NotInitializedException {

    final File emptyDir = new File("./target/empty/dir");
    emptyDir.delete();
    emptyDir.deleteOnExit();
    assertTrue(emptyDir.mkdirs());

    final Map<String, String> others = new HashMap<>();
    others.put("bundle.library.directory.alt", emptyDir.toString());
    BundleProperties properties = loadSpecifiedProperties("/BundleMapper/conf/bundle.properties",
        others);
    FileSystemManager fileSystemManager = FileSystemManagerFactory.createFileSystemManager(new String[] {properties.getArchiveExtension()});
    ArrayList<Class> classes = new ArrayList<>();
    classes.add(MessageParser.class);
    // create a FileSystemManager
    Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
    ExtensionManager.getInstance().init(classes, systemBundle, Collections.emptySet());
    final ExtensionMapping extensionMapping = BundleMapper
        .mapBundles(fileSystemManager,
             properties);

    assertNotNull(extensionMapping);
    assertEquals(1, extensionMapping.getAllExtensionNames().size());
    assertTrue(extensionMapping.getAllExtensionNames().keySet().contains(
        "org.apache.metron.bar.BarParser"));
  }

  @Test
  public void testUnpackBundlesFromNonExistantDir()
      throws FileSystemException, URISyntaxException, NotInitializedException {

    final File nonExistantDir = new File("./target/this/dir/should/not/exist/");
    nonExistantDir.delete();
    nonExistantDir.deleteOnExit();

    final Map<String, String> others = new HashMap<>();
    others.put("bundle.library.directory.alt", nonExistantDir.toString());
    BundleProperties properties = loadSpecifiedProperties("/BundleMapper/conf/bundle.properties",
        others);
    FileSystemManager fileSystemManager = FileSystemManagerFactory.createFileSystemManager(new String[] {properties.getArchiveExtension()});
    ArrayList<Class> classes = new ArrayList<>();
    classes.add(MessageParser.class);
    // create a FileSystemManager
    Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
    ExtensionManager.getInstance().init(classes, systemBundle, Collections.emptySet());
    final ExtensionMapping extensionMapping = BundleMapper
        .mapBundles(fileSystemManager,
             properties);

    assertTrue(extensionMapping.getAllExtensionNames().keySet().contains(
        "org.apache.metron.bar.BarParser"));

    assertEquals(1, extensionMapping.getAllExtensionNames().size());
  }

  @Test
  public void testUnpackBundlesFromNonDir()
      throws IOException, FileSystemException, URISyntaxException, NotInitializedException {

    final File nonDir = new File("./target/file.txt");
    nonDir.createNewFile();
    nonDir.deleteOnExit();

    final Map<String, String> others = new HashMap<>();
    others.put("bundle.library.directory.alt", nonDir.toString());
    BundleProperties properties = loadSpecifiedProperties("/BundleMapper/conf/bundle.properties",
        others);
    // create a FileSystemManager
    FileSystemManager fileSystemManager = FileSystemManagerFactory.createFileSystemManager(new String[] {properties.getArchiveExtension()});
    ArrayList<Class> classes = new ArrayList<>();
    classes.add(MessageParser.class);
    // create a FileSystemManager
    Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
    ExtensionManager.getInstance().init(classes, systemBundle, Collections.emptySet());
    final ExtensionMapping extensionMapping = BundleMapper
        .mapBundles(fileSystemManager,
            properties);

    assertNull(extensionMapping);
  }
}
