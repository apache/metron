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

package org.apache.metron.profiler.client.stellar;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.client.stellar.ProfilerConfig.PROFILER_PERIOD;
import static org.apache.metron.profiler.client.stellar.ProfilerConfig.PROFILER_PERIOD_UNITS;
import static org.apache.metron.profiler.client.stellar.ProfilerConfig.PROFILER_ROW_KEY_BUILDER;
import static org.apache.metron.profiler.client.stellar.ProfilerConfig.PROFILER_SALT_DIVISOR;

/**
 * A Factory class that can create a RowKeyBuilder based on global property values.
 */
public class RowKeyBuilderFactory {

  private static final Logger LOG = LoggerFactory.getLogger(RowKeyBuilderFactory.class);

  /**
   * Create a RowKeyBuilder.
   * @param global The global properties.
   * @return A RowKeyBuilder instantiated using the global property values.
   */
  public static RowKeyBuilder create(Map<String, Object> global) {
    String rowKeyBuilderClass = PROFILER_ROW_KEY_BUILDER.get(global, String.class);
    LOG.debug("profiler client: {}={}", PROFILER_ROW_KEY_BUILDER, rowKeyBuilderClass);

    // instantiate the RowKeyBuilder
    RowKeyBuilder builder = ReflectionUtils.createInstance(rowKeyBuilderClass);
    setSaltDivisor(global, builder);
    setPeriodDuration(global, builder);

    return builder;
  }

  /**
   * Set the period duration on the RowKeyBuilder.
   * @param global The global properties from Zk.
   * @param builder The RowKeyBuilder implementation.
   */
  private static void setPeriodDuration(Map<String, Object> global, RowKeyBuilder builder) {

    // how long is the profile period?
    long duration = PROFILER_PERIOD.get(global, Long.class);
    LOG.debug("profiler client: {}={}", PROFILER_PERIOD, duration);

    // which units are used to define the profile period?
    String configuredUnits = PROFILER_PERIOD_UNITS.get(global, String.class);
    TimeUnit units = TimeUnit.valueOf(configuredUnits);
    LOG.debug("profiler client: {}={}", PROFILER_PERIOD_UNITS, units);

    // set the period duration
    final String periodDurationProperty = "periodDurationMillis";
    setProperty(builder, periodDurationProperty, units.toMillis(duration));
  }

  /**
   * Set the salt divisor property on the RowKeyBuilder.
   * @param global The global properties from Zk.
   * @param builder The RowKeyBuilder implementation.
   */
  private static void setSaltDivisor(Map<String, Object> global, RowKeyBuilder builder) {

    // what is the salt divisor?
    Integer saltDivisor = PROFILER_SALT_DIVISOR.get(global, Integer.class);
    LOG.debug("profiler client: {}={}", PROFILER_SALT_DIVISOR, saltDivisor);

    final String saltDivisorProperty = "saltDivisor";
    setProperty(builder, saltDivisorProperty, saltDivisor);
  }

  /**
   * Set a property on a RowKeyBuilder.
   *
   * A user can dynamically set the RowKeyBuilder implementation to use at runtime.  Different RowKeyBuilder
   * implementations will require different configuration values.  For example, one RowKeyBuilder may need a
   * salt divisor while another may not.  This method will attempt to set the property, only if it is needed.
   *
   * @param builder The RowKeyBuilder to set the property on.
   * @param property The property to set.
   * @param value The value of the property.
   */
  private static void setProperty(RowKeyBuilder builder, String property, Object value) {
    try {
      if(PropertyUtils.isWriteable(builder, property)) {
        PropertyUtils.setProperty(builder, property, value);

      } else {
        LOG.debug(String.format("RowKeyBuilder: Property does not exist: builder='%s', property='%s",
                ClassUtils.getShortClassName(builder, "null"), property));
      }

    } catch(IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      LOG.warn(String.format("RowKeyBuilder: Unable to set property: builder='%s', property='%s'",
              ClassUtils.getShortClassName(builder, "null"), property), e);
    }
  }
}
