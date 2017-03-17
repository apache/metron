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
package org.apache.metron.bundles.integration;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.metron.bundles.*;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.integration.components.MRComponent;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileUtils;
import org.apache.metron.bundles.util.HDFSFileUtilities;
import org.apache.metron.bundles.util.VFSClassloaderUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.apache.metron.bundles.util.TestUtil.loadSpecifiedProperties;
import static org.junit.Assert.*;

public class BundleUnpackerIntegrationTest {
  static final Map<String,String> EMPTY_MAP = new HashMap<String,String>();
  static MRComponent component;
  static Configuration configuration;
  static FileSystem fileSystem;
  @BeforeClass
  public static void setup() {
    component = new MRComponent().withBasePath("target/hdfs");
    component.start();
    configuration = component.getConfiguration();

    try {
      fileSystem = FileSystem.newInstance(configuration);
      fileSystem.mkdirs(new Path("/work/"),new FsPermission(FsAction.READ_WRITE,FsAction.READ_WRITE,FsAction.READ_WRITE));
      fileSystem.copyFromLocalFile(new Path("./src/test/resources/bundle.properties"), new Path("/work/"));
      fileSystem.copyFromLocalFile(new Path("./src/test/resources/BundleUnpacker/lib/"), new Path("/"));
      fileSystem.copyFromLocalFile(new Path("./src/test/resources/BundleUnpacker/lib2/"), new Path("/"));
      RemoteIterator<LocatedFileStatus> files = fileSystem.listFiles(new Path("/"),true);
      System.out.println("==============(BEFORE)==============");
      while (files.hasNext()){
        LocatedFileStatus fileStat = files.next();
        System.out.println(fileStat.getPath().toString());
      }
      ExtensionClassInitializer.initializeFileUtilities(new HDFSFileUtilities(fileSystem));
    } catch (IOException e) {
      throw new RuntimeException("Unable to start cluster", e);
    }
  }

  @AfterClass
  public static void teardown(){
    try {
      RemoteIterator<LocatedFileStatus> files = fileSystem.listFiles(new Path("/"), true);
      System.out.println("==============(AFTER)==============");
      while (files.hasNext()) {
        LocatedFileStatus fileStat = files.next();
        System.out.println(fileStat.getPath().toString());
      }
    }catch(Exception e){}
    component.stop();
    ExtensionClassInitializer.reset();
    BundleClassLoaders.reset();
    FileUtils.reset();
  }

  @Test
  public void testUnpackBundles() throws Exception {
    // we unpack TWICE, because the code is different
    // if the files are there already
    // this still doesn't test what happens
    // if the HASH is different

    unpackBundles();
    unpackBundles();
  }
  public void unpackBundles() throws Exception {
    // setup properties
    BundleProperties properties = loadSpecifiedProperties("/BundleUnpacker/conf/bundle.properties", EMPTY_MAP);
    // get the port we ended up with and set the paths
    properties.setProperty(BundleProperties.HDFS_PREFIX,configuration.get("fs.defaultFS"));
    properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY, "/lib/");
    properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY_PREFIX + "alt","/lib2/");
    properties.setProperty(BundleProperties.BUNDLE_WORKING_DIRECTORY, "/work/");
    properties.setProperty(BundleProperties.COMPONENT_DOCS_DIRECTORY, "/work/docs/components/");
    FileSystemManager fileSystemManager = VFSClassloaderUtil.generateVfs(properties.getArchiveExtension());
    ArrayList<Class> classes = new ArrayList<>();
    classes.add(AbstractFoo.class);
    ExtensionClassInitializer.initialize(classes);
    // create a FileSystemManager
    Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
    ExtensionManager.discoverExtensions(systemBundle, Collections.emptySet());

    final ExtensionMapping extensionMapping = BundleUnpacker.unpackBundles(fileSystemManager, ExtensionManager.createSystemBundle(fileSystemManager, properties), properties);

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
}