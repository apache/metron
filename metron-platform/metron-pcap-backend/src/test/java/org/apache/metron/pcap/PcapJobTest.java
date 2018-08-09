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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.job.Finalizer;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.JobStatus.State;
import org.apache.metron.job.Pageable;
import org.apache.metron.job.Statusable;
import org.apache.metron.pcap.config.FixedPcapConfig;
import org.apache.metron.pcap.config.PcapOptions;
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
  private Job mrJob;
  @Mock
  private org.apache.hadoop.mapreduce.JobStatus mrStatus;
  @Mock
  private JobID jobId;
  @Mock
  private Finalizer<Path> finalizer;
  private TestTimer timer;
  private Pageable<Path> pageableResult;
  private FixedPcapConfig config;
  private Configuration hadoopConfig;
  private FileSystem fileSystem;
  private String jobIdVal = "job_abc_123";
  private Path basePath;
  private Path baseOutPath;
  private long startTime;
  private long endTime;
  private int numReducers;
  private int numRecordsPerFile;
  private Path finalOutputPath;
  private Map<String, String> fixedFields;
  private PcapJob<Map<String, String>> testJob;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    basePath = new Path("basepath");
    baseOutPath = new Path("outpath");
    startTime = 100;
    endTime = 200;
    numReducers = 5;
    numRecordsPerFile = 5;
    fixedFields = new HashMap<>();
    fixedFields.put("ip_src_addr", "192.168.1.1");
    hadoopConfig = new Configuration();
    fileSystem = FileSystem.get(hadoopConfig);
    finalOutputPath = new Path("finaloutpath");
    when(jobId.toString()).thenReturn(jobIdVal);
    when(mrStatus.getJobID()).thenReturn(jobId);
    when(mrJob.getJobID()).thenReturn(jobId);
    pageableResult = new PcapPages();
    timer = new TestTimer();
    // handles setting the file name prefix under the hood
    config = new FixedPcapConfig(clock -> "clockprefix");
    PcapOptions.HADOOP_CONF.put(config, hadoopConfig);
    PcapOptions.FILESYSTEM.put(config, FileSystem.get(hadoopConfig));
    PcapOptions.BASE_PATH.put(config, basePath);
    PcapOptions.BASE_INTERIM_RESULT_PATH.put(config, baseOutPath);
    PcapOptions.START_TIME_NS.put(config, startTime);
    PcapOptions.END_TIME_NS.put(config, endTime);
    PcapOptions.NUM_REDUCERS.put(config, numReducers);
    PcapOptions.FIELDS.put(config, fixedFields);
    PcapOptions.FILTER_IMPL.put(config, new FixedPcapFilter.Configurator());
    PcapOptions.NUM_RECORDS_PER_FILE.put(config, numRecordsPerFile);
    PcapOptions.FINAL_OUTPUT_PATH.put(config, finalOutputPath);
    testJob = new TestJob<>(mrJob);
    testJob.setStatusInterval(1);
    testJob.setCompleteCheckInterval(1);
    testJob.setTimer(timer);
  }

  private class TestJob<T> extends PcapJob<T> {

    private final Job mrJob;

    public TestJob(Job mrJob) {
      this.mrJob = mrJob;
    }

    @Override
    public Job createJob(Optional<String> jobName,
        Path basePath,
        Path outputPath,
        long beginNS,
        long endNS,
        int numReducers,
        T fields,
        Configuration conf,
        FileSystem fs,
        PcapFilterConfigurator<T> filterImpl) throws IOException {
      return mrJob;
    }
  }

  private class TestTimer extends Timer {

    private TimerTask task;

    @Override
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
      this.task = task;
    }

    public void updateJobStatus() {
      task.run();
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
    Assert.assertThat("Partition not in range",
        partitioner.getPartition(new LongWritable(1473978789181189000L), new BytesWritable(), 10),
        equalTo(8));
  }

  @Test
  public void job_succeeds_synchronously() throws Exception {
    pageableResult = new PcapPages(
        Arrays.asList(new Path("1.txt"), new Path("2.txt"), new Path("3.txt")));
    when(finalizer.finalizeJob(any())).thenReturn(pageableResult);
    when(mrJob.isComplete()).thenReturn(true);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.SUCCEEDED);
    when(mrJob.getStatus()).thenReturn(mrStatus);
    Statusable<Path> statusable = testJob.submit(finalizer, config);
    timer.updateJobStatus();
    Pageable<Path> results = statusable.get();
    Assert.assertThat(results.getSize(), equalTo(3));
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.SUCCEEDED));
    Assert.assertThat(status.getPercentComplete(), equalTo(100.0));
    Assert.assertThat(status.getJobId(), equalTo(jobIdVal));
  }

  @Test
  public void job_fails_synchronously() throws Exception {
    when(mrJob.isComplete()).thenReturn(true);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.FAILED);
    when(mrJob.getStatus()).thenReturn(mrStatus);
    Statusable<Path> statusable = testJob.submit(finalizer, config);
    timer.updateJobStatus();
    Pageable<Path> results = statusable.get();
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.FAILED));
    Assert.assertThat(status.getPercentComplete(), equalTo(100.0));
    Assert.assertThat(results.getSize(), equalTo(0));
  }

  @Test
  public void job_fails_with_killed_status_synchronously() throws Exception {
    when(mrJob.isComplete()).thenReturn(true);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.KILLED);
    when(mrJob.getStatus()).thenReturn(mrStatus);
    Statusable<Path> statusable = testJob.submit(finalizer, config);
    timer.updateJobStatus();
    Pageable<Path> results = statusable.get();
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.KILLED));
    Assert.assertThat(status.getPercentComplete(), equalTo(100.0));
    Assert.assertThat(results.getSize(), equalTo(0));
  }

  @Test
  public void job_succeeds_asynchronously() throws Exception {
    when(mrJob.isComplete()).thenReturn(true);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.SUCCEEDED);
    when(mrJob.getStatus()).thenReturn(mrStatus);
    Statusable<Path> statusable = testJob.submit(finalizer, config);
    timer.updateJobStatus();
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.SUCCEEDED));
    Assert.assertThat(status.getPercentComplete(), equalTo(100.0));
  }

  @Test
  public void job_reports_percent_complete() throws Exception {
    when(mrJob.isComplete()).thenReturn(false);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.RUNNING);
    when(mrJob.getStatus()).thenReturn(mrStatus);
    when(mrJob.mapProgress()).thenReturn(0.5f);
    when(mrJob.reduceProgress()).thenReturn(0f);
    Statusable<Path> statusable = testJob.submit(finalizer, config);
    timer.updateJobStatus();
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.RUNNING));
    Assert.assertThat(status.getDescription(), equalTo("map: 50.0%, reduce: 0.0%"));
    Assert.assertThat(status.getPercentComplete(), equalTo(25.0));
    when(mrJob.mapProgress()).thenReturn(1.0f);
    when(mrJob.reduceProgress()).thenReturn(0.5f);
    timer.updateJobStatus();
    status = statusable.getStatus();
    Assert.assertThat(status.getDescription(), equalTo("map: 100.0%, reduce: 50.0%"));
    Assert.assertThat(status.getPercentComplete(), equalTo(75.0));
  }

  @Test
  public void killing_job_causes_status_to_return_KILLED_state() throws Exception {
    when(mrJob.isComplete()).thenReturn(false);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.RUNNING);
    when(mrJob.getStatus()).thenReturn(mrStatus);
    Statusable<Path> statusable = testJob.submit(finalizer, config);
    statusable.kill();
    when(mrJob.isComplete()).thenReturn(true);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.KILLED);
    timer.updateJobStatus();
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.KILLED));
  }

  @Test
  public void handles_null_values_with_defaults() throws Exception {
    PcapOptions.START_TIME_NS.put(config, null);
    PcapOptions.END_TIME_NS.put(config, null);
    PcapOptions.NUM_REDUCERS.put(config, null);
    PcapOptions.NUM_RECORDS_PER_FILE.put(config, null);

    pageableResult = new PcapPages(
        Arrays.asList(new Path("1.txt"), new Path("2.txt"), new Path("3.txt")));
    when(finalizer.finalizeJob(any())).thenReturn(pageableResult);
    when(mrJob.isComplete()).thenReturn(true);
    when(mrStatus.getState()).thenReturn(org.apache.hadoop.mapreduce.JobStatus.State.SUCCEEDED);
    when(mrJob.getStatus()).thenReturn(mrStatus);
    Statusable<Path> statusable = testJob.submit(finalizer, config);
    timer.updateJobStatus();
    Pageable<Path> results = statusable.get();
    Assert.assertThat(results.getSize(), equalTo(3));
    JobStatus status = statusable.getStatus();
    Assert.assertThat(status.getState(), equalTo(State.SUCCEEDED));
    Assert.assertThat(status.getPercentComplete(), equalTo(100.0));
    Assert.assertThat(status.getJobId(), equalTo(jobIdVal));
  }

}
