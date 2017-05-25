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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.impl.VFSClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A <tt>ClassLoader</tt> for loading BUNDLES (plugin archives). BUNDLEs are designed to
 * allow isolating bundles of code (comprising one-or-more
 * plugin classes and their
 * dependencies) from other such bundles; this allows for dependencies and
 * processors that require conflicting, incompatible versions of the same
 * dependency to run in a single instance of a given process.</p>
 *
 * <p>
 * <tt>BundleClassLoader</tt> follows the delegation model described in
 * {@link ClassLoader#findClass(java.lang.String) ClassLoader.findClass(...)};
 * classes are first loaded from the parent <tt>ClassLoader</tt>, and only if
 * they cannot be found there does the <tt>BundleClassLoader</tt> provide a
 * definition. Specifically, this means that resources are loaded from the application's
 * <tt>conf</tt>
 * and <tt>lib</tt> directories first, and if they cannot be found there, are
 * loaded from the BUNDLE.</p>
 *
 * <p>
 * The packaging of a BUNDLE is such that it is a ZIP file with the following
 * directory structure:
 *
 * <pre>
 *   +META-INF/
 *   +-- bundled-dependencies/
 *   +-- &lt;JAR files&gt;
 *   +-- MANIFEST.MF
 * </pre>
 * </p>
 *
 * <p>
 * The MANIFEST.MF file contains the same information as a typical JAR file but
 * also includes two additional bundle properties: {@code Bundle-Id} and
 * {@code Bundle-Dependency-Id}.
 * </p>
 *
 * <p>
 * The {@code Bundle-Id} provides a unique identifier for this BUNDLE.
 * </p>
 *
 * <p>
 * The {@code Bundle-Dependency-Id} is optional. If provided, it indicates that
 * this BUNDLE should inherit all of the dependencies of the BUNDLE with the provided
 * ID. Often times, the BUNDLE that is depended upon is referred to as the Parent.
 * This is because its ClassLoader will be the parent ClassLoader of the
 * dependent BUNDLE.
 * </p>
 *
 * <p>
 * If a BUNDLE is built using the Bundles Maven Plugin, the {@code Bundle-Id} property
 * will be set to the artifactId of the BUNDLE. The {@code Bundle-Dependency-Id} will
 * be set to the artifactId of the BUNDLE that is depended upon. For example, if
 * BUNDLE A is defined as such:
 *
 * <pre>
 * ...
 * &lt;artifactId&gt;bundle-a&lt;/artifactId&gt;
 * &lt;packaging&gt;bundle&lt;/packaging&gt;
 * ...
 * &lt;dependencies&gt;
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;group&lt;/groupId&gt;
 *     &lt;artifactId&gt;bundle-z&lt;/artifactId&gt;
 *     <b>&lt;type&gt;bundle&lt;/type&gt;</b>
 *   &lt;/dependency&gt;
 * &lt;/dependencies&gt;
 * </pre>
 * </p>
 *
 *
 * <p>
 * Then the MANIFEST.MF file that is created for Bundle A will have the following
 * properties set:
 * <ul>
 * <li>{@code {Foo}-Id: bundle-a}</li>
 * <li>{@code {Foo}-Dependency-Id: bundle-z}</li>
 * </ul>
 * Where is configurable by BundleProperty META_ID_PREFIX [ default Bundle ]
 * </p>
 *
 * <p>
 * Note, above, that the {@code type} of the dependency is set to {@code foo}.
 * </p>
 *
 * <p>
 * If the Bundle has more than one dependency of {@code type} {@code Foo}, then the
 * Maven Bundle plugin will fail to build the Bundle.
 * </p>
 */
public class BundleClassLoader extends VFSClassLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleClassLoader.class);

    public static class Builder {

        private FileSystemManager fileSystemManager;
        private FileObject bundleWorkingDirectory;
        private FileObject[] existingPaths;
        private ClassLoader parentClassLoader;

        public Builder withFileSystemManager(FileSystemManager fileSystemManager) {
            this.fileSystemManager = fileSystemManager;
            return this;
        }

        public Builder withBundleWorkingDirectory(FileObject bundleWorkingDirectory) {
            this.bundleWorkingDirectory = bundleWorkingDirectory;
            return this;
        }

        public Builder withParentClassloader(ClassLoader parentClassloader){
            this.parentClassLoader = parentClassloader;
            return this;
        }

        public Builder withClassPaths(FileObject[] paths){
            this.existingPaths = paths;
            return this;
        }

        private FileObject[] updateClasspath(FileObject root, FileObject[] otherPaths) throws FileSystemException{
            final List<FileObject> paths = new ArrayList<>();
            // for compiled classes, META-INF/, etc.
            paths.add(root);

            if(otherPaths != null && otherPaths.length > 0){
                for ( FileObject path : otherPaths) {
                    paths.add(path);
                }
            }

            FileObject dependencies = root.resolveFile("META-INF/bundled-dependencies");
            if (!dependencies.isFolder()) {
                LOGGER.warn(bundleWorkingDirectory + " does not contain META-INF/bundled-dependencies!");
            }
            paths.add(dependencies);
            if (dependencies.isFolder()) {
                for (FileObject libJar : dependencies.findFiles(JAR_FILTER)) {
                    paths.add(libJar);
                }
            }
            return paths.toArray(new FileObject[0]);
        }
        public BundleClassLoader build() throws FileSystemException{
            FileObject[] paths = updateClasspath(bundleWorkingDirectory,existingPaths);
            return new BundleClassLoader(fileSystemManager, bundleWorkingDirectory, paths, parentClassLoader);
        }
    }

    private static final FileSelector JAR_FILTER = new FileSelector() {
        @Override
        public boolean includeFile(FileSelectInfo fileSelectInfo) throws Exception {
            final String nameToTest = fileSelectInfo.getFile().getName().getExtension();
            return nameToTest.equals("jar") && fileSelectInfo.getFile().isFile();
        }

        @Override
        public boolean traverseDescendents(FileSelectInfo fileSelectInfo) throws Exception {
            return true;
        }
    };

    /**
     * The BUNDLE for which this <tt>ClassLoader</tt> is responsible.
     */
    private final FileObject bundleWorkingDirectory;

    /**
     * Construct a bundle class loader with the specific parent.
     *
     * @param bundleWorkingDirectory directory to explode bundle contents to
     * @param parentClassLoader parent class loader of this bundle
     * @throws IllegalArgumentException if the bundle is missing the Java Services
     * API file for implementations.
     * @throws ClassNotFoundException if any of the
     * implementations defined by the Java Services API cannot be loaded.
     * @throws FileSystemException if an error occurs while loading the BUNDLE.
     */
    private BundleClassLoader(final FileSystemManager fileSystemManager, final FileObject bundleWorkingDirectory, final FileObject[] classPaths, final ClassLoader parentClassLoader) throws FileSystemException {
        super(classPaths,fileSystemManager,parentClassLoader);
        this.bundleWorkingDirectory = bundleWorkingDirectory;
    }

    public FileObject getWorkingDirectory() {
        return bundleWorkingDirectory;
    }


    @Override
    protected String findLibrary(final String libname) {
        try {
            FileObject dependencies = bundleWorkingDirectory.resolveFile("META-INF/bundled-dependencies");
            if (!dependencies.isFolder()) {
                LOGGER.warn(bundleWorkingDirectory + " does not contain META-INF/bundled-dependencies!");
            }

            final FileObject nativeDir = dependencies.resolveFile("native");
            final FileObject libsoFile = nativeDir.resolveFile( "lib" + libname + ".so");
            final FileObject dllFile = nativeDir.resolveFile(libname + ".dll");
            final FileObject soFile = nativeDir.resolveFile(libname + ".so");
            if (libsoFile.exists()) {
                return libsoFile.getURL().toString();
            } else if (dllFile.exists()) {
                return dllFile.getURL().toString();
            } else if (soFile.exists()) {
                return soFile.getURL().toString();
            }
        }catch(FileSystemException fse){
            LOGGER.error("Failed to get dependencies",fse);
        }

        // not found in the bundle. try system native dir
        return null;
    }

    @Override
    public String toString() {
        return BundleClassLoader.class.getName() + "[" + bundleWorkingDirectory.getName().toString() + "]";
    }
}
