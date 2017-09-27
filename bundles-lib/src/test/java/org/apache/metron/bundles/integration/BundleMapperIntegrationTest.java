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

import static org.apache.metron.bundles.util.TestUtil.loadSpecifiedProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.metron.bundles.BundleClassLoaders;
import org.apache.metron.bundles.BundleMapper;
import org.apache.metron.bundles.ExtensionManager;
import org.apache.metron.bundles.ExtensionMapping;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileSystemManagerFactory;
import org.apache.metron.integration.components.MRComponent;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BundleMapperIntegrationTest {

  static final Map<String, String> EMPTY_MAP = new HashMap<String, String>();
  static MRComponent component;
  static Configuration configuration;
  static FileSystem fileSystem;


  @AfterClass
  public static void after() {
    ExtensionManager.reset();
    BundleClassLoaders.reset();
  }

  @After
  public void afterTest() {
    ExtensionManager.reset();
    BundleClassLoaders.reset();
  }
  @BeforeClass
  public static void setup() {
    ExtensionManager.reset();
    BundleClassLoaders.reset();
    component = new MRComponent().withBasePath("target/hdfs");
    component.start();
    configuration = component.getConfiguration();

    try {
      fileSystem = FileSystem.newInstance(configuration);
      fileSystem.mkdirs(new Path("/work/"),
          new FsPermission(FsAction.READ_WRITE, FsAction.READ_WRITE, FsAction.READ_WRITE));
      fileSystem.copyFromLocalFile(new Path("./src/test/resources/bundle.properties"),
          new Path("/work/"));
      fileSystem
          .copyFromLocalFile(new Path("./src/test/resources/BundleMapper/lib/"), new Path("/"));
      fileSystem
          .copyFromLocalFile(new Path("./src/test/resources/BundleMapper/lib2/"), new Path("/"));
      RemoteIterator<LocatedFileStatus> files = fileSystem.listFiles(new Path("/"), true);
      System.out.println("==============(BEFORE)==============");
      while (files.hasNext()) {
        LocatedFileStatus fileStat = files.next();
        System.out.println(fileStat.getPath().toString());
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to start cluster", e);
    }
  }

  @AfterClass
  public static void teardown() {
    try {
      RemoteIterator<LocatedFileStatus> files = fileSystem.listFiles(new Path("/"), true);
      System.out.println("==============(AFTER)==============");
      while (files.hasNext()) {
        LocatedFileStatus fileStat = files.next();
        System.out.println(fileStat.getPath().toString());
      }
    } catch (Exception e) {
    }
    component.stop();
    BundleClassLoaders.reset();
  }

  @Test
  public void testUnpackBundles() throws Exception {
    unpackBundles();
  }

  public void unpackBundles() throws Exception {
    // setup properties
    BundleProperties properties = loadSpecifiedProperties("/BundleMapper/conf/bundle.properties",
        EMPTY_MAP);
    // get the port we ended up with and set the paths
    String hdfsPrefix = configuration.get("fs.defaultFS");
    properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY, hdfsPrefix +"/lib/");
    properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY_PREFIX + "alt", hdfsPrefix + "/lib2/");
    FileSystemManager fileSystemManager = FileSystemManagerFactory.createFileSystemManager(new String[] {properties.getArchiveExtension()});
    ArrayList<Class> classes = new ArrayList<>();
    classes.add(MessageParser.class);
    // create a FileSystemManager
    Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
    ExtensionManager.init(classes, systemBundle, Collections.emptySet());

    final ExtensionMapping extensionMapping = BundleMapper
        .mapBundles(fileSystemManager,
             properties);

    assertNotNull(extensionMapping);
    assertEquals(2, extensionMapping.getAllExtensionNames().size());

    assertTrue(extensionMapping.getAllExtensionNames().keySet().contains(
        "org.apache.metron.bar.BarParser"));
    assertTrue(extensionMapping.getAllExtensionNames().keySet().contains(
        "org.apache.metron.foo.FooParser"));
  }
}