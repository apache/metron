package org.apache.metron.job.manager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import org.apache.hadoop.fs.Path;
import org.apache.metron.job.JobException;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.JobStatus.State;
import org.apache.metron.job.Statusable;
import org.apache.metron.job.service.JobService;
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

public class JobManagerTest {

  private JobManager<Path> jm;
  @Mock
  private JobService<Path> jobService;
  @Mock
  private Statusable job;
  private String username;
  private String jobId;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    jm = new JobManager<Path>(jobService);
    username = "userabc";
    jobId = "job_abc";
  }

  @Test
  public void submits_job_and_returns_status() throws JobException {
    when(job.validate(any())).thenReturn(true);
    when(job.submit()).thenReturn(job);
    when(job.getStatus()).thenReturn(new JobStatus().withState(State.SUCCEEDED).withJobId(jobId));
    JobStatus status = jm.submit(job, new HashMap<>(), username);
    verify(jobService).add(job, jobId, username);
    assertThat(status.getState(), equalTo(State.SUCCEEDED));
  }

  @Test
  public void returns_job_status() throws JobException {
    JobStatus expected = new JobStatus().withState(State.SUCCEEDED).withJobId(jobId);
    when(job.getStatus()).thenReturn(expected);
    when(jobService.getJob(username, jobId)).thenReturn(job);
    JobStatus status = jm.getStatus(username, jobId);
    assertThat(status, equalTo(expected));
  }

  @Test
  public void returns_job_is_done() throws JobException {
    JobStatus expected = new JobStatus().withState(State.SUCCEEDED).withJobId(jobId);
    when(job.isDone()).thenReturn(true);
    when(jobService.getJob(username, jobId)).thenReturn(job);
    boolean done = jm.done(username, jobId);
    assertThat(done, equalTo(true));
  }

  @Test
  public void kills_job() throws JobException {
    when(jobService.getJob(username, jobId)).thenReturn(job);
    jm.killJob(username, jobId);
    verify(job).kill();
  }

  @Test
  public void returns_statusable_job() throws JobException {
    when(job.isDone()).thenReturn(true);
    when(jobService.getJob(username, jobId)).thenReturn(job);
    Statusable done = jm.getJob(username, jobId);
    assertThat(done, equalTo(job));
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void job_submission_exception_returned() throws JobException {
    JobException expected = new JobException("test exception");
    when(job.validate(any())).thenReturn(true);
    when(job.submit()).thenThrow(expected);
    exception.expect(JobException.class);
    exception.expect(equalTo(expected));
    jm.submit(job, new HashMap<>(), username);
  }

}
