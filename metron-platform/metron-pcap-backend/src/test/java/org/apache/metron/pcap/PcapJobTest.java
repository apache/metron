/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.pcap;

import static java.lang.Long.toUnsignedString;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.JobStatus.State;
import org.apache.metron.job.Statusable;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PcapJobTest {

  @Mock
  private Job job;
  @Mock
  private org.apache.hadoop.mapreduce.JobStatus mrStatus;
  @Mock
  private JobID jobId;
  private static final String JOB_ID_VAL = "job_abc_123";
  private Path basePath;
  private Path baseOutPath;
  private long startTime;
  private long endTime;
  private int numReducers;
  private Map<String, String> fixedFields;
  private Configuration hadoopConfig;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    basePath = new Path("basepath");
    baseOutPath = new Path("outpath");
    startTime = 100;
    endTime = 200;
    numReducers = 5;
    fixedFields = new HashMap<>();
    fixedFields.put("ip_src_addr", "192.168.1.1");
    hadoopConfig = new Configuration();
    when(jobId.toString()).thenReturn(JOB_ID_VAL);
    when(mrStatus.getJobID()).thenReturn(jobId);
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
    Assert.assertThat("Partition not in range",
        partitioner.getPartition(new LongWritable(1473978789181189000L), new BytesWritable(), 10),
        equalTo(8));
  }

  private class TestJob extends PcapJob {

    @Override
    public <T> Job createJob(Optional<String> jobName, Path basePath, Path outputPath, long beginNS, long endNS,
        int numReducers, T fields, Configuration conf, FileSystem fs,
        PcapFilterConfigurator<T> filterImpl) throws IOException {
      return job;
    }
  }

  @Test
  public void job_succeeds_synchronously() throws Exception {
    when(job.isComplete()).thenReturn(true);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.SUCCEEDED);
    when(job.getStatus()).thenReturn(mrStatus);
    TestJob testJob = new TestJob();
    Statusable statusable = testJob.query(
        Optional.empty(),
        basePath,
        baseOutPath,
        startTime,
        endTime,
        numReducers,
        fixedFields,
        hadoopConfig,
        FileSystem.get(hadoopConfig),
        new FixedPcapFilter.Configurator(),
        true);
    verify(job, times(1)).waitForCompletion(true);
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.SUCCEEDED));
    Assert.assertThat(status.getPercentComplete(), equalTo(100.0));
    String expectedOutPath = new Path(baseOutPath, format("%s_%s_%s", startTime, endTime, "192.168.1.1")).toString();
    Assert.assertThat(status.getResultPath(), notNullValue());
    Assert.assertThat(status.getResultPath().toString(), startsWith(expectedOutPath));
  }

  @Test
  public void job_fails_synchronously() throws Exception {
    when(job.isComplete()).thenReturn(true);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.FAILED);
    when(job.getStatus()).thenReturn(mrStatus);
    TestJob testJob = new TestJob();
    Statusable statusable = testJob.query(
        Optional.empty(),
        basePath,
        baseOutPath,
        startTime,
        endTime,
        numReducers,
        fixedFields,
        hadoopConfig,
        FileSystem.get(hadoopConfig),
        new FixedPcapFilter.Configurator(),
        true);
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.FAILED));
    Assert.assertThat(status.getPercentComplete(), equalTo(100.0));
    String expectedOutPath = new Path(baseOutPath, format("%s_%s_%s", startTime, endTime, "192.168.1.1")).toString();
    Assert.assertThat(status.getResultPath(), notNullValue());
    Assert.assertThat(status.getResultPath().toString(), startsWith(expectedOutPath));
  }

  @Test
  public void job_fails_with_killed_status_synchronously() throws Exception {
    when(job.isComplete()).thenReturn(true);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.KILLED);
    when(job.getStatus()).thenReturn(mrStatus);
    TestJob testJob = new TestJob();
    Statusable statusable = testJob.query(
        Optional.empty(),
        basePath,
        baseOutPath,
        startTime,
        endTime,
        numReducers,
        fixedFields,
        hadoopConfig,
        FileSystem.get(hadoopConfig),
        new FixedPcapFilter.Configurator(),
        true);
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.KILLED));
    Assert.assertThat(status.getPercentComplete(), equalTo(100.0));
    String expectedOutPath = new Path(baseOutPath, format("%s_%s_%s", startTime, endTime, "192.168.1.1")).toString();
    Assert.assertThat(status.getResultPath(), notNullValue());
    Assert.assertThat(status.getResultPath().toString(), startsWith(expectedOutPath));
  }

  @Test
  public void job_succeeds_asynchronously() throws Exception {
    when(job.isComplete()).thenReturn(true);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.SUCCEEDED);
    when(job.getStatus()).thenReturn(mrStatus);
    TestJob testJob = new TestJob();
    Statusable statusable = testJob.query(
        Optional.empty(),
        basePath,
        baseOutPath,
        startTime,
        endTime,
        numReducers,
        fixedFields,
        hadoopConfig,
        FileSystem.get(hadoopConfig),
        new FixedPcapFilter.Configurator(),
        false);
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.SUCCEEDED));
    Assert.assertThat(status.getPercentComplete(), equalTo(100.0));
    String expectedOutPath = new Path(baseOutPath, format("%s_%s_%s", startTime, endTime, "192.168.1.1")).toString();
    Assert.assertThat(status.getResultPath(), notNullValue());
    Assert.assertThat(status.getResultPath().toString(), startsWith(expectedOutPath));
  }

  @Test
  public void job_reports_percent_complete() throws Exception {
    when(job.isComplete()).thenReturn(false);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.RUNNING);
    when(job.getStatus()).thenReturn(mrStatus);
    TestJob testJob = new TestJob();
    Statusable statusable = testJob.query(
        Optional.empty(),
        basePath,
        baseOutPath,
        startTime,
        endTime,
        numReducers,
        fixedFields,
        hadoopConfig,
        FileSystem.get(hadoopConfig),
        new FixedPcapFilter.Configurator(),
        false);
    when(job.mapProgress()).thenReturn(0.5f);
    when(job.reduceProgress()).thenReturn(0f);
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.RUNNING));
    Assert.assertThat(status.getDescription(), equalTo("map: 50.0%, reduce: 0.0%"));
    Assert.assertThat(status.getPercentComplete(), equalTo(25.0));
    when(job.mapProgress()).thenReturn(1.0f);
    when(job.reduceProgress()).thenReturn(0.5f);
    status = statusable.getStatus();
    Assert.assertThat(status.getPercentComplete(), equalTo(75.0));
    Assert.assertThat(status.getDescription(), equalTo("map: 100.0%, reduce: 50.0%"));
  }

}
