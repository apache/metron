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

package org.apache.metron.rest.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.metron.rest.model.SupervisorSummary;
import org.apache.metron.rest.model.TopologyResponse;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.model.TopologySummary;
import org.apache.metron.rest.service.StormStatusService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Decorator around the Storm status service that caches results.
 */
public class CachedStormStatusServiceImpl implements StormStatusService {

  private enum CacheKey {
    SUPERVISOR_SUMMARY,
    TOPOLOGY_SUMMARY,
    TOPOLOGY_STATUS,
    ALL_TOPOLOGY_STATUS
  }

  @Autowired
  private StormStatusService stormService;
  private Cache<CacheKey, Object> statusCache;

  public CachedStormStatusServiceImpl(StormStatusService stormService) {
    this.stormService = stormService;
    Caffeine builder = Caffeine.newBuilder().maximumSize(100)
        .expireAfterWrite(30, TimeUnit.SECONDS);
//        .executor(Executors.newFixedThreadPool(1));
    statusCache = builder.build();
  }

  @Override
  public SupervisorSummary getSupervisorSummary() {
    return (SupervisorSummary) statusCache
        .get(CacheKey.SUPERVISOR_SUMMARY, cacheKey -> stormService.getSupervisorSummary());
  }

  @Override
  public TopologySummary getTopologySummary() {
    statusCache.get()
    return null;
  }

  @Override
  public TopologyStatus getTopologyStatus(String name) {
    return null;
  }

  @Override
  public List<TopologyStatus> getAllTopologyStatus() {
    return null;
  }

  @Override
  public TopologyResponse activateTopology(String name) {
    return null;
  }

  @Override
  public TopologyResponse deactivateTopology(String name) {
    return null;
  }

  /**
   * Resets the cache
   */
  public void reset() {
    statusCache.invalidateAll();
  }
}
