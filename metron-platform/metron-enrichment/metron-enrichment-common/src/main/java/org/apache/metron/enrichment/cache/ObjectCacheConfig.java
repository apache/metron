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

package org.apache.metron.enrichment.cache;

import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ObjectCacheConfig {

  public static final String OBJECT_CACHE_SIZE_KEY = "object.cache.size";
  public static final String OBJECT_CACHE_EXPIRATION_MINUTES_KEY = "object.cache.expiration.minutes";
  public static final String OBJECT_CACHE_EXPIRATION_KEY = "object.cache.expiration";
  public static final String OBJECT_CACHE_TIME_UNIT_KEY = "object.cache.time.unit";
  public static final String OBJECT_CACHE_MAX_FILE_SIZE_KEY = "object.cache.max.file.size";
  public static final long OBJECT_CACHE_SIZE_DEFAULT = 1000;
  public static final long OBJECT_CACHE_EXPIRATION_MIN_DEFAULT = 1440;
  public static final TimeUnit OBJECT_CACHE_TIME_UNIT_DEFAULT = TimeUnit.MINUTES;
  public static final long OBJECT_CACHE_MAX_FILE_SIZE_DEFAULT = 1048576; // default to 1 mb

  private long cacheSize;
  private long cacheExpiration;
  private TimeUnit timeUnit;
  private long maxFileSize;

  public ObjectCacheConfig(Map<String, Object> config) {
      cacheSize = ConversionUtils.convert(config.getOrDefault(OBJECT_CACHE_SIZE_KEY, OBJECT_CACHE_SIZE_DEFAULT), Long.class);
      if (config.containsKey(OBJECT_CACHE_EXPIRATION_MINUTES_KEY)) {
          cacheExpiration = ConversionUtils.convert(config.getOrDefault(OBJECT_CACHE_EXPIRATION_MINUTES_KEY, OBJECT_CACHE_EXPIRATION_MIN_DEFAULT), Long.class);
          timeUnit = OBJECT_CACHE_TIME_UNIT_DEFAULT;
      } else {
          cacheExpiration = ConversionUtils.convert(config.getOrDefault(OBJECT_CACHE_EXPIRATION_KEY, OBJECT_CACHE_EXPIRATION_MIN_DEFAULT), Long.class);
          timeUnit = config.containsKey(OBJECT_CACHE_TIME_UNIT_KEY) ?
                  TimeUnit.valueOf((String) config.get(OBJECT_CACHE_TIME_UNIT_KEY)) : OBJECT_CACHE_TIME_UNIT_DEFAULT;
      }
      maxFileSize = ConversionUtils.convert(config.getOrDefault(OBJECT_CACHE_MAX_FILE_SIZE_KEY, OBJECT_CACHE_MAX_FILE_SIZE_DEFAULT), Long.class);
  }

  public long getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(long cacheSize) {
    this.cacheSize = cacheSize;
  }

  public long getCacheExpiration() {
    return cacheExpiration;
  }

  public void setCacheExpiration(long cacheExpiration) {
    this.cacheExpiration = cacheExpiration;
  }

  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  public void setTimeUnit(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  public long getMaxFileSize() {
    return maxFileSize;
  }

  public void setMaxFileSize(long maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ObjectCacheConfig that = (ObjectCacheConfig) o;
    return cacheSize == that.cacheSize &&
            cacheExpiration == that.cacheExpiration &&
            timeUnit == that.timeUnit &&
            maxFileSize == that.maxFileSize;
  }

  @Override
  public int hashCode() {

    return Objects.hash(cacheSize, cacheExpiration, timeUnit, maxFileSize);
  }

  @Override
  public String toString() {
    return "ObjectCacheConfig{" +
            "cacheSize=" + cacheSize +
            ", cacheExpiration=" + cacheExpiration +
            ", timeUnit=" + timeUnit +
            ", maxFileSize=" + maxFileSize +
            '}';
  }
}
