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

import java.util.Arrays;
import org.apache.metron.bundles.BundleThreadContextClassLoaderTest.WithPropertiesConstructor;
import org.apache.metron.bundles.util.BundleProperties;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

public class BundleSystemTest {

  @AfterClass
  public static void after() {
    BundleClassLoaders.reset();
    ExtensionManager.reset();;
  }
  
  @Test
  public void createInstance() throws Exception {
    BundleProperties properties = BundleProperties
        .createBasicBundleProperties("src/test/resources/bundle.properties", null);

    properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY,"src/test/resources/BundleMapper/lib");
    BundleSystem bundleSystem = new BundleSystem.Builder().withBundleProperties(properties).withExtensionClasses(
        Arrays.asList(AbstractFoo.class)).build();
    Assert.assertTrue(
        bundleSystem.createInstance(WithPropertiesConstructor.class.getName(),
            WithPropertiesConstructor.class) instanceof WithPropertiesConstructor);

  }

  @Test(expected = IllegalArgumentException.class)
  public void createInstanceFail() throws Exception {
    BundleSystem bundleSystem = new BundleSystem.Builder().build();
  }

}