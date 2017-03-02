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

public class TopologySummary {

    private TopologyStatus[] topologies;

    public TopologyStatus[] getTopologies() {
        if (topologies == null) {
            return new TopologyStatus[0];
        }
        return topologies;
    }

    public void setTopologies(TopologyStatus[] topologies) {
        this.topologies = topologies;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TopologySummary that = (TopologySummary) o;

      return topologies != null ? Arrays.equals(topologies, that.topologies) : that.topologies != null;
    }

    @Override
    public int hashCode() {
      return topologies != null ? Arrays.hashCode(topologies) : 0;
    }
}
