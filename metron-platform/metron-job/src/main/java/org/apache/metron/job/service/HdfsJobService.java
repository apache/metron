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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.common.utils.HDFSUtils;
import org.apache.metron.job.Statusable;
import org.apache.metron.job.Statusable.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HDFS-backed implementation of a job service
 */
public class HdfsJobService implements JobService {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Map<String, Map<String, Statusable>> jobs;
  private String basePath;
  private String jobsPath;

  public HdfsJobService() {
    jobs = new HashMap<>();
    basePath = "/apps/metron";
    jobsPath = basePath + "/jobs";
  }

  @Override
  public void configure(Map<String, Object> config) {
    if (config != null) {
      if (config.containsKey("basePath")) {
        basePath = (String) config.get("basePath");
        jobsPath = basePath + "/jobs";
      }
    }
  }

  @Override
  public void add(Statusable job, String username, String jobId) {
    Map<String, Statusable> jobMapping = new HashMap<>();
    jobMapping.put(jobId, job);
    jobs.put(username, jobMapping);
    try {
      writeToHdfs(jobsPath, job.getJobType(), username, jobId);
    } catch (IOException e) {
      LOG.error("Unable to save running job information to HDFS: {}, {}, {}", job.getJobType(),
          username, jobId, e);
    }
  }

  private void writeToHdfs(String basePath, JobType jobType, String username, String jobId)
      throws IOException {
    String path = String.format("%s/%s/%s/%s", basePath, username, jobType.toString(), jobId);
    HDFSUtils.write(new Configuration(), new byte[]{}, path);
  }

  @Override
  public boolean jobExists(String username, String jobId) {
    if (jobs.containsKey(username)) {
      Map<String, Statusable> jobsByUser = jobs.get(username);
      return jobsByUser.containsKey(jobId);
    } else {
      return false;
    }
  }

  @Override
  public Statusable getJob(String username, String jobId) {
    if (jobs.containsKey(username)) {
      Map<String, Statusable> jobsByUser = jobs.get(username);
      return jobsByUser.get(jobId);
    } else {
      return null;
    }
  }

}
