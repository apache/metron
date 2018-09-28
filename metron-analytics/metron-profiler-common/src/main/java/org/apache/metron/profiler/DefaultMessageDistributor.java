/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.profiler;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.stellar.dsl.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * The default implementation of a {@link MessageDistributor}.
 *
 * <p>Two caches are maintained; one for active profiles and another for expired
 * profiles.  A profile will remain on the active cache as long as it continues
 * to receive messages.
 *
 * <p>If a profile has not received messages for an extended period of time, it
 * is expired and moved to the expired cache.  A profile that is expired can no
 * longer receive new messages.
 *
 * <p>A profile is stored in the expired cache for a fixed period of time so that
 * a client can flush the state of expired profiles.  If the client does not flush
 * the expired profiles using `flushExpired`, the state of these profiles will be
 * lost.
 *
 */
public class DefaultMessageDistributor implements MessageDistributor, Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The duration of each profile period in milliseconds.
   */
  private long periodDurationMillis;

  /**
   * A cache of active profiles.
   *
   * A profile will remain on the active cache as long as it continues to receive
   * messages.  Once it has not received messages for a period of time, it is
   * moved to the expired cache.
   */
  private Cache<Integer, ProfileBuilder> activeCache;

  /**
   * A cache of expired profiles.
   *
   * When a profile expires from the active cache, it is moved here for a
   * period of time.  In the expired cache a profile can no longer receive
   * new messages.  A profile waits on the expired cache so that the client
   * can flush the state of the expired profile.  If the client does not flush
   * the expired profiles, this state will be lost forever.
   */
  private Cache<Integer, ProfileBuilder> expiredCache;

  /**
   * Create a new message distributor.
   *
   * @param periodDurationMillis The period duration in milliseconds.
   * @param profileTimeToLiveMillis The time-to-live of a profile in milliseconds.
   * @param maxNumberOfRoutes The max number of unique routes to maintain.  After this is exceeded, lesser
   *                          used routes will be evicted from the internal cache.
   */
  public DefaultMessageDistributor(
          long periodDurationMillis,
          long profileTimeToLiveMillis,
          long maxNumberOfRoutes) {
    this(periodDurationMillis, profileTimeToLiveMillis, maxNumberOfRoutes, Ticker.systemTicker());
  }

  /**
   * Create a new message distributor.
   *
   * @param periodDurationMillis The period duration in milliseconds.
   * @param profileTimeToLiveMillis The time-to-live of a profile in milliseconds.
   * @param maxNumberOfRoutes The max number of unique routes to maintain.  After this is exceeded, lesser
   *                          used routes will be evicted from the internal cache.
   * @param ticker The ticker used to drive time for the caches.  Only needs set for testing.
   */
  public DefaultMessageDistributor(
          long periodDurationMillis,
          long profileTimeToLiveMillis,
          long maxNumberOfRoutes,
          Ticker ticker) {

    if(profileTimeToLiveMillis < periodDurationMillis) {
      throw new IllegalStateException(format(
              "invalid configuration: expect profile TTL (%d) to be greater than period duration (%d)",
              profileTimeToLiveMillis,
              periodDurationMillis));
    }
    this.periodDurationMillis = periodDurationMillis;

    // build the cache of active profiles
    this.activeCache = CacheBuilder
            .newBuilder()
            .maximumSize(maxNumberOfRoutes)
            .expireAfterAccess(profileTimeToLiveMillis, TimeUnit.MILLISECONDS)
            .removalListener(new ActiveCacheRemovalListener())
            .ticker(ticker)
            .build();

    // build the cache of expired profiles
    this.expiredCache = CacheBuilder
            .newBuilder()
            .maximumSize(maxNumberOfRoutes)
            .expireAfterWrite(profileTimeToLiveMillis, TimeUnit.MILLISECONDS)
            .removalListener(new ExpiredCacheRemovalListener())
            .ticker(ticker)
            .build();
  }

  /**
   * Distribute a message along a MessageRoute.
   *
   * @param route The message route.
   * @param context The Stellar execution context.
   */
  @Override
  public void distribute(MessageRoute route, Context context) {
    try {
      ProfileBuilder builder = getBuilder(route, context);
      builder.apply(route.getMessage(), route.getTimestamp());

    } catch(ExecutionException e) {
      LOG.error("Unexpected error", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Flush all active profiles.
   *
   * <p>A profile will remain active as long as it continues to receive messages.  If a profile
   * does not receive a message for an extended duration, it may be marked as expired.
   *
   * <p>Flushes all active {@link ProfileBuilder} objects that this distributor is responsible for.
   *
   * @return The {@link ProfileMeasurement} values; one for each (profile, entity) pair.
   */
  @Override
  public List<ProfileMeasurement> flush() {
    LOG.debug("About to flush active profiles");

    // cache maintenance needed here to ensure active profiles will expire
    cacheMaintenance();

    List<ProfileMeasurement> measurements = flushCache(activeCache);
    return measurements;
  }

  /**
   * Flush all expired profiles.
   *
   * <p>Flushes all expired {@link ProfileBuilder}s that this distributor is responsible for.
   *
   * <p>If a profile has not received messages for an extended period of time, it will be marked as
   * expired.  When a profile is expired, it can no longer receive new messages.  Expired profiles
   * remain only to give the client a chance to flush them.
   *
   * <p>If the client does not flush the expired profiles periodically, any state maintained in the
   * profile since the last flush may be lost.
   *
   * @return The {@link ProfileMeasurement} values; one for each (profile, entity) pair.
   */
  @Override
  public List<ProfileMeasurement> flushExpired() {
    LOG.debug("About to flush expired profiles");

    // cache maintenance needed here to ensure active profiles will expire
    cacheMaintenance();

    // flush all expired profiles
    List<ProfileMeasurement> measurements = flushCache(expiredCache);

    // once the expired profiles have been flushed, they are no longer needed
    expiredCache.invalidateAll();

    return measurements;
  }

  /**
   * Performs cache maintenance on both the active and expired caches.
   */
  private void cacheMaintenance() {
    activeCache.cleanUp();
    expiredCache.cleanUp();

    LOG.debug("Cache maintenance complete: activeCacheSize={}, expiredCacheSize={}", activeCache.size(), expiredCache.size());
  }

  /**
   * Flush all of the profiles maintained in a cache.
   *
   * @param cache The cache to flush.
   * @return The measurements captured when flushing the profiles.
   */
  private List<ProfileMeasurement> flushCache(Cache<Integer, ProfileBuilder> cache) {

    List<ProfileMeasurement> measurements = new ArrayList<>();
    for(ProfileBuilder profileBuilder: cache.asMap().values()) {

      // only need to flush, if the profile has been initialized
      if(profileBuilder.isInitialized()) {

        // flush the profiler and save the measurement, if one exists
        Optional<ProfileMeasurement> measurement = profileBuilder.flush();
        measurement.ifPresent(m -> measurements.add(m));
      }
    }

    return measurements;
  }

  /**
   * Retrieves the cached ProfileBuilder that is used to build and maintain the Profile.  If none exists,
   * one will be created and returned.
   *
   * @param route The message route.
   * @param context The Stellar execution context.
   */
  public ProfileBuilder getBuilder(MessageRoute route, Context context) throws ExecutionException {
    ProfileConfig profile = route.getProfileDefinition();
    String entity = route.getEntity();
    return activeCache.get(
            cacheKey(profile, entity),
            () -> new DefaultProfileBuilder.Builder()
                    .withDefinition(profile)
                    .withEntity(entity)
                    .withPeriodDurationMillis(periodDurationMillis)
                    .withContext(context)
                    .build());
  }

  /**
   * Builds the key that is used to lookup the {@link ProfileBuilder} within the cache.
   *
   * <p>The cache key is built using the hash codes of the profile and entity name.  If the profile
   * definition is ever changed, the same cache entry will not be reused.  This ensures that no
   * state can be carried over from the old definition into the new, which might result in an
   * invalid profile measurement.
   *
   * @param profile The profile definition.
   * @param entity The entity.
   */
  private int cacheKey(ProfileConfig profile, String entity) {
    return new HashCodeBuilder(17, 37)
            .append(profile)
            .append(entity)
            .hashCode();
  }

  public DefaultMessageDistributor withPeriodDurationMillis(long periodDurationMillis) {
    this.periodDurationMillis = periodDurationMillis;
    return this;
  }

  public DefaultMessageDistributor withPeriodDuration(int duration, TimeUnit units) {
    return withPeriodDurationMillis(units.toMillis(duration));
  }

  /**
   * A listener that is notified when profiles expire from the active cache.
   */
  private class ActiveCacheRemovalListener implements RemovalListener<Integer, ProfileBuilder>, Serializable {

    @Override
    public void onRemoval(RemovalNotification<Integer, ProfileBuilder> notification) {

      ProfileBuilder expired = notification.getValue();
      LOG.warn("Profile expired from active cache; profile={}, entity={}",
              expired.getDefinition().getProfile(),
              expired.getEntity());

      // add the profile to the expired cache
      expiredCache.put(notification.getKey(), expired);
    }
  }

  /**
   * A listener that is notified when profiles expire from the active cache.
   */
  private class ExpiredCacheRemovalListener implements RemovalListener<Integer, ProfileBuilder>, Serializable {

    @Override
    public void onRemoval(RemovalNotification<Integer, ProfileBuilder> notification) {

      if(notification.wasEvicted()) {

        // the expired profile was NOT flushed in time
        ProfileBuilder expired = notification.getValue();
        LOG.warn("Expired profile NOT flushed before removal, some state lost; profile={}, entity={}",
                expired.getDefinition().getProfile(),
                expired.getEntity());

      } else {

        // the expired profile was flushed successfully
        ProfileBuilder expired = notification.getValue();
        LOG.debug("Expired profile successfully flushed; profile={}, entity={}",
                expired.getDefinition().getProfile(),
                expired.getEntity());
      }
    }
  }
}
