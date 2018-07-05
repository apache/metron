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

import java.util.Map;
import org.apache.metron.job.JobException;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.Statusable;
import org.apache.metron.job.service.JobService;

public class JobManager<T> {

  private JobService<T> jobs;

  public JobManager(JobService<T> jobs) {
    this.jobs = jobs;
  }

  public JobStatus submit(Statusable<T> job, Map<String, Object> configuration, String username)
      throws JobException {
    JobStatus status = new JobStatus();
    if (job.validate(configuration)) {
      status = job.submit().getStatus();
    }
    jobs.add(job, status.getJobId(), username);
    return status;
  }

  public JobStatus getStatus(String username, String jobId) throws JobException {
    return jobs.getJob(username, jobId).getStatus();
  }

  public boolean done(String username, String jobId) throws JobException {
    return jobs.getJob(username, jobId).isDone();
  }

  public void killJob(String username, String jobId) throws JobException {
    jobs.getJob(username, jobId).kill();
  }

  public Statusable<T> getJob(String username, String jobId) {
    return jobs.getJob(username, jobId);
  }

}
