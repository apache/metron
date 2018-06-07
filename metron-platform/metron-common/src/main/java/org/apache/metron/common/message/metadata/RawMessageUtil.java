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

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public enum RawMessageUtil {

  INSTANCE;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String METADATA_PREFIX = "metron.metadata";
  public static final String METADATA_PREFIX_CONFIG = "metadata_prefix";
  static final int KEY_INDEX = 1;

  public RawMessage getRawMessage(RawMessageStrategy strategy, Tuple t, byte[] rawMessage, boolean ignoreMetadata, Map<String, Object> config) {
    Map<String, Object> metadata = new HashMap<>();
    if(!ignoreMetadata) {
      String prefix = (String) config.getOrDefault(METADATA_PREFIX_CONFIG, METADATA_PREFIX);
      metadata = extractMetadata(prefix, t);
    }
    return strategy.get(metadata, rawMessage, ignoreMetadata, config);
  }

  private Map<String, Object> extractMetadata(String prefix, Tuple t) {
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
        metadata.put(Joiner.on(".").join(prefix, envMetadataFieldName), envMetadataFieldValue);
      }
    }
    byte[] keyObj = t.getBinary(KEY_INDEX);
    String keyStr = null;
    try {
      keyStr = keyObj == null ? null : new String(keyObj);
      if (!StringUtils.isEmpty(keyStr)) {
        Map<String, Object> rawMetadata = JSONUtils.INSTANCE.load(keyStr, JSONUtils.MAP_SUPPLIER);
        for (Map.Entry<String, Object> kv : rawMetadata.entrySet()) {
          metadata.put(Joiner.on(".").join(prefix, kv.getKey()), kv.getValue());
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
