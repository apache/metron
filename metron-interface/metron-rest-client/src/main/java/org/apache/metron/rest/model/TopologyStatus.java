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
  private double latency = 0;
  private double throughput = 0;

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

  public void setTopologyStats(List<Map<String, Object>> topologyStats) {
    for(Map<String, Object> topologyStatsItem: topologyStats) {
      if ("600".equals(topologyStatsItem.get("window"))) {
        latency = Double.parseDouble((String) topologyStatsItem.get("completeLatency"));
        int acked = 0;
        if (topologyStatsItem.get("acked") != null) {
          acked = (int) topologyStatsItem.get("acked");
        }
        throughput = acked / 600.00;
      }
    }
  }
}
