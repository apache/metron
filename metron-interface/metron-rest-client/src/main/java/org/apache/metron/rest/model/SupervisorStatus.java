/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.rest.model;

public class SupervisorStatus {
  /*
  /api/v1/supervisor/summary(GET)

  returns all supervisors summary
  Response Fields:
  Field Value Description
  id String Supervisor's id
  host String Supervisor's host name
  upTime String Shows how long the supervisor is running
  slotsTotal Integer Total number of available worker slots for this supervisor
  slotsUsed Integer
  Number of worker slots used on this supervisor

  Sample Response:
  json

  {
    "supervisors": [{
    "id":"0b879808-2a26-442b-8f7d-23101e0c3696", "host":"10.11.1.7",
        "upTime":"5m 58s", "slotsTotal":4, "slotsUsed":3
  } ]}

  */

  private String id;
  private String host;
  private String uptime;
  private int slotsTotal;
  private int slotsUsed;

  public SupervisorStatus() {
  }

  /**
   * Creates a new SupervisorStatus.
   *
   * @param id Supervisor ID
   * @param host Supervisor Host
   * @param upTime the uptime
   * @param slotsTotal The number of slots total
   * @param slotsUsed The number of slots used
   */
  public SupervisorStatus(String id, String host, String upTime, int slotsTotal, int slotsUsed) {
    this.id = id;
    this.host = host;
    this.uptime = upTime;
    this.slotsTotal = slotsTotal;
    this.slotsUsed = slotsUsed;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getUptime() {
    return uptime;
  }

  public void setUptime(String upTime) {
    this.uptime = upTime;
  }

  public int getSlotsTotal() {
    return slotsTotal;
  }

  public void setSlotsTotal(int slotsTotal) {
    this.slotsTotal = slotsTotal;
  }

  public int getSlotsUsed() {
    return slotsUsed;
  }

  public void setSlotsUsed(int slotsUsed) {
    this.slotsUsed = slotsUsed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SupervisorStatus that = (SupervisorStatus) o;

    if (getSlotsTotal() != that.getSlotsTotal()) {
      return false;
    }
    if (getSlotsUsed() != that.getSlotsUsed()) {
      return false;
    }
    if (!getId().equals(that.getId())) {
      return false;
    }
    if (!getHost().equals(that.getHost())) {
      return false;
    }
    return getUptime().equals(that.getUptime());
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (host != null ? host.hashCode() : 0);
    result = 31 * result + (uptime != null ? uptime.hashCode() : 0);
    result = 31 * result + getSlotsTotal();
    result = 31 * result + getSlotsUsed();
    return result;
  }
}
