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
package org.apache.metron.elasticsearch.writer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.field.DeDotFieldNameConverter;
import org.apache.metron.common.field.FieldNameConverter;
import org.apache.metron.common.field.FieldNameConverters;
import org.apache.metron.common.field.NoopFieldNameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

/**
 * A {@link FieldNameConverterFactory} that is backed by a cache.
 *
 * <p>Each sensor type can use a different {@link FieldNameConverter} implementation.
 *
 * <p>The {@link WriterConfiguration} allows a user to define the {@link FieldNameConverter}
 * that should be used for a given sensor type.
 *
 * <p>The {@link FieldNameConverter}s are maintained in a cache for a fixed period of time
 * after they are created.  Once they expire, the {@link WriterConfiguration} is used to
 * reload the {@link FieldNameConverter}.
 *
 * <p>The user can change the {@link FieldNameConverter} in use at runtime. A change
 * to this configuration is recognized once the old {@link FieldNameConverter} expires
 * from the cache.
 *
 * <p>Defining a shorter expiration interval allows config changes to be recognized more
 * quickly, but also can impact performance negatively.
 */
public class CachedFieldNameConverterFactory implements FieldNameConverterFactory {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * A cache that contains a {@link FieldNameConverter} for each sensor type.
   *
   * A user can alter the {@link FieldNameConverter} for a given sensor at any time
   * by altering the Indexing configuration.  The actual {@link FieldNameConverter}
   * in use for a given sensor will only change once the original converter has
   * expired from the cache.
   */
  private Cache<String, FieldNameConverter> fieldNameConverters;

  /**
   * Creates a {@link CachedFieldNameConverterFactory}.
   *
   * @param expires The duration before {@link FieldNameConverter}s are expired.
   * @param expiresUnits The units before {@link FieldNameConverter}s are expired.
   */
  public CachedFieldNameConverterFactory(int expires, TimeUnit expiresUnits) {

    fieldNameConverters = createFieldNameConverterCache(expires, expiresUnits);
  }

  /**
   * Creates a {@link CachedFieldNameConverterFactory} where the cache expires after 5 minutes.
   */
  public CachedFieldNameConverterFactory() {

    this(5, TimeUnit.MINUTES);
  }

  /**
   * Creates a {@link CachedFieldNameConverterFactory} using the given cache.  This should only
   * be used for testing.
   *
   * @param fieldNameConverters A {@link Cache} containing {@link FieldNameConverter}s.
   */
  public CachedFieldNameConverterFactory(Cache<String, FieldNameConverter> fieldNameConverters) {

    this.fieldNameConverters = fieldNameConverters;
  }

  /**
   * Creates a cache of {@link FieldNameConverter}s, one for each source type.
   *
   * @return A cache of {@link FieldNameConverter}s.
   */
  private Cache<String, FieldNameConverter> createFieldNameConverterCache(int expire, TimeUnit expireUnits) {

    return Caffeine
            .newBuilder()
            .expireAfterWrite(expire, expireUnits)
            .build();
  }

  /**
   * Create a new {@link FieldNameConverter}.
   *
   * @param sensorType The type of sensor.
   * @param config The writer configuration.
   * @return
   */
  @Override
  public FieldNameConverter create(String sensorType, WriterConfiguration config) {

    return fieldNameConverters.get(sensorType, (s) -> createInstance(sensorType, config));
  }

  /**
   * Create a new {@link FieldNameConverter}.
   *
   * @param sensorType The type of sensor.
   * @param config The writer configuration.
   * @return
   */
  private FieldNameConverter createInstance(String sensorType, WriterConfiguration config) {

    // default to the 'DEDOT' field name converter to maintain backwards compatibility
    FieldNameConverter result = new DeDotFieldNameConverter();

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
