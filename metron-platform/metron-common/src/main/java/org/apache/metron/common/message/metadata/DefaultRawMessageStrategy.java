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

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * The default implementation. Defines:
 * <ul>
 *   <li>Metadata: The data which comes in via the
 *   kafka key and the other bits of the tuple from the storm spout (e.g. the topic, etc).
 *   </li>
 *   <li>Data: The byte[] that comes across as the kafka value</li>
 * </ul>
 */
public class DefaultRawMessageStrategy implements RawMessageStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  /**
   * The default behavior is to use the raw data from kafka value as the message and the raw metadata as the metadata.
   *
   * @param rawMetadata The metadata read from kafka Key (e.g. the topic, index, etc.)
   * @param rawMessage The raw message from the kafka value
   * @param readMetadata True if we want to read read the metadata
   * @param config The config for the RawMessageStrategy (See the rawMessageStrategyConfig in the SensorParserConfig)
   * @return The {@link RawMessage} that includes metadata.
   */
  @Override
  public RawMessage get(Map<String, Object> rawMetadata, byte[] rawMessage, boolean readMetadata, Map<String, Object> config) {
    return new RawMessage(rawMessage, rawMetadata);
  }

  /**
   * Simple merging of metadata by adding the metadata into the message (if mergeMetadata is set to true).
   *
   * @param message The parsed message (note: prior to the field transformations)
   * @param metadata The metadata passed along
   * @param mergeMetadata Whether to merge the metadata or not
   * @param config The config for the message strategy.
   */
  @Override
  public void mergeMetadata(JSONObject message, Map<String, Object> metadata, boolean mergeMetadata, Map<String, Object> config) {
    if(mergeMetadata) {
      message.putAll(metadata);
    }
  }

  /**
   * The default mergeMetadata is false.
   * @return false
   */
  @Override
  public boolean mergeMetadataDefault() {
    return false;
  }

  /**
   * The default readMetadata is false.
   * @return false
   */
  @Override
  public boolean readMetadataDefault() {
    return false;
  }
}
