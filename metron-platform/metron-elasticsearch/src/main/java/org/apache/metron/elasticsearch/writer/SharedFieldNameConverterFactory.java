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
package org.apache.metron.elasticsearch.writer;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.field.FieldNameConverter;
import org.apache.metron.common.field.FieldNameConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * A {@link FieldNameConverterFactory} that returns instances of {@link FieldNameConverter}
 * that are shared and reused.
 *
 * <p>The instances are created and managed by the {@link FieldNameConverters} class.
 */
public class SharedFieldNameConverterFactory implements FieldNameConverterFactory {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Create a new {@link FieldNameConverter}.
   *
   * @param sensorType The type of sensor.
   * @param config The writer configuration.
   * @return
   */
  @Override
  public FieldNameConverter create(String sensorType, WriterConfiguration config) {

    // default to the 'DEDOT' field name converter to maintain backwards compatibility
    FieldNameConverter result = FieldNameConverters.DEDOT.get();

    // which field name converter should be used?
    String converterName = config.getFieldNameConverter(sensorType);
    if(StringUtils.isNotBlank(converterName)) {

      try {
        result = FieldNameConverters.valueOf(converterName).get();

      } catch(IllegalArgumentException e) {
        LOG.error("Unable to create field name converter, using default; value={}, error={}",
                converterName, ExceptionUtils.getRootCauseMessage(e));
      }
    }

    LOG.debug("Created field name converter; sensorType={}, configuredName={}, class={}",
            sensorType, converterName, ClassUtils.getShortClassName(result, "null"));

    return result;
  }
}
