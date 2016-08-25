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
import org.apache.metron.common.stellar.StellarProcessor;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.function.Function;

public class StellarConfig implements Config {

  @Override
  public List<String> getSubgroups(Map<String, Object> config) {
    boolean includeEmpty = false;
    List<String> ret = new ArrayList<>();
    for(Map.Entry<String, Object> kv : config.entrySet()) {
      if(kv.getValue() instanceof String) {
        includeEmpty = true;
      }
      else if(kv.getValue() instanceof Map) {
        ret.add(kv.getKey());
      }
    }
    if(includeEmpty) {
      ret.add("");
    }
    return ret;
  }

  @Override
  public List<JSONObject> splitByFields( JSONObject message
                                 , Object fields
                                 , Function<String, String> fieldToEnrichmentKey
                                 , Map<String, Object> config
                                 )
  {
    StellarProcessor processor = new StellarProcessor();
    List<JSONObject> messages = new ArrayList<>();
    Map<String, String> defaultStellarStatementGroup = new HashMap<>();
    for(Map.Entry<String, Object> kv : config.entrySet()) {
      if(kv.getValue() instanceof String) {
        defaultStellarStatementGroup.put(kv.getKey(), (String)kv.getValue());
      }
      else if(kv.getValue() instanceof Map) {
        JSONObject ret = new JSONObject();
        ret.put(kv.getKey(), getMessage(processor, (Map<String, String>) kv.getValue(), message));
        messages.add(ret);
      }
    }
    if(defaultStellarStatementGroup.size() > 0)
    {
      JSONObject ret = new JSONObject();
      ret.put("", getMessage(processor, defaultStellarStatementGroup, message));
      messages.add(ret);
    }
    return messages;
  }

  private Map<String, Object> getMessage( StellarProcessor processor
                                        , Map<String, String> stellarStatementGroup
                                        , JSONObject message
                                        )
  {
    Set<String> stellarFields = new HashSet<>();
    for(String stellarStatement: stellarStatementGroup.values()) {
      Set<String> variables = processor.variablesUsed(stellarStatement);
      if(variables != null) {
        stellarFields.addAll(variables);
      }
    }
    Map<String, Object> messageSegment = new HashMap<>();
    for(String variable : stellarFields) {
      messageSegment.put(variable, message.get(variable));
    }
    return messageSegment;
  }
}
