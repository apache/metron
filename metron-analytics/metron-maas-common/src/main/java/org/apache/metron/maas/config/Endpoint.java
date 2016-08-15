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

import java.util.HashMap;
import java.util.Map;

public class Endpoint {
  String url;
  Map<String, String> endpoints = new HashMap<String, String>(){{
    put("apply", "apply");
  }};

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Map<String, String> getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(Map<String, String> endpoints) {
    this.endpoints = endpoints;
  }


  @Override
  public String toString() {
    return "Endpoint{" +
            "url='" + url + '\'' +
            ", endpoints=" + endpoints +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Endpoint endpoint = (Endpoint) o;

    if (getUrl() != null ? !getUrl().equals(endpoint.getUrl()) : endpoint.getUrl() != null) return false;
    return getEndpoints() != null ? getEndpoints().equals(endpoint.getEndpoints()) : endpoint.getEndpoints() == null;

  }

  @Override
  public int hashCode() {
    int result = getUrl() != null ? getUrl().hashCode() : 0;
    result = 31 * result + (getEndpoints() != null ? getEndpoints().hashCode() : 0);
    return result;
  }
}
