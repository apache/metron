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

package org.apache.metron.job.service;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.apache.metron.job.Statusable;
import org.apache.metron.job.Statusable.JobType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HdfsJobServiceTest {

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Mock
  private Statusable job1;
  @Mock
  private Statusable job2;
  private String username;
  private String jobId1;
  private String jobId2;
  private String basePath;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    username = "user123";
    jobId1 = "job_abc_123";
    jobId2 = "job_def_456";
    when(job1.getJobType()).thenReturn(JobType.MAP_REDUCE);
    when(job2.getJobType()).thenReturn(JobType.MAP_REDUCE);
    basePath = tempDir.getRoot().getAbsolutePath();
    Map<String, Object> config = new HashMap<>();
    config.put("basePath", basePath);
  }
/*
  @Test
  public void adds_jobs() {
    js.add(job1, username, jobId1);
    Statusable actual = js.getJob(username, jobId1);
    assertThat("Job 1 should exist", actual, equalTo(job1));
    js.add(job2, username, jobId2);
    actual = js.getJob(username, jobId1);
    assertThat("Job 1 should still exist after adding job 2", actual, equalTo(job1));
    actual = js.getJob(username, jobId2);
    assertThat("Job 2 should exist", actual, equalTo(job2));
  }

  @Test
  public void job_exists_true_for_submitted_jobs() {
    js.add(job1, username, jobId1);
    js.add(job2, username, jobId2);
    boolean actual = js.jobExists(username, jobId1);
    assertThat("Job 1 should exist", actual, equalTo(true));
    actual = js.jobExists(username, jobId2);
    assertThat("Job 2 should exist", actual, equalTo(true));
  }

  @Test
  public void job_exists_false_for_non_existent_jobs() {
    boolean actual = js.jobExists(username, jobId1);
    assertThat(actual, equalTo(false));
    js.add(job1, username, jobId1);
    actual = js.jobExists(username, "this_job_id_does_not_exist");
    assertThat(actual, equalTo(false));
  }

  @Test
  public void returns_null_for_non_existent_job() {
    Statusable actual = js.getJob(username, jobId1);
    assertThat(actual, equalTo(null));
    js.add(job1, username, jobId1);
    actual = js.getJob(username, "this_job_id_does_not_exist");
    assertThat(actual, equalTo(null));
  }

  @Test
  public void writes_job_info_to_hdfs() {
    // /base/path/jobs/metron_user/MAP_REDUCE/job_abc_123
    js.add(job1, username, jobId1);
    File jobFile = new File(String.format("%s/jobs/%s/%s/%s", basePath, username, JobType.MAP_REDUCE.toString(),
        jobId1));
    assertThat("File should exist", jobFile.exists(), equalTo(true));
    assertThat("File should be a file", jobFile.isFile(), equalTo(true));
  }
*/
}
