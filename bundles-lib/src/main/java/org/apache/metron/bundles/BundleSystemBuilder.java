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
 * Builder for a BundleSystem. only {@link BundleProperties} are required. Beyond that, the
 * BundleProperties, if they are the only parameter must have archive extension and bundle
 * extension types properties present.
 *
 * The {@link BundleSystemType} determines if a type other than the default should be created.
 *
 */
public class BundleSystemBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private BundleProperties properties;
  private FileSystemManager fileSystemManager;
  private List<Class> extensionClasses = new LinkedList<>();
  private Bundle systemBundle;
  private BundleSystemType bundleSystemType = BundleSystemType.DEFAULT;

  /**
   * The BundleProperties to use.  Unless other builder parameters override options
   * (withExtensionClasses ), they must have archive extension and bundle extensions types
   * specified
   *
   * @param properties The BundleProperties
   * @return BundleSystemBuilder
   */
  public BundleSystemBuilder withBundleProperties(BundleProperties properties) {
    this.properties = properties;
    return this;
  }

  /**
   * Provide a {@link FileSystemManager} to overide the default
   *
   * @param fileSystemManager override
   * @return BundleSystemBuilder
   */
  public BundleSystemBuilder withFileSystemManager(FileSystemManager fileSystemManager) {
    this.fileSystemManager = fileSystemManager;
    return this;
  }

  /**
   * Provide Extension Classes.  If not provided with this override then the classes will be
   * configured from the BundleProperties. If provided, the properties file will not be used for
   * classes.
   *
   * @param extensionClasses override
   * @return BundleSystemBuilder
   */
  public BundleSystemBuilder withExtensionClasses(List<Class> extensionClasses) {
    this.extensionClasses.addAll(extensionClasses);
    return this;
  }

  /**
   * Provide a SystemBundle.  If not provided with this override then the default SystemBundle
   * will be created.
   */
  public BundleSystemBuilder withSystemBundle(Bundle systemBundle) {
    this.systemBundle = systemBundle;
    return this;
  }

  /**
   * Provide the requested BundleSystemType
   * @param bundleSystemType the type to build
   * @return BundleSystemBuilder
   */
  public BundleSystemBuilder withBundleSystemType(BundleSystemType bundleSystemType) {
    this.bundleSystemType = bundleSystemType;
    return this;
  }

  /**
   * Creates a BundleSystemBuilder using another builder's settings
   * @param otherBuilder BundleSystemBuilder to 'copy'
   * @return BundleSystemBuilder
   */
  public BundleSystemBuilder withOtherBuilder(BundleSystemBuilder otherBuilder){
    this.bundleSystemType = otherBuilder.bundleSystemType;
    this.systemBundle = otherBuilder.systemBundle;
    this.extensionClasses.addAll(otherBuilder.extensionClasses);
    this.properties = otherBuilder.properties;
    return this;
  }

  /**
   * Builds a new BundleSystem.
   *
   * @return BundleSystem
   * @throws NotInitializedException if any errors happen during build
   */
  public BundleSystem build() throws NotInitializedException {
    if(bundleSystemType == BundleSystemType.ON_DEMAND) {
      return buildOnDemand();
    }
    return buildDefault();
  }

  private BundleSystem buildOnDemand() throws NotInitializedException {
    return new OnDemandBundleSystem(this);
  }

  private BundleSystem buildDefault() throws NotInitializedException {
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
      libDirs.forEach((uri) -> LOG.debug(uri.toString()));
      List<FileObject> libFileObjects = new ArrayList<>();
      libDirs.forEach((x) -> {
        try {
          FileObject fileObject = fileSystemManager.resolveFile(x);
          if (fileObject.exists()) {
            if(libFileObjects.contains(fileObject) == false) {
              libFileObjects.add(fileObject);
            }
          }
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      });

      // initialize the Bundle System
      BundleClassLoaders.init(fileSystemManager, libFileObjects, properties);
      ExtensionManager
          .init(extensionClasses, systemBundle, BundleClassLoaders.getInstance().getBundles());
      return new DefaultBundleSystem(fileSystemManager, extensionClasses, libFileObjects, systemBundle, properties);
    } catch (Exception e) {
      throw new NotInitializedException(e);
    }
  }
}
