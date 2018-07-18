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

    PcapStatus pcapStatus = (PcapStatus) o;

    return (getJobId() != null ? getJobId().equals(pcapStatus.getJobId()) : pcapStatus.getJobId() != null) &&
            (getJobStatus() != null ? getJobStatus().equals(pcapStatus.getJobStatus()) : pcapStatus.getJobStatus() != null) &&
            (getDescription() != null ? getDescription().equals(pcapStatus.getDescription()) : pcapStatus.getDescription() != null) &&
            (getPercentComplete() != null ? getPercentComplete().equals(pcapStatus.getPercentComplete()) : pcapStatus.getPercentComplete() != null) &&
            (getPageTotal() != null ? getPageTotal().equals(pcapStatus.getPageTotal()) : pcapStatus.getPageTotal() != null);
  }

  @Override
  public int hashCode() {
    int result = (getJobId() != null ? getJobId().hashCode() : 0);
    result = 31 * result + (getJobStatus() != null ? getJobStatus().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    result = 31 * result + (getPercentComplete() != null ? getPercentComplete().hashCode() : 0);
    result = 31 * result + (getPageTotal() != null ? getPageTotal().hashCode() : 0);
    return result;
  }
}
