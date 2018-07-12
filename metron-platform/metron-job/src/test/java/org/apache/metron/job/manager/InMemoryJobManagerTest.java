package org.apache.metron.job.manager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
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
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

public class InMemoryJobManagerTest {

  @Mock
  private Statusable<Path> job1;
  @Mock
  private Statusable<Path> job2;
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

  @Before
  public void setup() throws JobException {
    MockitoAnnotations.initMocks(this);
    jm = new InMemoryJobManager<Path>();
    config = new HashMap<>();
    username1 = "user123";
    username2 = "user456";
    jobId1 = "job_abc_123";
    jobId2 = "job_def_456";
    when(job1.getJobType()).thenReturn(JobType.MAP_REDUCE);
    when(job2.getJobType()).thenReturn(JobType.MAP_REDUCE);
    when(finalizer.finalizeJob(any())).thenReturn(results);
  }

  @Test
  public void submits_job_and_returns_status() throws JobException {
    when(job1.validate(any())).thenReturn(true);
    when(job1.submit(finalizer, config)).thenReturn(job1);
    when(job1.getStatus()).thenReturn(new JobStatus().withState(State.RUNNING).withJobId(jobId1));
    JobStatus status = jm.submit(() -> {
      try {
        return job1.submit(finalizer, config);
      } catch (JobException e) {
        throw new RuntimeException("Something went wrong", e);
      }
    }, username1);
    assertThat(status.getState(), equalTo(State.RUNNING));
    assertThat(status.getJobId(), equalTo(jobId1));
    when(job1.getStatus()).thenReturn(new JobStatus().withState(State.SUCCEEDED).withJobId(jobId1));
    status = jm.getStatus(username1, status.getJobId());
    assertThat(status.getState(), equalTo(State.SUCCEEDED));
    assertThat(status.getJobId(), equalTo(jobId1));
  }

  @Test
  public void returns_job_status() throws JobException {
    JobStatus expected = new JobStatus().withState(State.SUCCEEDED).withJobId(jobId1);
    when(job1.getStatus()).thenReturn(expected);
    JobStatus status = jm.getStatus(username1, jobId1);
    assertThat(status, equalTo(expected));
  }

  @Test
  public void returns_job_is_done() throws JobException {
    JobStatus expected = new JobStatus().withState(State.SUCCEEDED).withJobId(jobId1);
    when(job1.isDone()).thenReturn(true);
    boolean done = jm.done(username1, jobId1);
    assertThat(done, equalTo(true));
  }

  @Test
  public void kills_job() throws JobException {
    jm.killJob(username1, jobId1);
    verify(job1).kill();
  }

  @Test
  public void returns_statusable_job() throws JobException {
    when(job1.isDone()).thenReturn(true);
    Statusable done = jm.getJob(username1, jobId1);
    assertThat(done, equalTo(job1));
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void job_submission_exception_returned() throws JobException {
    JobException expected = new JobException("test exception");
    when(job1.validate(any())).thenReturn(true);
//    when(job.submit()).thenThrow(expected);
    exception.expect(JobException.class);
    exception.expect(equalTo(expected));
//    jm.submit(job, new HashMap<>(), username);
    fail();
  }

}
