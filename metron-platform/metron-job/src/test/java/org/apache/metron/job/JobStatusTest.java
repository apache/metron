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

import org.apache.metron.job.JobStatus.State;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobStatusTest {

  @Test
  public void constructor_copies_from_existing_instance() {
    JobStatus original = new JobStatus()
        .withState(State.SUCCEEDED)
        .withCompletionTime(5000)
        .withJobId("abc123")
        .withDescription("All done")
        .withPercentComplete(100.0);
    JobStatus copied = new JobStatus(original);
    assertThat(copied.getState(), equalTo(State.SUCCEEDED));
    assertThat(copied.getCompletionTime(), equalTo(5000L));
    assertThat(copied.getJobId(), equalTo("abc123"));
    assertThat(copied.getDescription(), equalTo("All done"));
    assertThat(copied.getPercentComplete(), equalTo(100.0));
  }

  @Test
  public void failure_info_provided() {
    JobException e = new JobException("The job blew up.");
    JobStatus original = new JobStatus()
        .withState(State.FAILED)
        .withDescription("Failed")
        .withFailureException(e);
    assertThat(original.getFailureReason(), equalTo(e));
  }

}
