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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.BundleThreadContextClassLoaderTest.WithPropertiesConstructor;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileSystemManagerFactory;
import org.apache.metron.bundles.util.ResourceCopier;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BundleSystemTest {

  @AfterClass
  public static void after() {
    BundleClassLoaders.reset();
    ExtensionManager.reset();
    File t = new File("target/BundleMapper/lib/metron-parser-foo-bundle-0.4.1.bundle");
    if (t.exists()) {
      t.delete();
    }
  }

  @After
  public void afterTest() {
    ExtensionManager.reset();
    BundleClassLoaders.reset();
  }

  @BeforeClass
  public static void copyResources() throws IOException {
    ResourceCopier.copyResources(Paths.get("./src/test/resources"), Paths.get("./target"));
  }

  @Test
  public void createInstance() throws Exception {
    BundleProperties properties = BundleProperties
        .createBasicBundleProperties("target/bundle.properties", null);

    properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY, "target/BundleMapper/lib");
    BundleSystem bundleSystem = new BundleSystem.Builder().withBundleProperties(properties)
        .withExtensionClasses(
            Arrays.asList(AbstractFoo.class)).build();
    Assert.assertTrue(
        bundleSystem.createInstance(WithPropertiesConstructor.class.getName(),
            WithPropertiesConstructor.class) instanceof WithPropertiesConstructor);

  }

  @Test(expected = IllegalArgumentException.class)
  public void createInstanceFail() throws Exception {
    BundleSystem bundleSystem = new BundleSystem.Builder().build();
  }

  @Test
  public void testAddBundle() throws Exception {
    BundleProperties properties = BundleProperties
        .createBasicBundleProperties("target/bundle.properties", null);

    properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY,
        "target/BundleMapper/lib");
    File f = new File("target/BundleMapper/metron-parser-foo-bundle-0.4.1.bundle");
    File t = new File(
        "target/BundleMapper/lib/metron-parser-foo-bundle-0.4.1.bundle");
    if (t.exists()) {
      t.delete();
    }
    FileSystemManager fileSystemManager = FileSystemManagerFactory
        .createFileSystemManager(new String[]{properties.getArchiveExtension()});
    BundleSystem bundleSystem = new BundleSystem.Builder()
        .withFileSystemManager(fileSystemManager)
        .withBundleProperties(properties).withExtensionClasses(
            Arrays.asList(AbstractFoo.class, MessageParser.class)).build();
    Assert.assertTrue(
        bundleSystem.createInstance(WithPropertiesConstructor.class.getName(),
            WithPropertiesConstructor.class) instanceof WithPropertiesConstructor);
    // copy the file into bundles library
    FileUtils.copyFile(f, t);
    Assert.assertEquals(1, BundleClassLoaders.getInstance().getBundles().size());
    for (Bundle thisBundle : BundleClassLoaders.getInstance().getBundles()) {
      Assert.assertTrue(
          thisBundle.getBundleDetails().getCoordinates().getCoordinates()
              .equals("org.apache.metron:metron-parser-bar-bundle:0.4.1")
      );
    }
    bundleSystem.addBundle("metron-parser-foo-bundle-0.4.1.bundle");

    Assert.assertEquals(2, BundleClassLoaders.getInstance().getBundles().size());
    for (Bundle thisBundle : BundleClassLoaders.getInstance().getBundles()) {
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
