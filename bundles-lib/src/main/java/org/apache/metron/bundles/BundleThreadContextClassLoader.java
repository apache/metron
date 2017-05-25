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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;

/**
 * THREAD SAFE
 */
public class BundleThreadContextClassLoader extends URLClassLoader {

    static final ContextSecurityManager contextSecurityManager = new ContextSecurityManager();
    private final ClassLoader forward = ClassLoader.getSystemClassLoader();
    private static final List<Class<?>> bundleSpecificClasses = new ArrayList<>();
    private static AtomicBoolean inited = new AtomicBoolean(false);
    // should initialize class definitions
    public static void initClasses(final List<Class> classes){
        bundleSpecificClasses.clear();
        if(classes != null){
            for( Class clazz : classes){
                bundleSpecificClasses.add(clazz);
            }
        }
        inited.set(true);
    }

    public static void resetClasses(){
        bundleSpecificClasses.clear();
        inited.set(false);
    }

    private BundleThreadContextClassLoader() {
        super(new URL[0]);
    }

    @Override
    public void clearAssertionStatus() {
        lookupClassLoader().clearAssertionStatus();
    }

    @Override
    public URL getResource(String name) {
        return lookupClassLoader().getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return lookupClassLoader().getResourceAsStream(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return lookupClassLoader().getResources(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return lookupClassLoader().loadClass(name);
    }

    @Override
    public void setClassAssertionStatus(String className, boolean enabled) {
        lookupClassLoader().setClassAssertionStatus(className, enabled);
    }

    @Override
    public void setDefaultAssertionStatus(boolean enabled) {
        lookupClassLoader().setDefaultAssertionStatus(enabled);
    }

    @Override
    public void setPackageAssertionStatus(String packageName, boolean enabled) {
        lookupClassLoader().setPackageAssertionStatus(packageName, enabled);
    }

    private ClassLoader lookupClassLoader() {
        final Class<?>[] classStack = contextSecurityManager.getExecutionStack();

        for (Class<?> currentClass : classStack) {
            final Class<?> bundleClass = findBundleClass(currentClass);
            if (bundleClass != null) {
                final ClassLoader desiredClassLoader = bundleClass.getClassLoader();

                // When new Threads are created, the new Thread inherits the ClassLoaderContext of
                // the caller. However, the call stack of that new Thread may not trace back to any app-specific
                // code. Therefore, the BundleThreadContextClassLoader will be unable to find the appropriate Bundle
                // ClassLoader. As a result, we want to set the ContextClassLoader to the Bundel ClassLoader that
                // contains the class or resource that we are looking for.
                // This locks the current Thread into the appropriate Bundle ClassLoader Context. The framework will change
                // the ContextClassLoader back to the BundleThreadContextClassLoader as appropriate via the
                // OPF NEEDS CLARIFICATION
                // {@link FlowEngine.beforeExecute(Thread, Runnable)} and
                // {@link FlowEngine.afterExecute(Thread, Runnable)} methods.
                if (desiredClassLoader instanceof BundleClassLoader) {
                    Thread.currentThread().setContextClassLoader(desiredClassLoader);
                }
                return desiredClassLoader;
            }
        }
        return forward;
    }

    private Class<?> findBundleClass(final Class<?> cls) {
        for (final Class<?> bundleClass : bundleSpecificClasses) {
            if (bundleClass.isAssignableFrom(cls)) {
                return cls;
            } else if (cls.getEnclosingClass() != null) {
                return findBundleClass(cls.getEnclosingClass());
            }
        }

        return null;
    }

    private static class SingletonHolder {

        public static final BundleThreadContextClassLoader instance = new BundleThreadContextClassLoader();
    }

    public static BundleThreadContextClassLoader getInstance() throws NotInitializedException {
        if(!inited.get()){
            throw new NotInitializedException();
        }
        return SingletonHolder.instance;
    }

    static class ContextSecurityManager extends SecurityManager {

        Class<?>[] getExecutionStack() {
            return getClassContext();
        }
    }

    /**
     * Constructs an instance of the given type using either default no args
     * constructor or a constructor which takes a BundleProperties object
     * (preferred).
     *
     * @param <T> the type to create an instance for
     * @param implementationClassName the implementation class name
     * @param typeDefinition the type definition
     * @param bundleProperties the BundleProperties instance
     * @return constructed instance
     * @throws InstantiationException if there is an error instantiating the class
     * @throws IllegalAccessException if there is an error accessing the type
     * @throws ClassNotFoundException if the class cannot be found
     */
    public static <T> T createInstance(final String implementationClassName, final Class<T> typeDefinition, final BundleProperties bundleProperties)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, NotInitializedException {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(BundleThreadContextClassLoader.getInstance());
        try {
            final List<Bundle> bundles = ExtensionManager.getBundles(implementationClassName);
            if (bundles.size() == 0) {
                throw new IllegalStateException(String.format("The specified implementation class '%s' is not known to this nifi.", implementationClassName));
            }
            if (bundles.size() > 1) {
                throw new IllegalStateException(String.format("More than one bundle was found for the specified implementation class '%s', only one is allowed.", implementationClassName));
            }

            final Bundle bundle = bundles.get(0);
            final ClassLoader detectedClassLoaderForType = bundle.getClassLoader();
            final Class<?> rawClass = Class.forName(implementationClassName, true, detectedClassLoaderForType);

            Thread.currentThread().setContextClassLoader(detectedClassLoaderForType);
            final Class<?> desiredClass = rawClass.asSubclass(typeDefinition);
            if(bundleProperties == null){
                return typeDefinition.cast(desiredClass.newInstance());
            }
            Constructor<?> constructor = null;

            try {
                constructor = desiredClass.getConstructor(BundleProperties.class);
            } catch (NoSuchMethodException nsme) {
                try {
                    constructor = desiredClass.getConstructor();
                } catch (NoSuchMethodException nsme2) {
                    throw new IllegalStateException("Failed to find constructor which takes BundleProperties as argument as well as the default constructor on "
                            + desiredClass.getName(), nsme2);
                }
            }
            try {
                if (constructor.getParameterTypes().length == 0) {
                    return typeDefinition.cast(constructor.newInstance());
                } else {
                    return typeDefinition.cast(constructor.newInstance(bundleProperties));
                }
            } catch (InvocationTargetException ite) {
                throw new IllegalStateException("Failed to instantiate a component due to (see target exception)", ite);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
