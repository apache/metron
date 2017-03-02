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
package org.apache.metron.rest.mock;

import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.TopologyStatusCode;
import org.apache.metron.rest.service.impl.StormCLIWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MockStormCLIClientWrapper extends StormCLIWrapper {

  private final Map<String, TopologyStatusCode> parsersStatus = new HashMap<>();
  private TopologyStatusCode enrichmentStatus = TopologyStatusCode.TOPOLOGY_NOT_FOUND;
  private TopologyStatusCode indexingStatus = TopologyStatusCode.TOPOLOGY_NOT_FOUND;

  public Set<String> getParserTopologyNames() {
    return parsersStatus.keySet();
  }

  public TopologyStatusCode getParserStatus(String name) {
    TopologyStatusCode parserStatus = parsersStatus.get(name);
    if (parserStatus == null) {
      return TopologyStatusCode.TOPOLOGY_NOT_FOUND;
    } else {
      return parserStatus;
    }
  }

  @Override
  public int startParserTopology(String name) throws RestException {
    TopologyStatusCode parserStatus = parsersStatus.get(name);
    if (parserStatus == null || parserStatus == TopologyStatusCode.TOPOLOGY_NOT_FOUND) {
      parsersStatus.put(name, TopologyStatusCode.ACTIVE);
      return 0;
    } else {
      return 1;
    }
  }

  @Override
  public int stopParserTopology(String name, boolean stopNow) throws RestException {
    TopologyStatusCode parserStatus = parsersStatus.get(name);
    if (parserStatus == TopologyStatusCode.ACTIVE) {
      parsersStatus.put(name, TopologyStatusCode.TOPOLOGY_NOT_FOUND);
      return 0;
    } else {
      return 1;
    }
  }

  public int activateParserTopology(String name) {
    TopologyStatusCode parserStatus = parsersStatus.get(name);
    if (parserStatus == TopologyStatusCode.INACTIVE || parserStatus == TopologyStatusCode.ACTIVE) {
      parsersStatus.put(name, TopologyStatusCode.ACTIVE);
      return 0;
    } else {
      return 1;
    }
  }

  public int deactivateParserTopology(String name) {
    TopologyStatusCode parserStatus = parsersStatus.get(name);
    if (parserStatus == TopologyStatusCode.INACTIVE || parserStatus == TopologyStatusCode.ACTIVE) {
      parsersStatus.put(name, TopologyStatusCode.INACTIVE);
      return 0;
    } else {
      return 1;
    }
  }

  public TopologyStatusCode getEnrichmentStatus() {
    return enrichmentStatus;
  }

  @Override
  public int startEnrichmentTopology() throws RestException {
    if (enrichmentStatus == TopologyStatusCode.TOPOLOGY_NOT_FOUND) {
      enrichmentStatus = TopologyStatusCode.ACTIVE;
      return 0;
    } else {
      return 1;
    }
  }

  @Override
  public int stopEnrichmentTopology(boolean stopNow) throws RestException {
    if (enrichmentStatus == TopologyStatusCode.ACTIVE) {
      enrichmentStatus = TopologyStatusCode.TOPOLOGY_NOT_FOUND;
      return 0;
    } else {
      return 1;
    }
  }

  public int activateEnrichmentTopology() {
    if (enrichmentStatus == TopologyStatusCode.INACTIVE || enrichmentStatus == TopologyStatusCode.ACTIVE) {
      enrichmentStatus = TopologyStatusCode.ACTIVE;
      return 0;
    } else {
      return 1;
    }
  }

  public int deactivateEnrichmentTopology() {
    if (enrichmentStatus == TopologyStatusCode.INACTIVE || enrichmentStatus == TopologyStatusCode.ACTIVE) {
      enrichmentStatus = TopologyStatusCode.INACTIVE;
      return 0;
    } else {
      return 1;
    }
  }

  public TopologyStatusCode getIndexingStatus() {
    return indexingStatus;
  }

  @Override
  public int startIndexingTopology() throws RestException {
    if (indexingStatus == TopologyStatusCode.TOPOLOGY_NOT_FOUND) {
      indexingStatus = TopologyStatusCode.ACTIVE;
      return 0;
    } else {
      return 1;
    }
  }

  @Override
  public int stopIndexingTopology(boolean stopNow) throws RestException {
    if (indexingStatus == TopologyStatusCode.ACTIVE) {
      indexingStatus = TopologyStatusCode.TOPOLOGY_NOT_FOUND;
      return 0;
    } else {
      return 1;
    }
  }

  public int activateIndexingTopology() {
    if (indexingStatus == TopologyStatusCode.INACTIVE || indexingStatus == TopologyStatusCode.ACTIVE) {
      indexingStatus = TopologyStatusCode.ACTIVE;
      return 0;
    } else {
      return 1;
    }
  }

  public int deactivateIndexingTopology() {
    if (indexingStatus == TopologyStatusCode.INACTIVE || indexingStatus == TopologyStatusCode.ACTIVE) {
      indexingStatus = TopologyStatusCode.INACTIVE;
      return 0;
    } else {
      return 1;
    }
  }

  @Override
  protected String stormClientVersionInstalled() throws RestException {
    return "1.0.1";
  }
}
