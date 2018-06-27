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

import org.apache.hadoop.fs.Path;

/**
 * Capture metadata about a batch job.
 */
public class JobStatus {

  public enum State {
    NOT_RUNNING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    KILLED
  }

  private String jobId;
  private State state = State.NOT_RUNNING;
  private double percentComplete = 0.0;
  private String description;
  private Path resultPath;

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

  public JobStatus withResultPath(Path resultPath) {
    this.resultPath = resultPath;
    return this;
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

  public Path getResultPath() {
    return resultPath;
  }

}
