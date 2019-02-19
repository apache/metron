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

import java.util.Map;

/**
 * The strategies which we can use to interpret data and metadata.  This fits the normal enum pattern
 * that we use elsewhere for strategy pattern.
 */
public enum RawMessageStrategies implements RawMessageStrategy {
  /**
   * The default strategy.
   */
  DEFAULT(new DefaultRawMessageStrategy()),
  /**
   * Enveloping strategy, used for parser chaining.
   */
  ENVELOPE(new EnvelopedRawMessageStrategy())
  ;
  RawMessageStrategy supplier;
  RawMessageStrategies(RawMessageStrategy supplier) {
    this.supplier = supplier;
  }

  /**
   * Retrieve the raw message given the strategy specified. Note the Javadocs for the individual strategy for more info.
   *
   * @param rawMetadata The metadata read from kafka Key (e.g. the topic, index, etc.)
   * @param originalMessage The raw bytes of the original message
   * @param readMetadata True if we want to read read the metadata
   * @param config The config for the RawMessageStrategy (See the rawMessageStrategyConfig in the SensorParserConfig)
   * @return a {@link RawMessage} based on the given strategy.
   */
  @Override
  public RawMessage get(Map<String, Object> rawMetadata, byte[] originalMessage, boolean readMetadata, Map<String, Object> config) {
    return this.supplier.get(rawMetadata, originalMessage, readMetadata, config);
  }

  /**
   * Merge metadata given the strategy specified. Note the Javadocs for the individual strategy for more info.
   *
   * @param message The parsed message (note: prior to the field transformations)
   * @param metadata The metadata passed along
   * @param mergeMetadata Whether to merge the metadata or not
   * @param config The config for the message strategy.
   */
  @Override
  public void mergeMetadata(JSONObject message, Map<String, Object> metadata, boolean mergeMetadata, Map<String, Object> config) {
    this.supplier.mergeMetadata(message, metadata, mergeMetadata, config);
  }

  @Override
  public boolean mergeMetadataDefault() {
    return this.supplier.mergeMetadataDefault();
  }

  @Override
  public boolean readMetadataDefault() {
    return this.supplier.readMetadataDefault();
  }


}
