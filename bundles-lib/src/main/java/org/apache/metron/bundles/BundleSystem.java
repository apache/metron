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

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.bundle.Bundle;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.bundles.util.FileSystemManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High level interface to the Bundle System.  While you may want to use the lower level classes it
 * is not required, as BundleSystem provides the base required interface.
 */
public class BundleSystem {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Builder for a BundleSystem. only {@link BundleProperties} are required
   */
  public static class Builder {

    private BundleProperties properties;
    private FileSystemManager fileSystemManager;
    private List<Class> extensionClasses = new LinkedList<>();
    private Bundle systemBundle;

    public Builder withBundleProperties(BundleProperties properties) {
      this.properties = properties;
      return this;
    }

    public Builder withFileSystemManager(FileSystemManager fileSystemManager) {
      this.fileSystemManager = fileSystemManager;
      return this;
    }

    public Builder withExtensionClasses(List<Class> extensionClasses) {
      this.extensionClasses.addAll(extensionClasses);
      return this;
    }

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
        BundleClassLoaders.getInstance().init(fileSystemManager, libFileObjects, properties);
        ExtensionManager.getInstance()
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

  public <T> T createInstance(final String specificClassName, final Class<T> clazz)
      throws ClassNotFoundException, InstantiationException,
      NotInitializedException, IllegalAccessException {
    return BundleThreadContextClassLoader.createInstance(specificClassName, clazz, this.properties);
  }

}
