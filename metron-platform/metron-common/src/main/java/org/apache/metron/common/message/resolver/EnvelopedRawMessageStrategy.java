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
package org.apache.metron.common.message.resolver;

import org.apache.metron.common.utils.JSONUtils;
import org.apache.storm.tuple.Tuple;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnvelopedRawMessageStrategy implements RawMessageStrategy {
  public static final String MESSAGE_FIELD_CONFIG = "messageField";

  @Override
  public RawMessage get(Tuple t, byte[] rawMessage, boolean ignoreMetadata, Map<String, Object> config) {
    String messageField = (String)config.get(MESSAGE_FIELD_CONFIG);
    if(messageField == null) {
      throw new IllegalStateException("You must specify a message field in the message supplier config.  " +
              "I expected to find a \"messageField\" field in the config.");
    }
    byte[] envelope = rawMessage;
    try {
      Map<String, Object> rawMetadata = JSONUtils.INSTANCE.load(new String(envelope), JSONUtils.MAP_SUPPLIER);
      String message = (String) rawMetadata.get(messageField);
      if(message != null) {
        //remove the message field from the metadata since it's data, not metadata.
        rawMetadata.remove(messageField);
        return new RawMessage(message.getBytes(), ignoreMetadata?new HashMap<>():rawMetadata);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Expected a JSON Map as the envelope.", e);
    }
    return null;
  }
}
