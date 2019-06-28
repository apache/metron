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

package org.apache.metron.pcap.mr;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hadoop.fs.Path;
import org.apache.metron.pcap.utils.FileFilterUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FileFilterUtilTest {

  private List<Path> filesIn;

  @Before
  public void setup() {
    filesIn = new ArrayList<>();
    filesIn.add(new Path("/apath/pcap_pcap5_1495135372055519000_2_pcap-9-1495134910"));
    filesIn.add(new Path("/apath/pcap_pcap5_1495135372168719000_1_pcap-9-1495134910"));
    filesIn.add(new Path("/apath/pcap_pcap5_1495135377055375000_0_pcap-9-1495134910"));
    filesIn.add(new Path("/apath/pcap_pcap5_1495135512102506000_4_pcap-9-1495134910"));
    filesIn.add(new Path("/apath/pcap_pcap5_1495135512123943000_3_pcap-9-1495134910"));
  }

  @Test
  public void returns_files_by_partition() {
    Map<Integer, List<Path>> filesByPartition = FileFilterUtil.getFilesByPartition(filesIn);
    Map<Integer, List<Path>> expectedFilesPartitioned = new HashMap() {{
      put(0, toList("/apath/pcap_pcap5_1495135377055375000_0_pcap-9-1495134910"));
      put(1, toList("/apath/pcap_pcap5_1495135372168719000_1_pcap-9-1495134910"));
      put(2, toList("/apath/pcap_pcap5_1495135372055519000_2_pcap-9-1495134910"));
      put(3, toList("/apath/pcap_pcap5_1495135512123943000_3_pcap-9-1495134910"));
      put(4, toList("/apath/pcap_pcap5_1495135512102506000_4_pcap-9-1495134910"));
    }};
    assertThat(filesByPartition, equalTo(expectedFilesPartitioned));
  }

  private List<Path> toList(String... items) {
    return Arrays.asList(items).stream().map(i -> new Path(i)).collect(Collectors.toList());
  }

  @Test
  public void returns_left_trailing_filtered_list() {
    Map<Integer, List<Path>> filesByPartition = new HashMap() {{
      put(0, toList("/apath/pcap_pcap5_1495135377055375000_0_pcap-9-1495134910"));
      put(1, toList("/apath/pcap_pcap5_1495135372168719000_1_pcap-9-1495134910"));
      put(2, toList("/apath/pcap_pcap5_1495135372055519000_2_pcap-9-1495134910"));
      put(3, toList("/apath/pcap_pcap5_1495135512123943000_3_pcap-9-1495134910"));
      put(4, toList("/apath/pcap_pcap5_1495135512102506000_4_pcap-9-1495134910"));
    }};
    List<String> lt = FileFilterUtil
        .filterByTimestampLT(1495135377055375000L, 1495135512124943000L, filesByPartition);
    List<String> expectedFiles = Arrays.asList(
        "/apath/pcap_pcap5_1495135377055375000_0_pcap-9-1495134910",
        "/apath/pcap_pcap5_1495135372168719000_1_pcap-9-1495134910",
        "/apath/pcap_pcap5_1495135372055519000_2_pcap-9-1495134910",
        "/apath/pcap_pcap5_1495135512123943000_3_pcap-9-1495134910",
        "/apath/pcap_pcap5_1495135512102506000_4_pcap-9-1495134910");
    assertThat(lt, equalTo(expectedFiles));
  }

  @Test
  public void returns_left_trailing_filtered_list_from_paths() {
    Iterable<String> paths = FileFilterUtil
        .getPathsInTimeRange(1495135377055375000L, 1495135512124943000L, filesIn);
    List<String> expectedFiles = Arrays.asList(
        "/apath/pcap_pcap5_1495135377055375000_0_pcap-9-1495134910",
        "/apath/pcap_pcap5_1495135372168719000_1_pcap-9-1495134910",
        "/apath/pcap_pcap5_1495135372055519000_2_pcap-9-1495134910",
        "/apath/pcap_pcap5_1495135512123943000_3_pcap-9-1495134910",
        "/apath/pcap_pcap5_1495135512102506000_4_pcap-9-1495134910");
    assertThat(paths, equalTo(expectedFiles));
  }

  @Test
  public void test_getPaths_NoFiles() throws Exception {
    final List<Path> inputFiles = new ArrayList<Path>();
    Iterable<String> paths = FileFilterUtil.getPathsInTimeRange(0, 1000, inputFiles);
    Assert.assertTrue(Iterables.isEmpty(paths));
  }

  @Test
  public void test_getPaths_leftEdge() throws Exception {
    final long firstFileTSNanos = 1461589332993573000L;
    final long secondFileTSNanos = 1561589332993573000L;
    final List<Path> inputFiles = new ArrayList<Path>() {{
      add(new Path("/apps/metron/pcap/pcap_pcap_" + firstFileTSNanos + "_0_73686171-64a1-46e5-9e67-66cf603fb094"));
      add(new Path("/apps/metron/pcap/pcap_pcap_" + secondFileTSNanos + "_0_73686171-64a1-46e5-9e67-66cf603fb094"));
    }};
    Iterable<String> paths = FileFilterUtil.getPathsInTimeRange(0, secondFileTSNanos - 1L, inputFiles);
    Assert.assertEquals(1, Iterables.size(paths));
  }

  @Test
  public void test_getPaths_rightEdge() throws Exception {
    final long firstFileTSNanos = 1461589332993573000L;
    final long secondFileTSNanos = 1461589333993573000L;
    final long thirdFileTSNanos = 1461589334993573000L;
    {
      final List<Path> inputFiles = new ArrayList<Path>() {{
        add(new Path("/apps/metron/pcap/pcap0_pcap_" + firstFileTSNanos + "_0_73686171-64a1-46e5-9e67-66cf603fb094"));
        add(new Path("/apps/metron/pcap/pcap1_pcap_" + secondFileTSNanos + "_0_73686171-64a1-46e5-9e67-66cf603fb094"));
      }};
      Iterable<String> paths = FileFilterUtil.getPathsInTimeRange(secondFileTSNanos - 1L, secondFileTSNanos + 1L, inputFiles);
      Assert.assertEquals(2, Iterables.size(paths));
    }
    {
      final List<Path> inputFiles = new ArrayList<Path>() {{
        add(new Path("/apps/metron/pcap/pcap0_pcap_" + firstFileTSNanos + "_0_73686171-64a1-46e5-9e67-66cf603fb094"));
        add(new Path("/apps/metron/pcap/pcap1_pcap_" + secondFileTSNanos + "_0_73686171-64a1-46e5-9e67-66cf603fb094"));
        add(new Path("/apps/metron/pcap/pcap1_pcap_" + thirdFileTSNanos + "_0_73686171-64a1-46e5-9e67-66cf603fb094"));
      }};
      Iterable<String> paths = FileFilterUtil.getPathsInTimeRange(thirdFileTSNanos - 1L, thirdFileTSNanos + 1L, inputFiles);
      Assert.assertEquals(2, Iterables.size(paths));
    }
  }

  @Test
  public void test_getPaths_bothEdges() throws Exception {
    final long firstFileTSNanos = 1461589332993573000L;
    final long secondFileTSNanos = 1461589333993573000L;
    final long thirdFileTSNanos = 1461589334993573000L;
    final List<Path> inputFiles = new ArrayList<Path>() {{
      add(new Path("/apps/metron/pcap/pcap_pcap_" + firstFileTSNanos + "_0_73686171-64a1-46e5-9e67-66cf603fb094"));
      add(new Path("/apps/metron/pcap/pcap_pcap_" + secondFileTSNanos + "_0_73686171-64a1-46e5-9e67-66cf603fb094"));
      add(new Path("/apps/metron/pcap/pcap1_pcap_" + thirdFileTSNanos + "_0_73686171-64a1-46e5-9e67-66cf603fb094"));
    }};
    Iterable<String> paths = FileFilterUtil.getPathsInTimeRange(0, thirdFileTSNanos + 1L, inputFiles);
    Assert.assertEquals(3, Iterables.size(paths));
  }
}
