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
package org.apache.metron.common.message.metadata;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * Captures some common utility methods around metadata manipulation.
 */
public enum MetadataUtil {
  INSTANCE;
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * The default metadata prefix.
   */
  public static final String METADATA_PREFIX = "metron.metadata";
  /**
   * The config key for defining the prefix.
   */
  public static final String METADATA_PREFIX_CONFIG = "metadataPrefix";

  /**
   * Return the prefix that we want to use for metadata keys.  This comes from the config and is defaulted to
   * 'metron.metadata'.
   *
   * @param config The rawMessageStrategyConfig
   * @return the prefix for metadata keys
   */
  public String getMetadataPrefix(Map<String, Object> config) {
    String prefix = (String) config.getOrDefault(METADATA_PREFIX_CONFIG, METADATA_PREFIX);
    if(StringUtils.isEmpty(prefix)) {
      return null;
    }
    return prefix;
  }

  /**
   * Take a field and prefix it with the metadata key.
   *
   * @param prefix The metadata prefix to use (e.g. 'foo')
   * @param key The key name (e.g. my_field)
   * @return The prefixed key separated by a . (e.g. foo.my_field)
   */
  public String prefixKey(String prefix, String key) {
    if(StringUtils.isEmpty(prefix)) {
      return key;
    }
    else {
      return prefix + "." + key;
    }
  }
}
