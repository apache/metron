/*
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

package org.apache.metron.enrichment.bolt;

import org.json.simple.JSONObject;
import org.apache.storm.tuple.Tuple;

/**
 * Exists in order to provide a bolt that tests that when the GenericEnrichmentBolt writes to error, that it actually carries
 * through the queue
 */
public class ErrorEnrichmentBolt extends GenericEnrichmentBolt {

  public static final String TEST_ERROR_MESSAGE = "Test throwing error from ErrorEnrichmentBolt";

  public ErrorEnrichmentBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    JSONObject rawMessage = new JSONObject();
    rawMessage.put("rawMessage", "Error Test Raw Message String");

    JSONObject enrichedMessage= new JSONObject();
    enrichedMessage.put("enrichedMessage", "Error Test Enriched Message String");
    handleError("key", rawMessage, "subgroup", enrichedMessage, new IllegalStateException(TEST_ERROR_MESSAGE));
  }
}
