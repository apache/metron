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

import java.util.Arrays;

public class SupervisorSummary {

  private SupervisorStatus[] supervisors;

  public SupervisorSummary(){}
  public SupervisorSummary(SupervisorStatus[] supervisors) {
    this.supervisors = supervisors;
  }

  public SupervisorStatus[] getSupervisors() {
    return supervisors;
  }

  public void setSupervisors(SupervisorStatus[] supervisors) {
    this.supervisors = supervisors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SupervisorSummary that = (SupervisorSummary) o;

    return supervisors != null ? Arrays.equals(supervisors, that.supervisors) : that.supervisors != null;
  }

  @Override
  public int hashCode() {
    return supervisors != null ? Arrays.hashCode(supervisors) : 0;
  }
}
