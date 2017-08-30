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

import com.google.common.annotations.VisibleForTesting;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileSystemManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High level interface to the Bundle System.  While you may want to use the lower level classes it
 * is not required, as BundleSystem provides the base required interface for initializing the system
 * and instantiating classes
 */
public class BundleSystem {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Builder for a BundleSystem. only {@link BundleProperties} are required. Beyond that, the
   * BundleProperties, if they are the only parameter must have archive extension and bundle
   * extension types properties present.
   */
  public static class Builder {

    private BundleProperties properties;
    private FileSystemManager fileSystemManager;
    private List<Class> extensionClasses = new LinkedList<>();
    private Bundle systemBundle;

    /**
     * The BundleProperties to use.  Unless other builder parameters override options
     * (withExtensionClasses ), they must have archive extension and bundle extensions types
     * specified
     *
     * @param properties The BundleProperties
     * @return Builder
     */
    public Builder withBundleProperties(BundleProperties properties) {
      this.properties = properties;
      return this;
    }

    /**
     * Provide a {@link FileSystemManager} to overide the default
     *
     * @param fileSystemManager override
     * @return Builder
     */
    public Builder withFileSystemManager(FileSystemManager fileSystemManager) {
      this.fileSystemManager = fileSystemManager;
      return this;
    }

    /**
     * Provide Extension Classes.  If not provided with this override then the classes will be
     * configured from the BundleProperties. If provided, the properties file will not be used for
     * classes.
     *
     * @param extensionClasses override
     * @return Builder
     */
    public Builder withExtensionClasses(List<Class> extensionClasses) {
      this.extensionClasses.addAll(extensionClasses);
      return this;
    }

    /**
     * Provide a SystemBundle.  If not provided with this override then the default SystemBundle
     * will be created.
     */
    public Builder withSystemBundle(Bundle systemBundle) {
      this.systemBundle = systemBundle;
      return this;
    }

    /**
     * Builds a new BundleSystem.
     *
     * @return BundleSystem
     * @throws NotInitializedException if any errors happen during build
     */
    public BundleSystem build() throws NotInitializedException {
      if (this.properties == null) {
        throw new IllegalArgumentException("BundleProperties are required");
      }
      try {
        if (this.fileSystemManager == null) {
          this.fileSystemManager = FileSystemManagerFactory
              .createFileSystemManager(new String[]{properties.getArchiveExtension()});
        }
        if (this.extensionClasses.isEmpty()) {
          properties.getBundleExtensionTypes().forEach((x, y) -> {
            try {
              this.extensionClasses.add(Class.forName(y));
            } catch (ClassNotFoundException e) {
              throw new IllegalStateException(e);
            }
          });
        }
        if (this.systemBundle == null) {
          this.systemBundle = ExtensionManager
              .createSystemBundle(this.fileSystemManager, this.properties);
        }
        List<URI> libDirs = properties.getBundleLibraryDirectories();
        List<FileObject> libFileObjects = new ArrayList<>();
        libDirs.forEach((x) -> {
          try {
            FileObject fileObject = fileSystemManager.resolveFile(x);
            if (fileObject.exists()) {
              libFileObjects.add(fileObject);
            }
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });

        // initialize the Bundle System
        BundleClassLoaders.init(fileSystemManager, libFileObjects, properties);
        ExtensionManager
            .init(extensionClasses, systemBundle, BundleClassLoaders.getInstance().getBundles());
        return new BundleSystem(fileSystemManager, extensionClasses, systemBundle, properties);
      } catch (Exception e) {
        throw new NotInitializedException(e);
      }
    }


  }

  private final BundleProperties properties;
  private final FileSystemManager fileSystemManager;
  private final List<Class> extensionClasses;
  private final Bundle systemBundle;

  private BundleSystem(FileSystemManager fileSystemManager, List<Class> extensionClasses,
      Bundle systemBundle, BundleProperties properties) {
    this.properties = properties;
    this.fileSystemManager = fileSystemManager;
    this.extensionClasses = extensionClasses;
    this.systemBundle = systemBundle;
  }

  /**
   * Constructs an instance of the given type using either default no args constructor or a
   * constructor which takes a BundleProperties object.
   *
   * @param specificClassName the implementation class name
   * @param clazz the type (T) to create an instance for
   * @return an instance of specificClassName which extends T
   * @throws ClassNotFoundException if the class cannot be found
   * @throws InstantiationException if the class cannot be instantiated
   */
  public <T> T createInstance(final String specificClassName, final Class<T> clazz)
      throws ClassNotFoundException, InstantiationException,
      NotInitializedException, IllegalAccessException {
    return BundleThreadContextClassLoader.createInstance(specificClassName, clazz, this.properties);
  }

  @SuppressWarnings("unchecked")
  public <T> Set<Class<? extends T>> getExtensionsClassesForExtensionType(final Class<T> extensionType)
      throws NotInitializedException {
    Set<Class<? extends T>> set = new HashSet<Class<? extends T>>();
    ExtensionManager.getInstance().getExtensions(extensionType).forEach((x) -> {
      set.add((Class<T>)x);
    });
    return set;
  }

  @VisibleForTesting()
  public static void reset() {
    BundleClassLoaders.reset();
    ExtensionManager.reset();
  }

}
