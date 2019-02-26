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


  /**
   * Extract the raw message given the strategy, the tuple and the metadata configs.
   * @param strategy The {@link RawMessageStrategy} to use for extraction
   * @param t The tuple to pull the message from
   * @param rawMessage The raw message in bytes
   * @param readMetadata True if read metadata, false otherwise
   * @param config The config to use during extraction
   * @return The resulting {@link RawMessage}
   */
  public RawMessage getRawMessage(RawMessageStrategy strategy, Tuple t, byte[] rawMessage, boolean readMetadata, Map<String, Object> config) {
    Map<String, Object> metadata = new HashMap<>();
    if(readMetadata) {
      String prefix = MetadataUtil.INSTANCE.getMetadataPrefix(config);
      metadata = MetadataUtil.INSTANCE.extractMetadata(prefix, t);
    }
    return strategy.get(metadata, rawMessage, readMetadata, config);
  }


}
