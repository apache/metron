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
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class EnvelopedRawMessageStrategy implements RawMessageStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String MESSAGE_FIELD_CONFIG = "messageField";

  @Override
  public RawMessage get(Map<String, Object> rawMetadata, byte[] rawMessage, boolean readMetadata, Map<String, Object> config) {
    String messageField = (String)config.get(MESSAGE_FIELD_CONFIG);
    if(messageField == null) {
      throw new IllegalStateException("You must specify a message field in the message supplier config.  " +
              "I expected to find a \"messageField\" field in the config.");
    }
    byte[] envelope = rawMessage;

    try {
      String prefix = MetadataUtil.INSTANCE.getMetadataPrefix(config);
      Map<String, Object> extraMetadata = JSONUtils.INSTANCE.load(new String(envelope), JSONUtils.MAP_SUPPLIER);
      String message = null;
      if(extraMetadata != null) {
        for(Map.Entry<String, Object> kv : extraMetadata.entrySet()) {
          if(kv.getKey().equals(messageField)) {
            message = (String)kv.getValue();
          }
          rawMetadata.put(MetadataUtil.INSTANCE.prefixKey(prefix, kv.getKey()), kv.getValue());
        }
      }
      if(message != null) {
        if(!readMetadata) {
          LOG.debug("Ignoring metadata; Message: " + message + " rawMetadata: " + rawMetadata + " and field = " + messageField);
          return new RawMessage(message.getBytes(), new HashMap<>());
        }
        else {
          //remove the message field from the metadata since it's data, not metadata.
          rawMetadata.remove(MetadataUtil.INSTANCE.prefixKey(prefix, messageField));
          LOG.debug("Attaching metadata; Message: " + message + " rawMetadata: " + rawMetadata + " and field = " + messageField);
          return new RawMessage(message.getBytes(), rawMetadata);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Expected a JSON Map as the envelope.", e);
    }
    return null;
  }

  @Override
  public void mergeMetadata(JSONObject message, Map<String, Object> metadata, boolean mergeMetadata, Map<String, Object> config) {
    //we want to ensure the original string from the metadata, if provided is used
    String prefix = MetadataUtil.INSTANCE.getMetadataPrefix(config);
    String originalStringFromMetadata = (String)metadata.get(MetadataUtil.INSTANCE.prefixKey(prefix, Constants.Fields.ORIGINAL.getName()));
    if(mergeMetadata) {
      for (Map.Entry<String, Object> kv : metadata.entrySet()) {
        //and that otherwise we prefer fields from the current message, not the metadata
        message.putIfAbsent(kv.getKey(), kv.getValue());
      }
    }
    if(originalStringFromMetadata != null) {
      message.put(Constants.Fields.ORIGINAL.getName(), originalStringFromMetadata);
    }
  }

  @Override
  public boolean mergeMetadataDefault() {
    return true;
  }

  @Override
  public boolean readMetadataDefault() {
    return true;
  }
}
