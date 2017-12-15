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
import java.net.URL;
import java.util.jar.Attributes;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileUtil;

/**
 * Helper class for VfsBundleClassLoader. This represents a resource loaded with the classloader.
 * This class is adapted from Apache Commons VFS Resource class v.2.1
 *
 * @see VfsBundleClassLoader
 */
class VfsBundleClassLoaderResource {

  private final FileObject root;
  private final FileObject resource;
  private final FileObject packageFolder;
  private final String packageName;

  /**
   * Creates a new instance.
   *
   * @param root The code source FileObject.
   * @param resource The resource of the FileObject.
   */
  public VfsBundleClassLoaderResource(final String name,
      final FileObject root,
      final FileObject resource)
      throws FileSystemException {
    this.root = root;
    this.resource = resource;
    packageFolder = resource.getParent();
    final int pos = name.lastIndexOf('/');
    if (pos == -1) {
      packageName = null;
    } else {
      packageName = name.substring(0, pos).replace('/', '.');
    }
  }

  /**
   * Returns the URL of the resource.
   */
  public URL getUrl() throws FileSystemException {
    return resource.getURL();
  }

  /**
   * Returns the name of the package containing the resource.
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * Returns an attribute of the package containing the resource.
   */
  public String getPackageAttribute(final Attributes.Name attrName) throws FileSystemException {
    return (String) packageFolder.getContent().getAttribute(attrName.toString());
  }

  /**
   * Returns the folder for the package containing the resource.
   */
  public FileObject getPackageFolder() {
    return packageFolder;
  }

  /**
   * Returns the FileObject of the resource.
   */
  public FileObject getFileObject() {
    return resource;
  }

  /**
   * Returns the code source as an URL.
   */
  public URL getCodeSourceUrl() throws FileSystemException {
    return root.getURL();
  }

  /**
   * Returns the data for this resource as a byte array.
   */
  public byte[] getBytes() throws IOException {
    return FileUtil.getContent(resource);
  }
}
