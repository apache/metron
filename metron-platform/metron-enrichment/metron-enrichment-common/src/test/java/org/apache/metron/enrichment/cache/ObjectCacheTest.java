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

package org.apache.metron.enrichment.cache;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.integration.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectCacheTest {
  private FileSystem fs;
  private List<String> data;
  private ObjectCache cache;
  private File tempDir;

  @BeforeEach
  public void setup() throws IOException {
    fs = FileSystem.get(new Configuration());
    data = new ArrayList<>();
    {
      data.add("apache");
      data.add("metron");
      data.add("is");
      data.add("great");
    }
    cache = new ObjectCache();
    tempDir = TestUtils.createTempDir(this.getClass().getName());
  }

  @Test
  public void test() throws Exception {
    String filename = "test.ser";
    assertTrue(cache.isEmpty() || !cache.containsKey(filename));
    assertDataIsReadCorrectly(filename);
  }

  public void assertDataIsReadCorrectly(String filename) throws IOException {
    File file = new File(tempDir, filename);
    try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
      IOUtils.write(SerDeUtils.toBytes(data), bos);
    }
    cache.initialize(new ObjectCacheConfig(new HashMap<>()));
    List<String> readData = (List<String>) cache.get(file.getAbsolutePath());
    assertEquals(readData, data);
    assertTrue(cache.containsKey(file.getAbsolutePath()));
  }

  @Test
  public void testMultithreaded() throws Exception {
    String filename = "testmulti.ser";
    assertTrue(cache.isEmpty() || !cache.containsKey(filename));
    Thread[] ts = new Thread[10];
    for(int i = 0;i < ts.length;++i) {
      ts[i] = new Thread(() -> {
        try {
          assertDataIsReadCorrectly(filename);
        } catch (Exception e) {
          throw new IllegalStateException(e.getMessage(), e);
        }
      });
      ts[i].start();
    }
    for(Thread t : ts) {
      t.join();
    }
  }

  @Test
  public void shouldThrowExceptionOnMaxFileSize() throws Exception {
    String filename = "maxSizeException.ser";
    File file = new File(tempDir, filename);

    try(BufferedOutputStream bos = new BufferedOutputStream(fs.create(new Path(file.getAbsolutePath()), true))) {
      IOUtils.write(SerDeUtils.toBytes(data), bos);
    }
    ObjectCacheConfig objectCacheConfig = new ObjectCacheConfig(new HashMap<>());
    objectCacheConfig.setMaxFileSize(1);
    cache.initialize(objectCacheConfig);

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> cache.get(file.getAbsolutePath()));
    assertTrue(e.getMessage().contains(
            String.format("File at path '%s' is larger than the configured max file size of 1", file.getAbsolutePath())));
  }
}
