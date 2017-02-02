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
package org.apache.metron.rest.model;

import java.util.List;
import java.util.Map;

public class TopologyStatus {

  private String id;
  private String name;
  private TopologyStatusCode status;
  private Map<String, Object>[] topologyStats;
  private Double latency = 0.0;
  private Double throughput = 0.0;
  private Integer emitted = 0;
  private Integer acked = 0;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TopologyStatusCode getStatus() {
    return status;
  }

  public void setStatus(TopologyStatusCode status) {
    this.status = status;
  }

  public double getLatency() {
    return latency;
  }

  public double getThroughput() {
    return throughput;
  }

  public Integer getEmitted() {
    return emitted;
  }

  public long getAcked() {
    return acked;
  }

  public void setTopologyStats(List<Map<String, Object>> topologyStats) {
    for(Map<String, Object> topologyStatsItem: topologyStats) {
      if ("600".equals(topologyStatsItem.get("window"))) {
        latency = Double.parseDouble((String) topologyStatsItem.get("completeLatency"));
        if (topologyStatsItem.get("acked") != null) {
          acked = (int) topologyStatsItem.get("acked");
        }
        if (topologyStatsItem.get("emitted") != null) {
          emitted= (int) topologyStatsItem.get("emitted");
        }
        throughput = acked / 600.00;
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyStatus that = (TopologyStatus) o;

    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (status != null ? !status.equals(that.status) : that.status != null) return false;
    if (!latency.equals(that.latency)) return false;
    return throughput.equals(that.throughput);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (latency != null ? latency.hashCode() : 0);
    result = 31 * result + (throughput != null ? throughput.hashCode() : 0);
    return result;
  }
}
