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

import java.util.List;
import java.util.function.Supplier;
import org.apache.metron.job.JobException;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.Statusable;

public interface JobManager<PAGE_T> {

  JobStatus submit(Supplier<Statusable<PAGE_T>> jobSupplier, String username) throws JobException;

  JobStatus getStatus(String username, String jobId) throws JobException;

  boolean done(String username, String jobId) throws JobException;

  void killJob(String username, String jobId) throws JobException;

  Statusable<PAGE_T> getJob(String username, String jobId) throws JobException;

  List<Statusable<PAGE_T>> getJobs(String username) throws JobException;

}
