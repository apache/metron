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


public class Model {
  private String name;
  private String version;
  public Model(String name, String version) {
    setName(name);
    setVersion(version);
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

    Model model = (Model) o;

    if (getName() != null ? !getName().equals(model.getName()) : model.getName() != null) return false;
    return getVersion() != null ? getVersion().equals(model.getVersion()) : model.getVersion() == null;

  }

  @Override
  public int hashCode() {
    int result = getName() != null ? getName().hashCode() : 0;
    result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
    return result;
  }
}
