/*
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
package org.apache.metron.rest.mock;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.hadoop.SequenceFileIterable;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.mr.PcapJob;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockPcapJob extends PcapJob {

  private String basePath;
  private String baseOutputPath;
  private long beginNS;
  private long endNS;
  private int numReducers;
  private Map<String, String> fixedFields;
  private PcapFilterConfigurator filterImpl;
  private SequenceFileIterable sequenceFileIterable;

  public MockPcapJob() {
    sequenceFileIterable = mock(SequenceFileIterable.class);
  }

  @SuppressWarnings(value = "unchecked")
  @Override
  public <T> SequenceFileIterable query(Path basePath, Path baseOutputPath, long beginNS, long endNS, int numReducers, T fields, Configuration conf, FileSystem fs, PcapFilterConfigurator<T> filterImpl) throws IOException, ClassNotFoundException, InterruptedException {
    this.basePath = basePath.toString();
    this.baseOutputPath = baseOutputPath.toString();
    this.beginNS = beginNS;
    this.endNS = endNS;
    this.numReducers = numReducers;
    if (fields instanceof Map) {
      this.fixedFields = (Map<String, String>) fields;
    }
    this.filterImpl = filterImpl;
    return sequenceFileIterable;
  }

  public void setResults(List<byte[]> pcaps) {
    when(sequenceFileIterable.iterator()).thenReturn(pcaps.iterator());
  }

  public String getBasePath() {
    return basePath;
  }

  public String getBaseOutputPath() {
    return baseOutputPath;
  }

  public long getStartTime() {
    return beginNS / 1000000;
  }

  public long getEndTime() {
    return endNS / 1000000;
  }

  public int getNumReducers() {
    return numReducers;
  }

  public Map<String, String> getFixedFields() {
    return fixedFields;
  }

  public PcapFilterConfigurator getFilterImpl() {
    return filterImpl;
  }
}
