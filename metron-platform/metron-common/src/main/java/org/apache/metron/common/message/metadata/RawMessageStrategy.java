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

import java.io.Serializable;
import java.util.Map;

/**
 * This is a strategy which defines how parsers:
 * <ul>
 *   <li>Define what data constitutes the parseable message</li>
 *   <li>Define what data constitutes the metadata</li>
 * </ul>
 * Also, each strategy has the ability to define its own defaults around whether
 * metadata is read or merged.
 */
public interface RawMessageStrategy extends Serializable {
  /**
   * Retrieve the RawMessage (e.g. the data and metadata) given raw data and raw metadata read from kafka.
   * Note that the base metadata from kafka key and tuples, etc. along with prefixing is handled in the MetadataUtil.
   * This is intended for individual strategies to append OTHER metadata.
   *
   * @param rawMetadata The metadata read from kafka Key (e.g. the topic, index, etc.)
   * @param rawMessage The raw message from the kafka value
   * @param readMetadata True if we want to read read the metadata
   * @param config The config for the RawMessageStrategy (See the rawMessageStrategyConfig in the SensorParserConfig)
   * @return The RawMessage, which defines the data and metadata
   */
  RawMessage get( Map<String, Object> rawMetadata
                , byte[] rawMessage
                , boolean readMetadata
                , Map<String, Object> config
                );

  /**
   * Merge the metadata into the message. Note: Each strategy may merge differently based on their own config.
   *
   * @param message The parsed message (note: prior to the field transformations)
   * @param metadata The metadata passed along
   * @param mergeMetadata Whether to merge the metadata or not
   * @param config The config for the message strategy.
   */
  void mergeMetadata( JSONObject message
                    , Map<String, Object> metadata
                    , boolean mergeMetadata
                    , Map<String, Object> config
                    );

  /**
   * The default value for merging metadata.
   * @return true if default to merge, false otherwise
   */
  boolean mergeMetadataDefault();

  /**
   * The default value for reading metadata.
   * @return true if default to read, false otherwise
   */
  boolean readMetadataDefault();
}
