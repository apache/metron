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

package org.apache.metron.common.configuration.writer;

import org.apache.metron.common.configuration.IndexingConfigurations;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.apache.metron.test.bolt.BaseEnrichmentBoltTest.sampleSensorIndexingConfigPath;
import static org.apache.metron.test.bolt.BaseEnrichmentBoltTest.sensorType;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndexingWriterConfigurationTest {
  @Test
  public void testDefaultBatchSize() {
    IndexingWriterConfiguration config = new IndexingWriterConfiguration("hdfs",
           new IndexingConfigurations()
    );
    assertEquals(1, config.getBatchSize("foo"));
  }
  @Test
  public void testDefaultBatchTimeout() {
    IndexingWriterConfiguration config = new IndexingWriterConfiguration("hdfs",
           new IndexingConfigurations()
    );
    assertEquals(0, config.getBatchTimeout("foo"));
  }
  @Test
  public void testGetAllConfiguredTimeouts() throws FileNotFoundException, IOException {
    //default
    IndexingWriterConfiguration config = new IndexingWriterConfiguration("hdfs",
            new IndexingConfigurations()
    );
    assertEquals(0, config.getAllConfiguredTimeouts().size());
    //non-default
    IndexingConfigurations iconfigs = new IndexingConfigurations();
    iconfigs.updateSensorIndexingConfig(
            sensorType, new FileInputStream(sampleSensorIndexingConfigPath));
    config = new IndexingWriterConfiguration("elasticsearch", iconfigs);
    assertEquals(1, config.getAllConfiguredTimeouts().size());
    assertEquals(7, (long)config.getAllConfiguredTimeouts().get(0));
  }
  @Test
  public void testDefaultIndex() {
    IndexingWriterConfiguration config = new IndexingWriterConfiguration("hdfs",
           new IndexingConfigurations()
    );
    assertEquals("foo", config.getIndex("foo"));
  }
}
