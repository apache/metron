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

package org.apache.metron.common.writer;

import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.writer.IndexingWriterConfiguration;
import org.junit.Assert;
import org.junit.Test;

public class IndexingWriterConfigurationTest {
  @Test
  public void testDefaultBatchSize() {
    IndexingWriterConfiguration config = new IndexingWriterConfiguration("hdfs",
           new IndexingConfigurations()
    );
    Assert.assertEquals(1, config.getBatchSize("foo"));
  }
  @Test
  public void testDefaultBatchTimeout() {
    IndexingWriterConfiguration config = new IndexingWriterConfiguration("hdfs",
           new IndexingConfigurations()
    );
    Assert.assertEquals(0, config.getBatchTimeout("foo"));
  }
  @Test
  public void testGetAllConfiguredTimeouts() {
    IndexingWriterConfiguration config = new IndexingWriterConfiguration("hdfs",
           new IndexingConfigurations()
    );
    Assert.assertEquals(0, config.getAllConfiguredTimeouts().size());
  }
  @Test
  public void testDefaultIndex() {
    IndexingWriterConfiguration config = new IndexingWriterConfiguration("hdfs",
           new IndexingConfigurations()
    );
    Assert.assertEquals("foo", config.getIndex("foo"));
  }
}
