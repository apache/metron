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
package org.apache.metron.common.configuration.enrichment.handler;


import com.google.common.collect.ImmutableList;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface Config {

  /**
   * Split a message by the fields.  Certain configs will do this differently than others, but
   * these are the messages sent to the enrichment adapter downstream.
   *
   * @param message The Json message to be split
   * @param fields The fields to split by
   * @param fieldToEnrichmentKey A function to get the enrichment key
   * @param config The config to use
   * @return A list of Json objects that have been split from the message.
   */
  List<JSONObject> splitByFields( JSONObject message
                          , Object fields
                          , Function<String, String> fieldToEnrichmentKey
                          , Iterable<Map.Entry<String, Object>> config
                          );

  default List<JSONObject> splitByFields( JSONObject message
                          , Object fields
                          , Function<String, String> fieldToEnrichmentKey
                          , ConfigHandler handler
                          ) {
    return splitByFields(message, fields, fieldToEnrichmentKey, handler.getType().toConfig(handler.getConfig()));
  }

  /**
   * Return the subgroups for a given enrichment.  This will allow the join bolt to know when the join is complete.
   * NOTE: this implies that a given enrichment may have a 1 to many relationship with subgroups.
   * @param config An iterable of config entries
   * @return The list of subgroups
   */
  List<String> getSubgroups(Iterable<Map.Entry<String, Object>> config);

  default List<String> getSubgroups(ConfigHandler handler) {
    return getSubgroups(handler.getType().toConfig(handler.getConfig()));
  }

  /**
   * Convert a config object (currently either a map or list is supported) to a list of configs.
   * @param c Either a map or list representing the enrichment adapter configuration.
   * @return an iterable of config entries
   */
  Iterable<Map.Entry<String, Object>> toConfig(Object c);
}
