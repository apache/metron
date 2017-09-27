/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.test;


import org.apache.metron.bundles.BundleSystem;
import org.apache.metron.bundles.BundleSystemBuilder;
import org.apache.metron.bundles.util.BundleProperties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

// test that we can load a bundle and execute methods on the class
// note that we are not passing the class to the BundleSystemBuilder, but rather
// it is read from the properties
public class BundlesTestIntegrationTest {

  @Before
  public void before() {
    BundleSystem.reset();
  }

  @After
  public void after() {
    BundleSystem.reset();
  }

  @Test
  public void test() throws Exception {
    BundleProperties properties = BundleProperties
        .createBasicBundleProperties("src/test/resources/bundle.properties", null);

    properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY, "src/test/resources/");
    BundleSystem bundleSystem = new BundleSystemBuilder().withBundleProperties(properties)
        .build();
    TestInterface result = bundleSystem.createInstance("org.apache.test.TestImplementation",
        TestInterface.class);

    Assert.assertEquals("Foo", result.getFoo());
    Assert.assertEquals("Bar", result.getBar());
    Assert.assertEquals("Baz", result.getBaz());

  }

  @Test(expected = IllegalStateException.class)
  public void testUnknownClass() throws Exception {
    BundleProperties properties = BundleProperties
        .createBasicBundleProperties("src/test/resources/bundle.properties", null);

    properties.setProperty(BundleProperties.BUNDLE_LIBRARY_DIRECTORY, "src/test/resources/");
    BundleSystem bundleSystem = new BundleSystemBuilder().withBundleProperties(properties)
        .build();
    TestInterface result = bundleSystem.createInstance("org.apache.test.TestNonImplementation",
        TestInterface.class);
  }

}
