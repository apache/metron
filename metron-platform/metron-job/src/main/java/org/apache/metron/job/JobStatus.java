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

/**
 * Capture metadata about a batch job.
 */
public class JobStatus {

  private Throwable failureReason;

  public enum State {
    NOT_RUNNING,
    SUBMITTED,
    RUNNING,
    SUCCEEDED,
    FINALIZING,
    FAILED,
    KILLED
  }

  private String jobId;
  private State state;
  private double percentComplete;
  private String description;
  private long completionTime;

  public JobStatus() {
    jobId = "";
    state = State.NOT_RUNNING;
    percentComplete = 0.0;
    description = "Not started";
    completionTime = 0L;
  }

  /**
   * Copy constructor instead of clone. Effective for thread safety, per Goetz JCIP.
   *
   * @param jobStatus Existing JobStatus object to copy state from.
   */
  public JobStatus(JobStatus jobStatus) {
    this.jobId = jobStatus.jobId;
    this.state = jobStatus.state;
    this.percentComplete = jobStatus.percentComplete;
    this.description = jobStatus.description;
    this.completionTime = jobStatus.completionTime;
  }

  public JobStatus withJobId(String jobId) {
    this.jobId = jobId;
    return this;
  }

  public JobStatus withState(State state) {
    this.state = state;
    return this;
  }

  public JobStatus withPercentComplete(double percentComplete) {
    this.percentComplete = percentComplete;
    return this;
  }

  public JobStatus withDescription(String description) {
    this.description = description;
    return this;
  }

  public JobStatus withCompletionTime(long completionTime) {
    this.completionTime = completionTime;
    return this;
  }

  public JobStatus withFailureException(Throwable failureReason) {
    this.failureReason = failureReason;
    return this;
  }

  public String getJobId() {
    return jobId;
  }

  public State getState() {
    return state;
  }

  public double getPercentComplete() {
    return percentComplete;
  }

  public String getDescription() {
    return description;
  }

  public long getCompletionTime() {
    return completionTime;
  }

  /**
   * Null if no failure reason available.
   *
   * @return Throwable indicating failure.
   */
  public Throwable getFailureReason() {
    return failureReason;
  }

}
