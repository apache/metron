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

import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class DefaultRawMessageStrategy implements RawMessageStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  @Override
  public RawMessage get(Map<String, Object> rawMetadata, byte[] rawMessage, boolean readMetadata, Map<String, Object> config) {
    return new RawMessage(rawMessage, rawMetadata);
  }

  @Override
  public void mergeMetadata(JSONObject message, Map<String, Object> metadata, boolean mergeMetadata, Map<String, Object> config) {
    if(mergeMetadata) {
      message.putAll(metadata);
    }
  }

  @Override
  public boolean mergeMetadataDefault() {
    return false;
  }

  @Override
  public boolean readMetadataDefault() {
    return false;
  }
}
