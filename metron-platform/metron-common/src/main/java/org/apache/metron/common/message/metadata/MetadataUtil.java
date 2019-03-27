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
import org.apache.metron.common.utils.JSONUtils;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
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
  static final int KEY_INDEX = 1;

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

  /**
   * Default extraction of metadata.  This handles looking in the normal places for metadata
   * <ul>
   *   <li>The kafka key</li>
   *   <li>The tuple fields outside of the value (e.g. the topic)</li>
   * </ul>
   *
   * <p>In addition to extracting the metadata into a map, it applies the appropriate prefix (as configured in the rawMessageStrategyConfig).
   * @param prefix The prefix of the metadata keys
   * @param t The tuple to get metadata from
   * @return A map containing the metadata
   */
  public Map<String, Object> extractMetadata(String prefix, Tuple t) {
    Map<String, Object> metadata = new HashMap<>();
    if(t == null) {
      return metadata;
    }
    Fields tupleFields = t.getFields();
    if(tupleFields == null) {
      return metadata;
    }
    for (int i = 2; i < tupleFields.size(); ++i) {
      String envMetadataFieldName = tupleFields.get(i);
      Object envMetadataFieldValue = t.getValue(i);
      if (!StringUtils.isEmpty(envMetadataFieldName) && envMetadataFieldValue != null) {
        metadata.put(prefixKey(prefix, envMetadataFieldName), envMetadataFieldValue);
      }
    }
    byte[] keyObj = t.getBinary(KEY_INDEX);
    String keyStr = null;
    try {
      keyStr = keyObj == null ? null : new String(keyObj);
      if (!StringUtils.isEmpty(keyStr)) {
        Map<String, Object> rawMetadata = JSONUtils.INSTANCE.load(keyStr, JSONUtils.MAP_SUPPLIER);
        for (Map.Entry<String, Object> kv : rawMetadata.entrySet()) {
          metadata.put(prefixKey(prefix, kv.getKey()), kv.getValue());
        }

      }
    } catch (IOException e) {
      String reason = "Unable to parse metadata; expected JSON Map: " + (keyStr == null ? "NON-STRING!" : keyStr);
      LOG.error(reason, e);
      throw new IllegalStateException(reason, e);
    }
    return metadata;
  }
}
