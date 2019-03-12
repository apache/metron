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

package org.apache.metron.enrichment.stellar;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ObjectGetTest {
  FileSystem fs;
  List<String> data;

  @Before
  public void setup() throws IOException {
    fs = FileSystem.get(new Configuration());
    data = new ArrayList<>();
    {
      data.add("apache");
      data.add("metron");
      data.add("is");
      data.add("great");
    }

  }

  @Test
  public void test() throws Exception {
    String filename = "target/ogt/test.ser";
    Assert.assertTrue(ObjectGet.cache == null || !ObjectGet.cache.asMap().containsKey(filename));
    assertDataIsReadCorrectly(filename);
  }

  public void assertDataIsReadCorrectly(String filename) throws IOException {
    try(BufferedOutputStream bos = new BufferedOutputStream(fs.create(new Path(filename), true))) {
      IOUtils.write(SerDeUtils.toBytes(data), bos);
    }
    List<String> readData = (List<String>) StellarProcessorUtils.run("OBJECT_GET(loc)", ImmutableMap.of("loc", filename));
    Assert.assertEquals(readData, data);
    Assert.assertTrue(ObjectGet.cache.asMap().containsKey(filename));
  }

  @Test
  public void testMultithreaded() throws Exception {
    String filename = "target/ogt/testmulti.ser";
    Assert.assertTrue(ObjectGet.cache == null || !ObjectGet.cache.asMap().containsKey(filename));
    Thread[] ts = new Thread[10];
    for(int i = 0;i < ts.length;++i) {
      ts[i] = new Thread(() -> {
        try {
          assertDataIsReadCorrectly(filename);
        } catch (IOException e) {
          throw new IllegalStateException(e.getMessage(), e);
        }
      });
      ts[i].start();
    }
    for(Thread t : ts) {
      t.join();
    }
  }
}
