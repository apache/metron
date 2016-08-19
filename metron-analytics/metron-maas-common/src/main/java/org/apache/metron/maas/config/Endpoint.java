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

/**
 * An endpoint for a service being deployed.  An endpoint is constituted by a URL and
 * a set of functions which it exposes.
 */
public class Endpoint {
  String url;
  Map<String, String> functions = new HashMap<String, String>(){{
    put("apply", "apply");
  }};

  /**
   * Retrieve the URL associated with the endpoint
   * @return a URL
   */
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Retrieve the functions (or functions) exposed.  The key of the map is the logical alias
   * for an endpoint function that you can use across multiple services (i.e. 'apply')
   * and the value is the realized endpoint function name.
   *
   * For instance, if I have a service bound to localhost:8080 and exposes /foo and /bar and
   * for your organization or use you've decided to call functions which are used for enrichment 'enrich'
   * and functions which are used for data science as 'ds', you can alias /foo to 'enrich'
   * and /bar to 'ds' by creating a map which maps the 'enrich' to 'foo' and 'ds' to 'bar'.
   *
   * @return
   */
  public Map<String, String> getFunctions() {
    return functions;
  }

  public void setFunctions(Map<String, String> functions) {
    this.functions = functions;
  }


  @Override
  public String toString() {
    return "Endpoint{" +
            "url='" + url + '\'' +
            ", functions=" + functions +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Endpoint endpoint = (Endpoint) o;

    if (getUrl() != null ? !getUrl().equals(endpoint.getUrl()) : endpoint.getUrl() != null) return false;
    return getFunctions() != null ? getFunctions().equals(endpoint.getFunctions()) : endpoint.getFunctions() == null;

  }

  @Override
  public int hashCode() {
    int result = getUrl() != null ? getUrl().hashCode() : 0;
    result = 31 * result + (getFunctions() != null ? getFunctions().hashCode() : 0);
    return result;
  }
}
