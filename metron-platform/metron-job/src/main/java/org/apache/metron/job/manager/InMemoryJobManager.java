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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.metron.job.JobException;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.Statusable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryJobManager<PAGE_T> implements JobManager<PAGE_T> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Map<String, Map<String, Statusable<PAGE_T>>> jobs;

  public InMemoryJobManager() {
    this.jobs = Collections.synchronizedMap(new HashMap<>());
  }

  @Override
  public JobStatus submit(Supplier<Statusable<PAGE_T>> jobSupplier, String username)
      throws JobException {
    Map<String, Statusable<PAGE_T>> userJobs = getUserJobs(username);
    Statusable<PAGE_T> job = jobSupplier.get();
    userJobs.put(job.getStatus().getJobId(), job);
    jobs.put(username, userJobs);
    return job.getStatus();
  }

  @Override
  public JobStatus getStatus(String username, String jobId) throws JobException {
    return jobs.get(username).get(jobId).getStatus();
  }

  @Override
  public boolean done(String username, String jobId) throws JobException {
    return getJob(username, jobId).isDone();
  }

  @Override
  public void killJob(String username, String jobId) throws JobException {
    getJob(username, jobId).kill();
  }

  @Override
  public Statusable<PAGE_T> getJob(String username, String jobId) throws JobException {
    return getUserJobs(username).get(jobId);
  }

  private Map<String, Statusable<PAGE_T>> getUserJobs(String username) {
    return jobs.getOrDefault(username, Collections.synchronizedMap(new HashMap<>()));
  }

  @Override
  public List<Statusable<PAGE_T>> getJobs(String username) throws JobException {
    return new ArrayList<Statusable<PAGE_T>>(getUserJobs(username).values());
  }

}
