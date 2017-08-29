/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.common.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

/**
 * <p> The ResourceLoader loads resources from paths.  If configured, it will handle 'rooting' the
 * paths to correctly reflect the current configuration. This means that a configured path
 * '/foo/bar' can be correctly resolved to /some/hdfs/root/foo/bar or ./foo/bar through
 * configuration of the current root.</p>
 *
 * <p> The ResourceLoader addresses issues that arise when the root path is a configuration option,
 * but there are coded paths in configurations</p>
 *
 * If no configuration is provided, the defaults will be used. If the global configuration does
 * not have a "metron.apps.hdfs.dir" setting then the default will be used. </p>
 * <p>This class implements {@link AutoCloseable} and should
 * ideally be used with a try with resources pattern</p>
 */
public class ResourceLoader implements AutoCloseable {

  /**
   * Builds a new ResourceLoader.
   */
  public static class Builder {

    private Configuration fileSystemConfiguration;
    private Map<String, Object> configuration;

    /**
     * Provide a {@link Configuration}.
     *
     * @param fileSystemConfiguration Hadoop Configuration
     * @return The Builder
     */
    public Builder withFileSystemConfiguration(Configuration fileSystemConfiguration) {
      this.fileSystemConfiguration = fileSystemConfiguration;
      return this;
    }

    /**
     * Provide a configuration Map This map should have an inner map at key "globalConfig", and that
     * map should have a key "metron.apps.hdfs.dir".
     *
     * @param configuration the Map
     * @return the Builder
     */
    public Builder withConfiguration(Map<String, Object> configuration) {
      this.configuration = configuration;
      return this;
    }

    /**
     * Builds a new {@link ResourceLoader}.
     *
     * @return an instance of ResourceLoader
     */
    public ResourceLoader build() {
      return new ResourceLoader(configuration, fileSystemConfiguration);
    }
  }


  private static final String DEFAULT_ROOT_DIR = StringUtils.EMPTY;
  private String root = DEFAULT_ROOT_DIR;
  private FileSystem fileSystem;
  private Configuration fileSystemConfiguration;

  private ResourceLoader(){}

  @SuppressWarnings("unchecked")
  private ResourceLoader(Map<String, Object> configuration, Configuration fileSystemConfiguration) {
    if (configuration != null) {
      if (configuration.containsKey("metron.apps.hdfs.dir")) {
        root = (String) configuration.get("metron.apps.hdfs.dir");
      }
    }
    if (fileSystemConfiguration != null) {
      this.fileSystemConfiguration = fileSystemConfiguration;
    } else {
      this.fileSystemConfiguration = new Configuration();
    }
  }

  /**
   * getResources returns resource streams, mapped to their names. If the passed resourceLocation is
   * a directory, then all resources located in that directory and subdirectories will be loaded and
   * returned. If the passed resourceLocation is not a directory, the that location will be loaded
   * and returned
   *
   * @param resourceLocation The location from which to attempt to load resources
   * @return Map of resource names to InputStreams
   * @throws IOException if the resource doesn't exist, or if there is a configured root that does
   *                     not exist
   */
  public Map<String, InputStream> getResources(String resourceLocation) throws IOException {
    Map<String, InputStream> streamMap = new HashMap<>();
    if (fileSystem == null) {
      fileSystem = FileSystem.get(fileSystemConfiguration);
    }
    Path path = new Path(resourceLocation);
    if (!StringUtils.isEmpty(root) && !resourceLocation.startsWith(root)) {
      // mergePaths does not handle merging and separators well
      // as in merging '/root' and 'child/foo'  will result in
      // a path of '/rootchild/foo'
      // this is almost certainly not what you would expect
      // we will fix this up here
      String thisRoot = root;
      if(!thisRoot.endsWith("/") && !resourceLocation.startsWith("/")) {
        thisRoot = thisRoot + "/";
      }
      Path rootPath = new Path(thisRoot);
      if (!fileSystem.exists(rootPath)) {
        throw new FileNotFoundException(thisRoot);
      }
      path = Path.mergePaths(rootPath, path);
    }
    if (fileSystem.exists(path)) {
      if (fileSystem.isDirectory(path)) {
        RemoteIterator<LocatedFileStatus> fileStatusRemoteIterator = fileSystem
            .listFiles(path, true);
        while (fileStatusRemoteIterator.hasNext()) {
          LocatedFileStatus locatedFileStatus = fileStatusRemoteIterator.next();
          if (!locatedFileStatus.isDirectory()) {
            streamMap
                .put(locatedFileStatus.getPath().getName(),
                    fileSystem.open(locatedFileStatus.getPath()));
          }
        }
      } else {
        streamMap.put(resourceLocation, fileSystem.open(path));
      }
    } else {
      InputStream classLoaderStream = getClass().getResourceAsStream(resourceLocation);
      if (classLoaderStream == null) {
        throw new FileNotFoundException(resourceLocation);
      }
      streamMap.put(resourceLocation, classLoaderStream);
    }
    return streamMap;
  }

  @Override
  public void close() throws Exception {
    if (fileSystem != null) {
      fileSystem.close();
      fileSystem = null;
    }
  }
}
