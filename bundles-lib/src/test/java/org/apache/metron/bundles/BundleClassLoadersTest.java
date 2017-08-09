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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

public class BundleClassLoadersTest {

  static final Map<String, String> EMPTY_MAP = new HashMap<String, String>();

  @AfterClass
  public static void after() {
    ExtensionClassInitializer.reset();
    BundleClassLoaders.reset();
  }

  @BeforeClass
  public static void copyResources() throws IOException {
    ResourceCopier.copyResources(Paths.get("./src/test/resources"), Paths.get("./target"));
  }

  @Test
  public void testReset() throws Exception {
    // this test is to ensure that we can
    // reset the BundleClassLoaders and initialize
    // a second time
    BundleProperties properties = loadSpecifiedProperties("/BundleMapper/conf/bundle.properties",
        EMPTY_MAP);

    assertEquals("./target/BundleMapper/lib/",
        properties.getProperty("bundle.library.directory"));
    assertEquals("./target/BundleMapper/lib2/",
        properties.getProperty("bundle.library.directory.alt"));

    String altLib = properties.getProperty("bundle.library.directory.alt");
    properties.unSetProperty("bundle.library.directory.alt");

    FileSystemManager fileSystemManager = FileSystemManagerFactory
        .createFileSystemManager(new String[] {properties.getArchiveExtension()});

    BundleClassLoaders.getInstance()
        .init(fileSystemManager, TestUtil.getExtensionLibs(fileSystemManager, properties),
            properties);

    Set<Bundle> bundles = BundleClassLoaders.getInstance().getBundles();

    Assert.assertEquals(1, bundles.size());
    for (Bundle thisBundle : bundles) {
      Assert.assertEquals("org.apache.metron:metron-parser-bar-bundle:0.4.1",
          thisBundle.getBundleDetails().getCoordinates().getCoordinates());
    }



    properties.setProperty("bundle.library.directory", altLib);
    boolean thrown = false;
    try {
      BundleClassLoaders.getInstance()
          .init(fileSystemManager, TestUtil.getExtensionLibs(fileSystemManager, properties),
              properties);
    } catch (IllegalStateException ise){
      thrown = true;
    }
    Assert.assertTrue(thrown);

    BundleClassLoaders.reset();

    BundleClassLoaders.getInstance()
        .init(fileSystemManager, TestUtil.getExtensionLibs(fileSystemManager, properties),
            properties);

    bundles = BundleClassLoaders.getInstance().getBundles();

    Assert.assertEquals(1, bundles.size());
    for (Bundle thisBundle : bundles) {
      Assert.assertEquals("org.apache.metron:metron-parser-foo-bundle:0.4.1",
          thisBundle.getBundleDetails().getCoordinates().getCoordinates());
    }
  }
}