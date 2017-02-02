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

public class TopologyResponse {

  private TopologyResponseCode status;
  private String message;

  public TopologyResponseCode getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public void setSuccessMessage(String message) {
    this.status = TopologyResponseCode.SUCCESS;
    this.message = message;
  }

  public void setErrorMessage(String message) {
    this.status = TopologyResponseCode.ERROR;
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyResponse that = (TopologyResponse) o;

    if (status != null ? !status.equals(that.status) : that.status != null) return false;
    return message != null ? message.equals(that.message) : that.message == null;
  }

  @Override
  public int hashCode() {
    int result = status != null ? status.hashCode() : 0;
    result = 31 * result + (message != null ? message.hashCode() : 0);
    return result;
  }
}
