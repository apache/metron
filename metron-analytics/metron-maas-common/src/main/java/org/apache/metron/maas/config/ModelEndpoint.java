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

import java.io.Serializable;

public class ModelEndpoint implements Serializable {
  private String url;
  private String name;
  private String version;
  private String containerId;

  public String getContainerId() {
    return containerId;
  }

  @Override
  public String toString() {
    return "ModelEndpoint{" +
            "url='" + url + '\'' +
            ", name='" + name + '\'' +
            ", version='" + version + '\'' +
            ", containerId='" + containerId + '\'' +
            '}';
  }

  public void setContainerId(String containerId) {
    this.containerId = containerId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
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

    if (getUrl() != null ? !getUrl().equals(that.getUrl()) : that.getUrl() != null) return false;
    if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
    return getVersion() != null ? getVersion().equals(that.getVersion()) : that.getVersion() == null;

  }

  @Override
  public int hashCode() {
    int result = getUrl() != null ? getUrl().hashCode() : 0;
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
    return result;
  }
}
