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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.profiler.clock.WallClock;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Distributes a message along a MessageRoute.  A MessageRoute will lead to one or
 * more ProfileBuilders.
 *
 * A ProfileBuilder is responsible for maintaining the state of a single profile,
 * for a single entity.  There will be one ProfileBuilder for each (profile, entity) pair.
 * This class ensures that each ProfileBuilder receives the telemetry messages that
 * it needs.
 */
public class DefaultMessageDistributor implements MessageDistributor {

  /**
   * The duration of each profile period in milliseconds.
   */
  private long periodDurationMillis;

  /**
   * Maintains the state of a profile which is unique to a profile/entity pair.
   */
  private transient Cache<String, ProfileBuilder> profileCache;

  /**
   * Create a new message distributor.
   * @param periodDurationMillis The period duration in milliseconds.
   * @param profileTimeToLiveMillis The TTL of a profile in milliseconds.
   */
  public DefaultMessageDistributor(long periodDurationMillis, long profileTimeToLiveMillis) {
    if(profileTimeToLiveMillis < periodDurationMillis) {
      throw new IllegalStateException(format(
              "invalid configuration: expect profile TTL (%d) to be greater than period duration (%d)",
              profileTimeToLiveMillis,
              periodDurationMillis));
    }
    this.periodDurationMillis = periodDurationMillis;
    this.profileCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(profileTimeToLiveMillis, TimeUnit.MILLISECONDS)
            .build();
  }

  /**
   * Distribute a message along a MessageRoute.
   *
   * @param message The message that needs distributed.
   * @param route The message route.
   * @param context The Stellar execution context.
   * @throws ExecutionException
   */
  @Override
  public void distribute(JSONObject message, MessageRoute route, Context context) throws ExecutionException {
    getBuilder(route, context).apply(message);
  }

  /**
   * Flushes all profiles.  Flushes all ProfileBuilders that this distributor is responsible for.
   *
   * @return The profile measurements; one for each (profile, entity) pair.
   */
  @Override
  public List<ProfileMeasurement> flush() {
    List<ProfileMeasurement> measurements = new ArrayList<>();

    profileCache.asMap().forEach((key, profileBuilder) -> {
      if(profileBuilder.isInitialized()) {
        ProfileMeasurement measurement = profileBuilder.flush();
        measurements.add(measurement);
      }
    });

    profileCache.cleanUp();
    return measurements;
  }

  /**
   * Retrieves the cached ProfileBuilder that is used to build and maintain the Profile.  If none exists,
   * one will be created and returned.
   * @param route The message route.
   * @param context The Stellar execution context.
   */
  public ProfileBuilder getBuilder(MessageRoute route, Context context) throws ExecutionException {
    ProfileConfig profile = route.getProfileDefinition();
    String entity = route.getEntity();
    return profileCache.get(
            cacheKey(profile, entity),
            () -> new DefaultProfileBuilder.Builder()
                    .withDefinition(profile)
                    .withEntity(entity)
                    .withPeriodDurationMillis(periodDurationMillis)
                    .withContext(context)
                    .withClock(new WallClock())
                    .build());
  }

  /**
   * Builds the key that is used to lookup the ProfileState within the cache.
   * @param profile The profile definition.
   * @param entity The entity.
   */
  private String cacheKey(ProfileConfig profile, String entity) {
    return format("%s:%s", profile, entity);
  }

  public DefaultMessageDistributor withPeriodDurationMillis(long periodDurationMillis) {
    this.periodDurationMillis = periodDurationMillis;
    return this;
  }

  public DefaultMessageDistributor withPeriodDuration(int duration, TimeUnit units) {
    return withPeriodDurationMillis(units.toMillis(duration));
  }
}
