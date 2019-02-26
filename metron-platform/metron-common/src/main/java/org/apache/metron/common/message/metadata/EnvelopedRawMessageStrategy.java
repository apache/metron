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

import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * An alternative strategy whereby
 * <ul>
 *  <li>The raw data is presumed to be a JSON Map</li>
 *  <li>The data to be parsed is the contents of one of the fields.</li>
 *  <li>The non-data fields are considered metadata</li>
 * </ul>
 *
 * <p>Additionally, the defaults around merging and reading metadata are adjusted to be on by default.
 * Note, this strategy allows for parser chaining and for a fully worked example, check the parser chaining use-case.
 */
public class EnvelopedRawMessageStrategy implements RawMessageStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * The field from the rawMessageStrategyConfig in the SensorParserConfig that defines the field to use to
   * define the data to be parsed.
   */
  public static final String MESSAGE_FIELD_CONFIG = "messageField";

  /**
   * Retrieve the raw message by parsing the JSON Map in the kafka value and pulling the appropriate field.
   * Also, augment the default metadata with the non-data fields in the JSON Map.
   *
   * <p>Note: The data field in the JSON Map is not considered metadata.
   *
   * @param rawMetadata The metadata read from kafka Key (e.g. the topic, index, etc.)
   * @param rawMessage The raw message from the kafka value
   * @param readMetadata True if we want to read read the metadata
   * @param config The config for the RawMessageStrategy (See the rawMessageStrategyConfig in the SensorParserConfig)
   * @return The {@link RawMessage}, potentially including metadata
   */
  @Override
  public RawMessage get(Map<String, Object> rawMetadata, byte[] rawMessage, boolean readMetadata, Map<String, Object> config) {
    String messageField = (String)config.get(MESSAGE_FIELD_CONFIG);
    if(messageField == null) {
      throw new IllegalStateException("You must specify a message field in the message supplier config.  " +
              "\"messageField\" field was expected but wasn't in the config.");
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

  /**
   * Merge the metadata into the original message.  The strategy around duplicate keys is as follows:
   * <ul>
   *   <li>If the string is the "original_string" field, then we choose the oldest original string</li>
   *   <li>For all other fields, the fields from the message hold precidence against metadata fields on collision.</li>
   * </ul>
   * @param message The parsed message (note: prior to the field transformations)
   * @param metadata The metadata passed along
   * @param mergeMetadata Whether to merge the metadata or not
   * @param config The config for the message strategy.
   */
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

  /**
   * By default merge metadata.
   *
   * @return true
   */
  @Override
  public boolean mergeMetadataDefault() {
    return true;
  }

  /**
   * By default read metadata.
   * @return true
   */
  @Override
  public boolean readMetadataDefault() {
    return true;
  }
}
