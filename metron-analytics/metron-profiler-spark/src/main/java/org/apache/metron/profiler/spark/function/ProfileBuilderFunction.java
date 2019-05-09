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
package org.apache.metron.profiler.spark.function;

import org.apache.metron.profiler.DefaultMessageDistributor;
import org.apache.metron.profiler.MessageDistributor;
import org.apache.metron.profiler.MessageRoute;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.spark.ProfileMeasurementAdapter;
import org.apache.metron.stellar.dsl.Context;
import org.apache.spark.api.java.function.MapGroupsFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.PERIOD_DURATION;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.PERIOD_DURATION_UNITS;
import static org.apache.metron.profiler.spark.function.GroupByPeriodFunction.entityFromKey;
import static org.apache.metron.profiler.spark.function.GroupByPeriodFunction.periodFromKey;
import static org.apache.metron.profiler.spark.function.GroupByPeriodFunction.profileFromKey;

/**
 * The function responsible for building profiles in Spark.
 */
public class ProfileBuilderFunction implements MapGroupsFunction<String, MessageRoute, ProfileMeasurementAdapter>  {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private long periodDurationMillis;
  private Map<String, String> globals;

  public ProfileBuilderFunction(Properties properties, Map<String, String> globals) {
    TimeUnit periodDurationUnits = TimeUnit.valueOf(PERIOD_DURATION_UNITS.get(properties, String.class));
    int periodDuration = PERIOD_DURATION.get(properties, Integer.class);
    this.periodDurationMillis = periodDurationUnits.toMillis(periodDuration);
    this.globals = globals;
  }

  /**
   * Build a profile from a set of message routes.
   *
   * <p>This assumes that all of the necessary routes have been provided
   *
   * @param group The group identifier.
   * @param iterator The message routes.
   * @return
   */
  @Override
  public ProfileMeasurementAdapter call(String group, Iterator<MessageRoute> iterator) {
    // create the distributor; some settings are unnecessary because it is cleaned-up immediately after processing the batch
    int maxRoutes = Integer.MAX_VALUE;
    long profileTTLMillis = Long.MAX_VALUE;
    MessageDistributor distributor = new DefaultMessageDistributor(periodDurationMillis, profileTTLMillis, maxRoutes);
    Context context = TaskUtils.getContext(globals);

    // sort the messages/routes
    List<MessageRoute> routes = toStream(iterator)
            .sorted(comparing(rt -> rt.getTimestamp()))
            .collect(Collectors.toList());
    LOG.debug("Building a profile for group '{}' from {} message(s)", group, routes.size());

    // apply each message/route to build the profile
    for(MessageRoute route: routes) {
      distributor.distribute(route, context);
    }

    // flush the profile
    ProfileMeasurementAdapter result;
    List<ProfileMeasurement> measurements = distributor.flush();
    if(measurements.size() == 1) {
      ProfileMeasurement m = measurements.get(0);
      result = new ProfileMeasurementAdapter(m);
      LOG.debug("Profile measurement created; profile={}, entity={}, period={}, value={}",
              m.getProfileName(), m.getEntity(), m.getPeriod().getPeriod(), m.getProfileValue());

    } else if(measurements.size() == 0) {
      String msg = format("No profile measurement can be calculated. Review the profile for bugs. profile=%s, entity=%s, period=%s",
              profileFromKey(group), entityFromKey(group), periodFromKey(group));
      LOG.error(msg);
      throw new IllegalStateException(msg);

    } else {
      String msg = format("Expected 1 profile measurement, but got %d. profile=%s, entity=%s, period=%s",
              measurements.size(), profileFromKey(group), entityFromKey(group), periodFromKey(group));
      LOG.error(msg);
      throw new IllegalStateException(msg);
    }

    return result;
  }

  private static <T> Stream<T> toStream(Iterator<T> iterator) {
    Iterable<T> iterable = () -> iterator;
    return StreamSupport.stream(iterable.spliterator(), false);
  }
}
