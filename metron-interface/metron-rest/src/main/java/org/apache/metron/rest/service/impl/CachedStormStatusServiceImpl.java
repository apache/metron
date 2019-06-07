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
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.metron.rest.model.SupervisorSummary;
import org.apache.metron.rest.model.TopologyResponse;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.model.TopologySummary;
import org.apache.metron.rest.service.StormStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator around the Storm status service that caches results.
 */
public class CachedStormStatusServiceImpl implements StormStatusService {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private enum CacheKey {
    SUPERVISOR_SUMMARY,
    TOPOLOGY_SUMMARY,
    TOPOLOGY_STATUS,
    ALL_TOPOLOGY_STATUS
  }

  private StormStatusService stormService;
  // cache is thread-safe
  // key will be a base CacheKey or the base + a suffix. See for example getTopologyStatus(String name).
  private Cache<Object, Object> statusCache;

  /**
   *
   * @param stormService service to decorate and delegate calls to.
   * @param maxCacheSize max number of records in the backing cache.
   * @param cacheExpirationSeconds number of seconds before the cache will expire an entry.
   */
  public CachedStormStatusServiceImpl(StormStatusService stormService, long maxCacheSize,
      long cacheExpirationSeconds) {
    this.stormService = stormService;
    LOG.info("Creating Storm service cache with max size '{}', record expiration seconds '{}'",
        maxCacheSize, cacheExpirationSeconds);
    Caffeine builder = Caffeine.newBuilder().maximumSize(maxCacheSize)
        .expireAfterWrite(cacheExpirationSeconds, TimeUnit.SECONDS);
    statusCache = builder.build();
  }

  @Override
  public SupervisorSummary getSupervisorSummary() {
    return (SupervisorSummary) statusCache
        .get(CacheKey.SUPERVISOR_SUMMARY, cacheKey -> {
          LOG.debug("Loading new supervisor summary");
          return stormService.getSupervisorSummary();
        });
  }

  @Override
  public TopologySummary getTopologySummary() {
    return (TopologySummary) statusCache
        .get(CacheKey.TOPOLOGY_SUMMARY, cacheKey -> {
          LOG.debug("Loading new topology summary");
          return stormService.getTopologySummary();
        });
  }

  /**
   * Rather than worry about coalescing individual topology statuses with the call to get all topology statuses,
   * we handle them independently.
   * @param name topology name.
   * @return status for this topolopgy.
   */
  @Override
  public TopologyStatus getTopologyStatus(String name) {
    return (TopologyStatus) statusCache
        .get(CacheKey.TOPOLOGY_STATUS + name, cacheKey -> {
          LOG.debug("Loading new topology status for '{}'", name);
          return stormService.getTopologyStatus(name);
        });
  }

  @Override
  public List<TopologyStatus> getAllTopologyStatus() {
    return (List<TopologyStatus>) statusCache
        .get(CacheKey.ALL_TOPOLOGY_STATUS, cacheKey -> {
          LOG.debug("Loading all topology status");
          return stormService.getAllTopologyStatus();
        });
  }

  @Override
  public TopologyResponse activateTopology(String name) {
    return stormService.activateTopology(name);
  }

  @Override
  public TopologyResponse deactivateTopology(String name) {
    return stormService.deactivateTopology(name);
  }

  /**
   * Resets the cache, i.e. empties it.
   */
  public void reset() {
    statusCache.invalidateAll();
  }
}
