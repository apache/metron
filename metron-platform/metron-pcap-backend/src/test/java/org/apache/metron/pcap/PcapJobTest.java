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

package org.apache.metron.pcap;

import com.google.common.collect.Iterables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.mr.PcapJob;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Long.toUnsignedString;
import static org.hamcrest.CoreMatchers.equalTo;

public class PcapJobTest {

  @Test
  public void test_getPaths_NoFiles() throws Exception {
    PcapJob job;
    {
      final List<Path> inputFiles = new ArrayList<Path>() {{
      }};
      job = new PcapJob() {
        @Override
        protected Iterable<Path> listFiles(FileSystem fs, Path basePath) throws IOException {
          return inputFiles;
        }
      };
      Iterable<String> paths = job.getPaths(null, null, 0, 1000);
      Assert.assertTrue(Iterables.isEmpty(paths));
    }
  }

  @Test
  public void test_getPaths_leftEdge() throws Exception {
    PcapJob job;
    {
      final List<Path> inputFiles = new ArrayList<Path>() {{
        add(new Path("/apps/metron/pcap/pcap_pcap_1461589332993573000_0_73686171-64a1-46e5-9e67-66cf603fb094"));
        add(new Path("/apps/metron/pcap/pcap_pcap_1561589332993573000_0_73686171-64a1-46e5-9e67-66cf603fb094"));
      }};
      job = new PcapJob() {
        @Override
        protected Iterable<Path> listFiles(FileSystem fs, Path basePath) throws IOException {
          return inputFiles;
        }
      };
      Iterable<String> paths = job.getPaths(null, null, 0, TimestampConverters.MILLISECONDS.toNanoseconds(System.currentTimeMillis()));
      Assert.assertEquals(1, Iterables.size(paths));
    }
  }

  @Test
  public void test_getPaths_rightEdge() throws Exception {
    PcapJob job;
    {
      final List<Path> inputFiles = new ArrayList<Path>() {{
        add(new Path("/apps/metron/pcap/pcap0_pcap_1461589332993573000_0_73686171-64a1-46e5-9e67-66cf603fb094"));
        add(new Path("/apps/metron/pcap/pcap1_pcap_1461589333993573000_0_73686171-64a1-46e5-9e67-66cf603fb094"));
      }};
      job = new PcapJob() {
        @Override
        protected Iterable<Path> listFiles(FileSystem fs, Path basePath) throws IOException {
          return inputFiles;
        }
      };
      Iterable<String> paths = job.getPaths(null, null, 1461589333993573000L - 1L, 1461589333993573000L + 1L);
      Assert.assertEquals(2, Iterables.size(paths));
    }
    {
      final List<Path> inputFiles = new ArrayList<Path>() {{
        add(new Path("/apps/metron/pcap/pcap0_pcap_1461589332993573000_0_73686171-64a1-46e5-9e67-66cf603fb094"));
        add(new Path("/apps/metron/pcap/pcap1_pcap_1461589333993573000_0_73686171-64a1-46e5-9e67-66cf603fb094"));
        add(new Path("/apps/metron/pcap/pcap1_pcap_1461589334993573000_0_73686171-64a1-46e5-9e67-66cf603fb094"));
      }};
      job = new PcapJob() {
        @Override
        protected Iterable<Path> listFiles(FileSystem fs, Path basePath) throws IOException {
          return inputFiles;
        }
      };
      Iterable<String> paths = job.getPaths(null, null, 1461589334993573000L - 1L, 1461589334993573000L + 1L);
      Assert.assertEquals(2, Iterables.size(paths));
    }
  }

  @Test
  public void test_getPaths_bothEdges() throws Exception {
    PcapJob job;
    {
      final List<Path> inputFiles = new ArrayList<Path>() {{
        add(new Path("/apps/metron/pcap/pcap_pcap_1461589332993573000_0_73686171-64a1-46e5-9e67-66cf603fb094"));
        add(new Path("/apps/metron/pcap/pcap_pcap_1461589333993573000_0_73686171-64a1-46e5-9e67-66cf603fb094"));
        add(new Path("/apps/metron/pcap/pcap1_pcap_1461589334993573000_0_73686171-64a1-46e5-9e67-66cf603fb094"));
      }};
      job = new PcapJob() {
        @Override
        protected Iterable<Path> listFiles(FileSystem fs, Path basePath) throws IOException {
          return inputFiles;
        }
      };
      Iterable<String> paths = job.getPaths(null, null, 0, TimestampConverters.MILLISECONDS.toNanoseconds(System.currentTimeMillis()));
      Assert.assertEquals(3, Iterables.size(paths));
    }
  }

  @Test
  public void partition_gives_value_in_range() throws Exception {
    long start = 1473897600000000000L;
    long end = TimestampConverters.MILLISECONDS.toNanoseconds(1473995927455L);
    Configuration conf = new Configuration();
    conf.set(PcapJob.START_TS_CONF, toUnsignedString(start));
    conf.set(PcapJob.END_TS_CONF, toUnsignedString(end));
    conf.set(PcapJob.WIDTH_CONF, "" + PcapJob.findWidth(start, end, 10));
    PcapJob.PcapPartitioner partitioner = new PcapJob.PcapPartitioner();
    partitioner.setConf(conf);
    Assert.assertThat("Partition not in range", partitioner.getPartition(new LongWritable(1473978789181189000L), new BytesWritable(), 10), equalTo(8));
  }
}
