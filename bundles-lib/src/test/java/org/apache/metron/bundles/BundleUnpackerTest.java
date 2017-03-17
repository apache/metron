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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileUtils;
import org.apache.metron.bundles.util.VFSClassloaderUtil;
import org.apache.nifi.processor.Processor;
import org.junit.AfterClass;
import org.junit.Assert;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BundleUnpackerTest {

    static final Map<String,String> EMPTY_MAP = new HashMap<String,String>();
    @AfterClass
    public static void after(){
        ExtensionClassInitializer.reset();
        BundleClassLoaders.reset();
        FileUtils.reset();

    }

    @BeforeClass
    public static void copyResources() throws IOException {

        final Path sourcePath = Paths.get("./src/test/resources");
        final Path targetPath = Paths.get("./target");

        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {

                Path relativeSource = sourcePath.relativize(dir);
                Path target = targetPath.resolve(relativeSource);

                Files.createDirectories(target);

                return FileVisitResult.CONTINUE;

            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                Path relativeSource = sourcePath.relativize(file);
                Path target = targetPath.resolve(relativeSource);

                Files.copy(file, target, REPLACE_EXISTING);

                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    public void testUnpackBundles() throws FileSystemException, URISyntaxException, NotInitializedException {

        BundleProperties properties = loadSpecifiedProperties("/BundleUnpacker/conf/bundle.properties", EMPTY_MAP);

        assertEquals("./target/BundleUnpacker/lib/",
                properties.getProperty("bundle.library.directory"));
        assertEquals("./target/BundleUnpacker/lib2/",
                properties.getProperty("bundle.library.directory.alt"));

        FileSystemManager fileSystemManager = VFSClassloaderUtil.generateVfs(properties.getArchiveExtension());
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(Processor.class);
        ExtensionClassInitializer.initialize(classes);
        final ExtensionMapping extensionMapping = BundleUnpacker.unpackBundles(fileSystemManager,ExtensionManager.createSystemBundle(fileSystemManager, properties),properties);

        assertEquals(2, extensionMapping.getAllExtensionNames().size());

        assertTrue(extensionMapping.getAllExtensionNames().keySet().contains(
                "org.apache.nifi.processors.dummy.one"));
        assertTrue(extensionMapping.getAllExtensionNames().keySet().contains(
                "org.apache.nifi.processors.dummy.two"));
        final FileObject extensionsWorkingDir = fileSystemManager.resolveFile(properties.getExtensionsWorkingDirectory());
        FileObject[] extensionFiles = extensionsWorkingDir.getChildren();

        Set<String> expectedBundles = new HashSet<>();
        expectedBundles.add("dummy-one.foo-unpacked");
        expectedBundles.add("dummy-two.foo-unpacked");
        assertEquals(expectedBundles.size(), extensionFiles.length);

        for (FileObject extensionFile : extensionFiles) {
            Assert.assertTrue(expectedBundles.contains(extensionFile.getName().getBaseName()));
        }
    }

    @Test
    public void testUnpackBundlesFromEmptyDir() throws IOException, FileSystemException, URISyntaxException, NotInitializedException {

        final File emptyDir = new File("./target/empty/dir");
        emptyDir.delete();
        emptyDir.deleteOnExit();
        assertTrue(emptyDir.mkdirs());

        final Map<String, String> others = new HashMap<>();
        others.put("bundle.library.directory.alt", emptyDir.toString());
        BundleProperties properties = loadSpecifiedProperties("/BundleUnpacker/conf/bundle.properties", others);
        FileSystemManager fileSystemManager = VFSClassloaderUtil.generateVfs(properties.getArchiveExtension());
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(Processor.class);
        ExtensionClassInitializer.initialize(classes);
        // create a FileSystemManager
        Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
        ExtensionManager.discoverExtensions(systemBundle, Collections.emptySet());
        final ExtensionMapping extensionMapping = BundleUnpacker.unpackBundles(fileSystemManager, ExtensionManager.createSystemBundle(fileSystemManager, properties),properties);

        assertEquals(1, extensionMapping.getAllExtensionNames().size());
        assertTrue(extensionMapping.getAllExtensionNames().keySet().contains(
                "org.apache.nifi.processors.dummy.one"));

        final FileObject extensionsWorkingDir = fileSystemManager.resolveFile(properties.getExtensionsWorkingDirectory());
        FileObject[] extensionFiles = extensionsWorkingDir.getChildren();

        assertEquals(1, extensionFiles.length);
        assertEquals("dummy-one.foo-unpacked", extensionFiles[0].getName().getBaseName());
    }

    @Test
    public void testUnpackBundlesFromNonExistantDir() throws FileSystemException, URISyntaxException, NotInitializedException {

        final File nonExistantDir = new File("./target/this/dir/should/not/exist/");
        nonExistantDir.delete();
        nonExistantDir.deleteOnExit();

        final Map<String, String> others = new HashMap<>();
        others.put("bundle.library.directory.alt", nonExistantDir.toString());
        BundleProperties properties = loadSpecifiedProperties("/BundleUnpacker/conf/bundle.properties", others);
        FileSystemManager fileSystemManager = VFSClassloaderUtil.generateVfs(properties.getArchiveExtension());
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(Processor.class);
        ExtensionClassInitializer.initialize(classes);
        // create a FileSystemManager
        Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
        ExtensionManager.discoverExtensions(systemBundle, Collections.emptySet());
        final ExtensionMapping extensionMapping = BundleUnpacker.unpackBundles(fileSystemManager, ExtensionManager.createSystemBundle(fileSystemManager, properties), properties);

        assertTrue(extensionMapping.getAllExtensionNames().keySet().contains(
                "org.apache.nifi.processors.dummy.one"));

        assertEquals(1, extensionMapping.getAllExtensionNames().size());

        final FileObject extensionsWorkingDir = fileSystemManager.resolveFile(properties.getExtensionsWorkingDirectory());
        FileObject[] extensionFiles = extensionsWorkingDir.getChildren();

        assertEquals(1, extensionFiles.length);
        assertEquals("dummy-one.foo-unpacked", extensionFiles[0].getName().getBaseName());
    }

    @Test
    public void testUnpackBundlesFromNonDir() throws IOException, FileSystemException, URISyntaxException, NotInitializedException {

        final File nonDir = new File("./target/file.txt");
        nonDir.createNewFile();
        nonDir.deleteOnExit();

        final Map<String, String> others = new HashMap<>();
        others.put("bundle.library.directory.alt", nonDir.toString());
        BundleProperties properties = loadSpecifiedProperties("/BundleUnpacker/conf/bundle.properties", others);
        // create a FileSystemManager
        FileSystemManager fileSystemManager = VFSClassloaderUtil.generateVfs(properties.getArchiveExtension());
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(Processor.class);
        ExtensionClassInitializer.initialize(classes);
        // create a FileSystemManager
        Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
        ExtensionManager.discoverExtensions(systemBundle, Collections.emptySet());
        final ExtensionMapping extensionMapping = BundleUnpacker.unpackBundles(fileSystemManager, ExtensionManager.createSystemBundle(fileSystemManager, properties), properties);

        assertNull(extensionMapping);
    }
}
