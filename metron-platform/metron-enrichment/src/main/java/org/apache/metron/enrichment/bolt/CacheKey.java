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
package org.apache.metron.enrichment.bolt;

import org.apache.metron.common.configuration.SensorEnrichmentConfig;

public class CacheKey {
  private String field;
  private String value;
  private SensorEnrichmentConfig config;

  public CacheKey(String field, String value, SensorEnrichmentConfig config) {
    this.field = field;
    this.value = value;
    this.config = config;
  }

  public String getField() {
    return field;
  }

  public String getValue() {
    return value;
  }

  public SensorEnrichmentConfig getConfig() {
    return config;
  }

  @Override
  public String toString() {
    return "CacheKey{" +
            "field='" + field + '\'' +
            ", value='" + value + '\'' +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CacheKey cacheKey = (CacheKey) o;

    if (getField() != null ? !getField().equals(cacheKey.getField()) : cacheKey.getField() != null) return false;
    if (getValue() != null ? !getValue().equals(cacheKey.getValue()) : cacheKey.getValue() != null) return false;
    return config != null ? config.equals(cacheKey.config) : cacheKey.config == null;

  }

  @Override
  public int hashCode() {
    int result = getField() != null ? getField().hashCode() : 0;
    result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
    result = 31 * result + (config != null ? config.hashCode() : 0);
    return result;
  }
}
