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
package org.apache.metron.bundles.util;

import java.lang.invoke.MethodHandles;
import org.apache.accumulo.start.classloader.vfs.UniqueFileReplicator;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.cache.SoftRefFilesCache;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.impl.FileContentInfoFilenameFactory;
import org.apache.commons.vfs2.provider.hdfs.HdfsFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FileSystemManagerFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Create a FileSystem manager suitable for our purposes.
   * This manager supports files of the following types by default:
   * * jar
   * * HDFS
   * * file
   *
   * @return
   * @throws FileSystemException
   */
  public static FileSystemManager createFileSystemManager() throws FileSystemException {
    return createFileSystemManager(null);
  }

  /**
   * Create a FileSystem manager suitable for our purposes.
   * This manager supports files of the following types by default:
   * * jar
   * * HDFS
   * * file
   *
   * Other jar types can be supported through the jarExtensionToRegister parameter
   *
   * @param jarExtensionsToRegister Other extensions to jar compatible files
   * @return
   * @throws FileSystemException
   */
  public static FileSystemManager createFileSystemManager(String[] jarExtensionsToRegister) throws FileSystemException {
    DefaultFileSystemManager vfs = new DefaultFileSystemManager();

    if (jarExtensionsToRegister != null && jarExtensionsToRegister.length > 0) {
      for (String jarExtensionToRegister : jarExtensionsToRegister) {
        if (!StringUtils.isBlank(jarExtensionToRegister)) {
          vfs.addExtensionMap(jarExtensionToRegister, "jar");
          vfs.addProvider(jarExtensionToRegister,
              new org.apache.commons.vfs2.provider.jar.JarFileProvider());
        }
      }
    }

    vfs.addProvider("file", new org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider());
    vfs.addProvider("jar", new org.apache.commons.vfs2.provider.jar.JarFileProvider());
    vfs.addProvider("hdfs", new HdfsFileProvider());
    vfs.addExtensionMap("jar", "jar");

    vfs.setFileContentInfoFactory(new FileContentInfoFilenameFactory());
    vfs.setFilesCache(new SoftRefFilesCache());
    vfs.setReplicator(new UniqueFileReplicator(new File(System.getProperty("java.io.tmpdir"))));
    vfs.setCacheStrategy(CacheStrategy.ON_RESOLVE);
    vfs.init();
    return vfs;
  }
}
