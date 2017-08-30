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

import java.io.Closeable;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class BundleCloseable implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static BundleCloseable withBundleLoader() throws NotInitializedException{
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(BundleThreadContextClassLoader.getInstance());
        return new BundleCloseable(current);
    }

    /**
     * Sets the current thread context class loader to the specific appropriate class loader for the given
     * component. If the component requires per-instance class loading then the class loader will be the
     * specific class loader for instance with the given identifier, otherwise the class loader will be
     * the BundleClassLoader.
     *
     * @param componentClass the component class
     * @param componentIdentifier the identifier of the component
     * @return BundleCloseable with the current thread context classloader jailed to the Bundle
     *              or instance class loader of the component
     */
    public static BundleCloseable withComponentBundleLoader(final Class componentClass, final String componentIdentifier) throws NotInitializedException{
        final ClassLoader current = Thread.currentThread().getContextClassLoader();

        ClassLoader componentClassLoader = ExtensionManager.getInstance().getInstanceClassLoader(componentIdentifier);
        if (componentClassLoader == null) {
            componentClassLoader = componentClass.getClassLoader();
        }

        Thread.currentThread().setContextClassLoader(componentClassLoader);
        return new BundleCloseable(current);
    }

    /**
     * Sets the current thread context class loader to the provided class loader, and returns a BundleCloseable that will
     * return the current thread context class loader to it's previous state.
     *
     * @param componentBundleLoader the class loader to set as the current thread context class loader
     *
     * @return BundleCloseable that will return the current thread context class loader to its previous state
     */
    public static BundleCloseable withComponentBundleLoader(final ClassLoader componentBundleLoader) {
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(componentBundleLoader);
        return new BundleCloseable(current);
    }
    private final ClassLoader toSet;

    private BundleCloseable(final ClassLoader toSet) {
        this.toSet = toSet;
    }

    @Override
    public void close() {
        if (toSet != null) {
            Thread.currentThread().setContextClassLoader(toSet);
        }
    }
}
