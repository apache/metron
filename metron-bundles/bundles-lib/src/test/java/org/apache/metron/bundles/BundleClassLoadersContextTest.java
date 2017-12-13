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

import static org.apache.metron.bundles.util.TestUtil.loadSpecifiedProperties;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileSystemManagerFactory;
import org.apache.metron.bundles.util.ResourceCopier;
import org.apache.metron.bundles.util.TestUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BundleClassLoadersContextTest {
  static final Map<String, String> EMPTY_MAP = new HashMap<String, String>();

  @AfterClass
  public static void after() {
    BundleClassLoaders.reset();
  }

  @Test
  public void merge() throws Exception {
    BundleProperties properties = loadSpecifiedProperties("/BundleMapper/conf/bundle.properties",
        EMPTY_MAP);

    assertEquals("src/test/resources/BundleMapper/lib/",
        properties.getProperty("bundle.library.directory"));
    assertEquals("src/test/resources/BundleMapper/lib2/",
        properties.getProperty("bundle.library.directory.alt"));

    String altLib = properties.getProperty("bundle.library.directory.alt");
    String lib = properties.getProperty("bundle.library.directory");
    properties.unSetProperty("bundle.library.directory.alt");

    FileSystemManager fileSystemManager = FileSystemManagerFactory
        .createFileSystemManager(new String[] {properties.getArchiveExtension()});

    BundleClassLoadersContext firstContext = new BundleClassLoadersContext.Builder().withFileSystemManager(fileSystemManager)
        .withExtensionDirs(TestUtil.getExtensionLibs(fileSystemManager,properties)).withBundleProperties(properties).build();

    Assert.assertEquals(1, firstContext.getBundles().size());
    for (Bundle thisBundle : firstContext.getBundles().values()) {
      Assert.assertEquals("org.apache.metron:metron-parser-bar-bundle:0.4.1",
          thisBundle.getBundleDetails().getCoordinates().getCoordinates());
    }

    // set the lib again so the utils will pickup the other directory
    properties.setProperty("bundle.library.directory", altLib);

    BundleClassLoadersContext secondContext = new BundleClassLoadersContext.Builder().withFileSystemManager(fileSystemManager)
        .withExtensionDirs(TestUtil.getExtensionLibs(fileSystemManager,properties)).withBundleProperties(properties).build();


    Assert.assertEquals(1, secondContext.getBundles().size());
    for (Bundle thisBundle : secondContext.getBundles().values()) {
      Assert.assertEquals("org.apache.metron:metron-parser-foo-bundle:0.4.1",
          thisBundle.getBundleDetails().getCoordinates().getCoordinates());
    }

    // ok merge together

    firstContext.merge(secondContext);
    Assert.assertEquals(2, firstContext.getBundles().size());
    for (Bundle thisBundle : firstContext.getBundles().values()) {
      Assert.assertTrue(
          thisBundle.getBundleDetails().getCoordinates().getCoordinates()
              .equals("org.apache.metron:metron-parser-bar-bundle:0.4.1")
              ||
              thisBundle.getBundleDetails().getCoordinates().getCoordinates()
                  .equals("org.apache.metron:metron-parser-foo-bundle:0.4.1")

      );
    }

    // merge a thirds, with duplicates
    // set both dirs
    properties.setProperty("bundle.library.directory.alt",lib);

    BundleClassLoadersContext thirdContext = new BundleClassLoadersContext.Builder().withFileSystemManager(fileSystemManager)
        .withExtensionDirs(TestUtil.getExtensionLibs(fileSystemManager,properties)).withBundleProperties(properties).build();

    Assert.assertEquals(2, thirdContext.getBundles().size());
    for (Bundle thisBundle : thirdContext.getBundles().values()) {
      Assert.assertTrue(
          thisBundle.getBundleDetails().getCoordinates().getCoordinates()
              .equals("org.apache.metron:metron-parser-bar-bundle:0.4.1")
              ||
              thisBundle.getBundleDetails().getCoordinates().getCoordinates()
                  .equals("org.apache.metron:metron-parser-foo-bundle:0.4.1")

      );
    }

    // merge them
    firstContext.merge(thirdContext);
    Assert.assertEquals(2, firstContext.getBundles().size());
    for (Bundle thisBundle : firstContext.getBundles().values()) {
      Assert.assertTrue(
          thisBundle.getBundleDetails().getCoordinates().getCoordinates()
              .equals("org.apache.metron:metron-parser-bar-bundle:0.4.1")
              ||
              thisBundle.getBundleDetails().getCoordinates().getCoordinates()
                  .equals("org.apache.metron:metron-parser-foo-bundle:0.4.1")

      );
    }
  }
}