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

package org.apache.metron.bundles.util;

import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.metron.bundles.integration.BundleMapperIntegrationTest;

import java.net.URISyntaxException;
import java.util.Map;

public class TestUtil {
  public static BundleProperties loadSpecifiedProperties(final String propertiesFile, final Map<String, String> others) {
    String filePath;
    try {
      filePath = BundleMapperIntegrationTest.class.getResource(propertiesFile).toURI().getPath();
    } catch (URISyntaxException ex) {
      throw new RuntimeException("Cannot load properties file due to "
              + ex.getLocalizedMessage(), ex);
    }
    return BundleProperties.createBasicBundleProperties(filePath, others);
  }

  public static List<FileObject> getExtensionLibs(FileSystemManager fileSystemManager, BundleProperties properties) throws URISyntaxException, FileSystemException{
    List<URI> libDirs = properties.getBundleLibraryDirectories();
    List<FileObject> libFileObjects = new ArrayList<>();
    for(URI libUri : libDirs){
      FileObject fileObject = fileSystemManager.resolveFile(libUri);
      if(fileObject.exists()){
        libFileObjects.add(fileObject);
      }
    }
    return libFileObjects;
  }
}
