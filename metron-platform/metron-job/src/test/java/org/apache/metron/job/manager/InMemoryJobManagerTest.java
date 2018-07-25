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

package org.apache.metron.job.manager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.hadoop.fs.Path;
import org.apache.metron.job.Finalizer;
import org.apache.metron.job.JobException;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.JobStatus.State;
import org.apache.metron.job.Pageable;
import org.apache.metron.job.Statusable;
import org.apache.metron.job.Statusable.JobType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class InMemoryJobManagerTest {

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();
  @Mock
  private Statusable<Path> job1;
  @Mock
  private Statusable<Path> job2;
  @Mock
  private Statusable<Path> job3;
  @Mock
  private Finalizer<Path> finalizer;
  @Mock
  private Pageable<Path> results;
  private JobManager<Path> jm;
  private Map<String, Object> config;
  private String username1;
  private String username2;
  private String jobId1;
  private String jobId2;
  private String jobId3;
  private String emptyJobId;
  private String basePath;

  @Before
  public void setup() throws JobException {
    MockitoAnnotations.initMocks(this);
    jm = new InMemoryJobManager<Path>();
    config = new HashMap<>();
    username1 = "user123";
    username2 = "user456";
    jobId1 = "job_abc_123";
    jobId2 = "job_def_456";
    jobId3 = "job_ghi_789";
    emptyJobId = "";
    basePath = tempDir.getRoot().getAbsolutePath();
    when(job1.getJobType()).thenReturn(JobType.MAP_REDUCE);
    when(job2.getJobType()).thenReturn(JobType.MAP_REDUCE);
    when(job3.getJobType()).thenReturn(JobType.MAP_REDUCE);
    when(job1.submit(finalizer, config)).thenReturn(job1);
    when(job2.submit(finalizer, config)).thenReturn(job2);
    when(job3.submit(finalizer, config)).thenReturn(job3);
    when(finalizer.finalizeJob(any())).thenReturn(results);
  }

  @Test
  public void submits_job_and_returns_status() throws JobException {
    when(job1.getStatus()).thenReturn(new JobStatus().withState(State.RUNNING).withJobId(jobId1));
    JobStatus status = jm.submit(newSupplier(job1), username1);
    assertThat(status.getState(), equalTo(State.RUNNING));
    assertThat(status.getJobId(), equalTo(jobId1));
    when(job1.getStatus()).thenReturn(new JobStatus().withState(State.SUCCEEDED).withJobId(jobId1));
    status = jm.getStatus(username1, status.getJobId());
    assertThat(status.getState(), equalTo(State.SUCCEEDED));
    assertThat(status.getJobId(), equalTo(jobId1));
  }

  @Test
  public void submits_multiple_jobs_and_returns_status() throws JobException {
    when(job1.getStatus()).thenReturn(new JobStatus().withState(State.RUNNING).withJobId(jobId1));
    when(job2.getStatus()).thenReturn(new JobStatus().withState(State.RUNNING).withJobId(jobId2));
    when(job3.getStatus()).thenReturn(new JobStatus().withState(State.RUNNING).withJobId(jobId3));

    // user has 1 job
    jm.submit(newSupplier(job1), username1);
    assertThat(jm.getJob(username1, jobId1), equalTo(job1));

    // user has 2 jobs
    jm.submit(newSupplier(job2), username1);
    assertThat(jm.getJob(username1, jobId1), equalTo(job1));
    assertThat(jm.getJob(username1, jobId2), equalTo(job2));

    // user has 3 jobs
    jm.submit(newSupplier(job3), username1);
    assertThat(jm.getJob(username1, jobId1), equalTo(job1));
    assertThat(jm.getJob(username1, jobId2), equalTo(job2));
    assertThat(jm.getJob(username1, jobId3), equalTo(job3));

    // multiple users have 3 jobs
    jm.submit(newSupplier(job1), username2);
    jm.submit(newSupplier(job2), username2);
    jm.submit(newSupplier(job3), username2);
    // user 1 still good
    assertThat(jm.getJob(username1, jobId1), equalTo(job1));
    assertThat(jm.getJob(username1, jobId2), equalTo(job2));
    assertThat(jm.getJob(username1, jobId3), equalTo(job3));
    // and also user 2
    assertThat(jm.getJob(username2, jobId1), equalTo(job1));
    assertThat(jm.getJob(username2, jobId2), equalTo(job2));
    assertThat(jm.getJob(username2, jobId3), equalTo(job3));
  }

  @Test
  public void empty_result_set_with_empty_jobId_shows_status() throws JobException {
    when(job1.getStatus()).thenReturn(new JobStatus().withState(State.SUCCEEDED).withJobId(emptyJobId));

    // user submits 1 job with empty results
    jm.submit(newSupplier(job1), username1);
    assertThat(jm.getJob(username1, emptyJobId), equalTo(job1));

    // user submits another job with empty results
    when(job2.getStatus()).thenReturn(new JobStatus().withState(State.SUCCEEDED).withJobId(emptyJobId));
    jm.submit(newSupplier(job2), username1);
    assertThat(jm.getJob(username1, emptyJobId), equalTo(job2));
  }

  @Test
  public void returns_job_status() throws JobException {
    JobStatus expected = new JobStatus().withState(State.SUCCEEDED).withJobId(jobId1);
    when(job1.getStatus()).thenReturn(expected);
    jm.submit(newSupplier(job1), username1);
    JobStatus status = jm.getStatus(username1, jobId1);
    assertThat(status, equalTo(expected));
  }

  @Test
  public void returns_job_is_done() throws JobException {
    JobStatus expected = new JobStatus().withState(State.SUCCEEDED).withJobId(jobId1);
    when(job1.getStatus()).thenReturn(expected);
    when(job1.isDone()).thenReturn(true);
    jm.submit(newSupplier(job1), username1);
    boolean done = jm.done(username1, jobId1);
    assertThat(done, equalTo(true));
  }

  @Test
  public void kills_job() throws JobException {
    when(job1.getStatus()).thenReturn(new JobStatus().withState(State.SUCCEEDED).withJobId(jobId1));
    jm.submit(newSupplier(job1), username1);
    jm.killJob(username1, jobId1);
    verify(job1).kill();
  }

  @Test
  public void gets_list_of_user_jobs() throws JobException {
    when(job1.getStatus()).thenReturn(new JobStatus().withState(State.RUNNING).withJobId(jobId1));
    when(job2.getStatus()).thenReturn(new JobStatus().withState(State.RUNNING).withJobId(jobId2));
    when(job3.getStatus()).thenReturn(new JobStatus().withState(State.RUNNING).withJobId(jobId3));
    jm.submit(newSupplier(job1), username1);
    jm.submit(newSupplier(job2), username1);
    jm.submit(newSupplier(job3), username1);
    jm.submit(newSupplier(job1), username2);
    jm.submit(newSupplier(job2), username2);
    jm.submit(newSupplier(job3), username2);
    List<Statusable<Path>> jobsUser1 = jm.getJobs(username1);
    List<Statusable<Path>> jobsUser2 = jm.getJobs(username2);
    assertThat("Wrong size", jobsUser1.size(), equalTo(3));
    assertThat("Wrong size", jobsUser2.size(), equalTo(3));
    assertThat("", jobsUser1.containsAll(Arrays.asList(job1, job2, job3)), equalTo(true));
    assertThat("", jobsUser2.containsAll(Arrays.asList(job1, job2, job3)), equalTo(true));
  }

  private Supplier<Statusable<Path>> newSupplier(Statusable<Path> job) {
    return () -> {
      try {
        return job.submit(finalizer, config);
      } catch (JobException e) {
        throw new RuntimeException("Something went wrong", e);
      }
    };
  }
}
