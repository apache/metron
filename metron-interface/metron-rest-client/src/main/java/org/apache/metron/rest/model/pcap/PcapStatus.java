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
package org.apache.metron.rest.model.pcap;

import java.util.Objects;

public class PcapStatus {

  private String jobId;
  private String jobStatus;
  private String description;
  private Double percentComplete = 0.0;
  private Integer pageTotal = 0;

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getJobStatus() {
    return jobStatus;
  }

  public void setJobStatus(String jobStatus) {
    this.jobStatus = jobStatus;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Double getPercentComplete() {
    return percentComplete;
  }

  public void setPercentComplete(Double percentComplete) {
    this.percentComplete = percentComplete;
  }

  public Integer getPageTotal() {
    return pageTotal;
  }

  public void setPageTotal(Integer size) {
    this.pageTotal = size;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PcapStatus that = (PcapStatus) o;
    return Objects.equals(jobId, that.jobId) &&
            Objects.equals(jobStatus, that.jobStatus) &&
            Objects.equals(description, that.description) &&
            Objects.equals(percentComplete, that.percentComplete) &&
            Objects.equals(pageTotal, that.pageTotal);
  }

  @Override
  public int hashCode() {

    return Objects.hash(jobId, jobStatus, description, percentComplete, pageTotal);
  }
}
