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
package org.apache.metron.maas.config;

import com.google.common.base.Joiner;

import java.io.Serializable;

public class ModelEndpoint implements Serializable {
  private Endpoint endpoint;
  private String name;
  private String version;
  private String containerId;

  public String getContainerId() {
    return containerId;
  }

  public Endpoint getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(Endpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public String toString() {
    return name + ":" + version + " @ " + endpoint.getUrl()
                + " serving:\n\t" + Joiner.on("\n\t").join(getEndpoint().getFunctions().entrySet());
  }

  public void setContainerId(String containerId) {
    this.containerId = containerId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ModelEndpoint that = (ModelEndpoint) o;

    if (getEndpoint() != null ? !getEndpoint().equals(that.getEndpoint()) : that.getEndpoint() != null) return false;
    if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
    if (getVersion() != null ? !getVersion().equals(that.getVersion()) : that.getVersion() != null) return false;
    return getContainerId() != null ? getContainerId().equals(that.getContainerId()) : that.getContainerId() == null;

  }

  @Override
  public int hashCode() {
    int result = getEndpoint() != null ? getEndpoint().hashCode() : 0;
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
    result = 31 * result + (getContainerId() != null ? getContainerId().hashCode() : 0);
    return result;
  }
}
