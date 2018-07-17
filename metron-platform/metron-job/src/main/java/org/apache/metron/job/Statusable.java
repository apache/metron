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

package org.apache.metron.job;

import java.util.Map;

/**
 * Abstraction for getting status on running jobs. Also provides options for killing and validating.
 */
public interface Statusable<PAGE_T> {

  enum JobType {
    MAP_REDUCE;
  }

  /**
   * Submit the job asynchronously.
   *
   * @return self
   */
  Statusable<PAGE_T> submit(Finalizer<PAGE_T> finalizer, Map<String, Object> configuration) throws JobException;

  /**
   * Synchronous call.
   *
   * @return pages of results
   */
  Pageable<PAGE_T> get() throws JobException, InterruptedException;

  /**
   * Execution framework type of this job.
   *
   * @return type of job
   */
  JobType getJobType();

  /**
   * Current job status.
   *
   * @return status
   */
  JobStatus getStatus() throws JobException;

  /**
   * Completion flag.
   *
   * @return true if job is completed, whether KILLED, FAILED, SUCCEEDED. False otherwise.
   */
  boolean isDone();

  /**
   * Kill job.
   */
  void kill() throws JobException;

  /**
   * Validate job after submitted.
   *
   * @param configuration config for validating the job.
   * @return true if job is valid based on passed configuration, false if invalid.
   */
  boolean validate(Map<String, Object> configuration);

}
