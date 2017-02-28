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
package org.apache.metron.common.utils;

import org.apache.accumulo.start.classloader.vfs.UniqueFileReplicator;
import org.apache.commons.logging.Log;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.cache.SoftRefFilesCache;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.impl.FileContentInfoFilenameFactory;
import org.apache.commons.vfs2.impl.VFSClassLoader;
import org.apache.commons.vfs2.provider.FileReplicator;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponent;
import org.apache.commons.vfs2.provider.VfsComponentContext;
import org.apache.commons.vfs2.provider.hdfs.HdfsFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClassloaderUtil {
  private static final Logger LOG = LoggerFactory.getLogger(ClassloaderUtil.class);

  public enum Config {
    VFS_PATHS("classloader.paths", "")
    ;
    String param;
    Object defaultValue;
    Config(String param, String defaultValue) {
      this.param = param;
      this.defaultValue = defaultValue;
    }

    public String param() {
      return param;
    }

    public Object get(Map<String, Object> config) {
      return config.getOrDefault(param, defaultValue);
    }

    public <T> T get(Map<String, Object> config, Class<T> clazz) {
      return ConversionUtils.convert(get(config), clazz);
    }
  }

  public static FileSystemManager generateVfs() throws FileSystemException {
    DefaultFileSystemManager vfs = new DefaultFileSystemManager();
    vfs.addProvider("res", new org.apache.commons.vfs2.provider.res.ResourceFileProvider());
    vfs.addProvider("zip", new org.apache.commons.vfs2.provider.zip.ZipFileProvider());
    vfs.addProvider("gz", new org.apache.commons.vfs2.provider.gzip.GzipFileProvider());
    vfs.addProvider("ram", new org.apache.commons.vfs2.provider.ram.RamFileProvider());
    vfs.addProvider("file", new org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider());
    vfs.addProvider("jar", new org.apache.commons.vfs2.provider.jar.JarFileProvider());
    vfs.addProvider("http", new org.apache.commons.vfs2.provider.http.HttpFileProvider());
    vfs.addProvider("https", new org.apache.commons.vfs2.provider.https.HttpsFileProvider());
    vfs.addProvider("ftp", new org.apache.commons.vfs2.provider.ftp.FtpFileProvider());
    vfs.addProvider("ftps", new org.apache.commons.vfs2.provider.ftps.FtpsFileProvider());
    vfs.addProvider("war", new org.apache.commons.vfs2.provider.jar.JarFileProvider());
    vfs.addProvider("par", new org.apache.commons.vfs2.provider.jar.JarFileProvider());
    vfs.addProvider("ear", new org.apache.commons.vfs2.provider.jar.JarFileProvider());
    vfs.addProvider("sar", new org.apache.commons.vfs2.provider.jar.JarFileProvider());
    vfs.addProvider("ejb3", new org.apache.commons.vfs2.provider.jar.JarFileProvider());
    vfs.addProvider("tmp", new org.apache.commons.vfs2.provider.temp.TemporaryFileProvider());
    vfs.addProvider("tar", new org.apache.commons.vfs2.provider.tar.TarFileProvider());
    vfs.addProvider("tbz2", new org.apache.commons.vfs2.provider.tar.TarFileProvider());
    vfs.addProvider("tgz", new org.apache.commons.vfs2.provider.tar.TarFileProvider());
    vfs.addProvider("bz2", new org.apache.commons.vfs2.provider.bzip2.Bzip2FileProvider());
    vfs.addProvider("hdfs", new HdfsFileProvider());
    vfs.addExtensionMap("jar", "jar");
    vfs.addExtensionMap("zip", "zip");
    vfs.addExtensionMap("gz", "gz");
    vfs.addExtensionMap("tar", "tar");
    vfs.addExtensionMap("tbz2", "tar");
    vfs.addExtensionMap("tgz", "tar");
    vfs.addExtensionMap("bz2", "bz2");
    vfs.addMimeTypeMap("application/x-tar", "tar");
    vfs.addMimeTypeMap("application/x-gzip", "gz");
    vfs.addMimeTypeMap("application/zip", "zip");
    vfs.setFileContentInfoFactory(new FileContentInfoFilenameFactory());
    vfs.setFilesCache(new SoftRefFilesCache());
    vfs.setReplicator(new UniqueFileReplicator(new File(System.getProperty("java.io.tmpdir"))));
    vfs.setCacheStrategy(CacheStrategy.ON_RESOLVE);
    vfs.init();
    return vfs;
  }

  public static Optional<ClassLoader> configureClassloader(String paths) throws FileSystemException {
    if(paths.trim().isEmpty()) {
      return Optional.empty();
    }
    FileSystemManager vfs = generateVfs();
    FileObject[] objects = resolve(vfs, paths);
    if(objects == null || objects.length == 0) {
      return Optional.empty();
    }
    return Optional.of(new VFSClassLoader(objects, vfs, ClassLoader.getSystemClassLoader()));
  }

  static FileObject[] resolve(FileSystemManager vfs, String uris) throws FileSystemException {
    return resolve(vfs, uris, new ArrayList<>());
  }

  static FileObject[] resolve(FileSystemManager vfs, String uris, ArrayList<FileObject> pathsToMonitor) throws FileSystemException {
    if (uris == null)
      return new FileObject[0];

    ArrayList<FileObject> classpath = new ArrayList<>();

    pathsToMonitor.clear();

    for (String path : uris.split(",")) {

      path = path.trim();

      if (path.equals(""))
        continue;

      FileObject fo = vfs.resolveFile(path);

      switch (fo.getType()) {
        case FILE:
        case FOLDER:
          classpath.add(fo);
          pathsToMonitor.add(fo);
          break;
        case IMAGINARY:
          // assume its a pattern
          String pattern = fo.getName().getBaseName();
          if (fo.getParent() != null && fo.getParent().getType() == FileType.FOLDER) {
            pathsToMonitor.add(fo.getParent());
            FileObject[] children = fo.getParent().getChildren();
            for (FileObject child : children) {
              if (child.getType() == FileType.FILE && child.getName().getBaseName().matches(pattern)) {
                classpath.add(child);
              }
            }
          } else {
            LOG.warn("ignoring classpath entry " + fo);
          }
          break;
        default:
          LOG.warn("ignoring classpath entry " + fo);
          break;
      }

    }

    return classpath.toArray(new FileObject[classpath.size()]);
  }
}
