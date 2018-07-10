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
public interface Statusable<T> {

  enum JobType {
    MAP_REDUCE,
    SPARK;
  }

  /**
   * Submit the job.
   *
   * @return self
   */
  Statusable<T> submit() throws JobException;

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
  boolean isDone() throws JobException;

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

  /**
   * Finalize job results. Any post-processing is done here.
   *
   * @return Results in a pageable fashion.
   */
  Pageable<T> finalizeJob() throws JobException;

  /**
   * Gets final results of this job as a Pageable.
   *
   * @return pageable results
   */
  Pageable<T> getFinalResults() throws JobException;

}
