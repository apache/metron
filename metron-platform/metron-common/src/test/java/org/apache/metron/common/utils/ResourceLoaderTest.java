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

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ResourceLoaderTest {

  @BeforeClass
  public static void before() throws Exception {
    File f = new File("target/test");
    if (!f.exists()) {
      f.createNewFile();
    }

    f = new File("target/resdir");
    if (!f.exists()) {
      f.mkdir();
    }

    f = new File("target/resdir/test1");
    if (!f.exists()) {
      f.createNewFile();
    }
  }

  @Test(expected = FileNotFoundException.class)
  public void testNonFileThrowsException() throws Exception {
    try (ResourceLoader loader = new ResourceLoader.Builder().build()) {
      loader.getResources("/usr/nope");
    }
  }

  @Test
  public void getResourcesShouldReturnResourceWithNoRootSet() throws Exception {
    try (ResourceLoader loader = new ResourceLoader.Builder().build()) {
      Map<String, InputStream> resources = loader.getResources("target/test");
      Assert.assertNotNull(resources.get("target/test"));
    }
  }

  @Test
  public void getResourcesShouldReturnResourceWithRootSet() throws Exception {
    Map<String, Object> config = ImmutableMap.of("metron.apps.hdfs.dir", "./target/resdir/");
    try (ResourceLoader loader = new ResourceLoader.Builder().withConfiguration(config).build()) {
      Map<String, InputStream> resources = loader.getResources("/test1");
      Assert.assertNotNull(resources.get("/test1"));
    }
  }

  @Test
  public void getResourcesShouldReturnResourceWithNoRootSetAndExistingFSConfig() throws Exception {
    try (ResourceLoader loader = new ResourceLoader.Builder()
        .withFileSystemConfiguration(new Configuration()).build()) {
      Map<String, InputStream> resources = loader.getResources("target/test");
      Assert.assertNotNull(resources.get("target/test"));
    }
  }

  @Test
  public void getResourcesShouldReturnResourceWithRootSetAndExistingFSConfig() throws Exception {
    Map<String, Object> config = ImmutableMap.of("metron.apps.hdfs.dir", "./target/resdir/");
    try (ResourceLoader loader = new ResourceLoader.Builder().withConfiguration(config)
        .withFileSystemConfiguration(new Configuration()).build()) {
      Map<String, InputStream> resources = loader.getResources("/test1");
      Assert.assertNotNull(resources.get("/test1"));
    }
  }
}