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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.VFSClassloaderUtil;
import org.junit.AfterClass;
import org.junit.Test;

public class BundleThreadContextClassLoaderTest {


    @AfterClass
    public static void after(){
        ExtensionClassInitializer.reset();
    }

    @Test
    public void validateWithPropertiesConstructor() throws Exception {
        BundleProperties properties = BundleProperties.createBasicBundleProperties("src/test/resources/bundle.properties", null);
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(AbstractFoo.class);
        ExtensionClassInitializer.initialize(classes);
        // create a FileSystemManager
        FileSystemManager fileSystemManager = VFSClassloaderUtil.generateVfs(properties.getArchiveExtension());
        Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
        ExtensionManager.discoverExtensions(systemBundle, Collections.emptySet());

        assertTrue(BundleThreadContextClassLoader.createInstance(WithPropertiesConstructor.class.getName(),
                WithPropertiesConstructor.class, properties) instanceof WithPropertiesConstructor);
    }

    @Test(expected = IllegalStateException.class)
    public void validateWithPropertiesConstructorInstantiationFailure() throws Exception {
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(AbstractFoo.class);
        ExtensionClassInitializer.initialize(classes);
        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put("fail", "true");
        BundleProperties properties = BundleProperties.createBasicBundleProperties("src/test/resources/bundle.properties", additionalProperties);
        // create a FileSystemManager
        FileSystemManager fileSystemManager = VFSClassloaderUtil.generateVfs(properties.getArchiveExtension());
        Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
        ExtensionManager.discoverExtensions(systemBundle, Collections.emptySet());

        BundleThreadContextClassLoader.createInstance(WithPropertiesConstructor.class.getName(), WithPropertiesConstructor.class, properties);
    }

    @Test
    public void validateWithDefaultConstructor() throws Exception {
        BundleProperties properties = BundleProperties.createBasicBundleProperties("src/test/resources/bundle.properties", null);
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(AbstractFoo.class);
        ExtensionClassInitializer.initialize(classes);
        // create a FileSystemManager
        FileSystemManager fileSystemManager = VFSClassloaderUtil.generateVfs(properties.getArchiveExtension());
        Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, properties);
        ExtensionManager.discoverExtensions(systemBundle, Collections.emptySet());

        assertTrue(BundleThreadContextClassLoader.createInstance(WithDefaultConstructor.class.getName(),
                WithDefaultConstructor.class, properties) instanceof WithDefaultConstructor);
    }

    @Test(expected = IllegalStateException.class)
    public void validateWithWrongConstructor() throws Exception {
        ExtensionClassInitializer.initialize(new ArrayList<>());
        BundleProperties properties = BundleProperties.createBasicBundleProperties("src/test/resources/bundle.properties", null);
        BundleThreadContextClassLoader.createInstance(WrongConstructor.class.getName(), WrongConstructor.class, properties);
    }

    public static class WithPropertiesConstructor  extends AbstractFoo{
        public WithPropertiesConstructor(){}
        public WithPropertiesConstructor(BundleProperties properties) {
            if (properties.getProperty("fail") != null) {
                throw new RuntimeException("Intentional failure");
            }
        }
    }

    public static class WithDefaultConstructor extends AbstractFoo{
        public WithDefaultConstructor() {

        }
    }

    public static class WrongConstructor extends AbstractFoo {
        public WrongConstructor(String s) {

        }
    }
}
