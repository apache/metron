/*
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

package org.apache.metron.common.field;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Enumerates a set of {@link FieldNameConverter} implementations.
 *
 * <p>Provides shared instances of each {@link FieldNameConverter}.
 *
 * <p>Allows the field name converter to be specified using a short-hand
 * name, rather than the entire fully-qualified class name.
 */
public enum FieldNameConverters implements FieldNameConverter {

  /**
   * A {@link FieldNameConverter} that does not rename any fields.  All field
   * names remain unchanged.
   */
  NOOP(new NoopFieldNameConverter()),

  /**
   * A {@link FieldNameConverter} that replaces all field names containing dots
   * with colons.
   */
  DEDOT(new DeDotFieldNameConverter());

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private FieldNameConverter converter;

  FieldNameConverters(FieldNameConverter converter) {
    this.converter = converter;
  }

  /**
   * Returns a shared instance of the {@link FieldNameConverter}.
   *
   * @return A shared {@link FieldNameConverter} instance.
   */
  public FieldNameConverter get() {
    return converter;
  }

  /**
   * Allows the {@link FieldNameConverters} enums to be used directly as a {@link FieldNameConverter}.
   *
   * {@code
   * FieldNameConverter converter = FieldNameConverters.DEDOT;
   * }
   *
   * @param originalField The original field name.
   * @return the converted field name
   */
  @Override
  public String convert(String originalField) {
    return converter.convert(originalField);
  }

  /**
   * Create a new {@link FieldNameConverter} for a given sensor type and config.
   *
   * @param sensorType The type of sensor.
   * @param config The writer configuration.
   * @return The new {@link FieldNameConverter}
   */
  public static FieldNameConverter create(String sensorType, WriterConfiguration config) {
    FieldNameConverter result = null;

    // which field name converter has been configured?
    String converterName = config.getFieldNameConverter(sensorType);
    if(StringUtils.isNotBlank(converterName)) {
      try {
        result = FieldNameConverters.valueOf(converterName);

      } catch (IllegalArgumentException e) {
        LOG.error("Invalid field name converter, using default; configured={}, knownValues={}, error={}",
                converterName, FieldNameConverters.values(), ExceptionUtils.getRootCauseMessage(e));
      }
    }

    if(result == null) {
      // if no converter defined or an invalid converter is defined, default to 'DEDOT'
      result = FieldNameConverters.DEDOT;
    }

    LOG.debug("Created field name converter; sensorType={}, configured={}, class={}",
            sensorType, converterName, ClassUtils.getShortClassName(result, "null"));

    return result;
  }
}
