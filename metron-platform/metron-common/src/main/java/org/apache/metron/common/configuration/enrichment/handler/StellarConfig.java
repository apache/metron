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

import org.apache.metron.stellar.common.StellarAssignment;
import org.apache.metron.stellar.common.StellarProcessor;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public class StellarConfig implements Config {
  protected static final Logger _LOG = LoggerFactory.getLogger(StellarConfig.class);
  @Override
  public List<String> getSubgroups(Iterable<Map.Entry<String, Object>> config) {
    boolean includeEmpty = false;
    List<String> ret = new ArrayList<>();
    for(Map.Entry<String, Object> kv : config) {
      if(kv.getValue() instanceof String) {
        includeEmpty = true;
      }
      else if(kv.getValue() instanceof Map || kv.getValue() instanceof List) {
        ret.add(kv.getKey());
      }
    }
    if(includeEmpty) {
      ret.add("");
    }
    return ret;
  }

  @Override
  public Iterable<Map.Entry<String, Object>> toConfig(Object c) {
    if(c instanceof Map) {
      return ((Map<String, Object>)c).entrySet();
    }
    else if(c instanceof Collection) {
      List<Map.Entry<String, Object>> ret = new ArrayList<>();
      for(Object o : (Collection)c) {
        if(o instanceof String) {
          StellarAssignment assignment = StellarAssignment.from((String)o);
          ret.add(assignment);
        }
        else if(o instanceof Map.Entry) {
          ret.add((Map.Entry<String, Object>)o);
        }
        else {
          throw new IllegalStateException("Expected " + c + " to be a list of strings, but got non-string.");
        }
      }
      return ret;
    }
    throw new IllegalStateException("Unable to convert config " + c
                                   + " to stellar config.  Expected List<String> or Map<String, Object>");
  }

  @Override
  public List<JSONObject> splitByFields( JSONObject message
                                 , Object fields
                                 , Function<String, String> fieldToEnrichmentKey
                                 , Iterable<Map.Entry<String, Object>> config
                                 )
  {
    StellarProcessor processor = new StellarProcessor();
    List<JSONObject> messages = new ArrayList<>();
    List<String> defaultStellarStatementGroup = new ArrayList<>();
    for(Map.Entry<String, Object> kv : config) {
      if(kv.getValue() instanceof String) {
        defaultStellarStatementGroup.add((String)kv.getValue());
      }
      else if(kv.getValue() instanceof Map) {
        JSONObject ret = new JSONObject();
        ret.put(kv.getKey(), getMessage(getFields(processor, (Map)kv.getValue()), message));
        messages.add(ret);
      }
      else if(kv.getValue() instanceof List) {
        JSONObject ret = new JSONObject();
        ret.put(kv.getKey(), getMessage(getFields(processor, (List)kv.getValue()), message));
        messages.add(ret);
      }
    }
    if(defaultStellarStatementGroup.size() > 0)
    {
      JSONObject ret = new JSONObject();
      ret.put("", getMessage(getFields(processor, defaultStellarStatementGroup), message));
      messages.add(ret);
    }
    _LOG.debug("Stellar enrichment split: {}", messages );
    return messages;
  }

  private Set<String> getFields(StellarProcessor processor
                               , List<String> stellarStatementGroup
                               )
  {
    Set<String> stellarFields = new HashSet<>();
    for(String stellarStatementExpr: stellarStatementGroup) {
      StellarAssignment assignment = StellarAssignment.from(stellarStatementExpr);
      if(assignment.getStatement() != null) {
        Set<String> variables = processor.variablesUsed(assignment.getStatement());
        if (variables != null) {
          stellarFields.addAll(variables);
        }
      }
    }
    return stellarFields;
  }

  private Set<String> getFields( StellarProcessor processor
                               , Map<String, String> stellarStatementGroup
  ) {
    Set<String> stellarFields = new HashSet<>();
    for (String stellarStatement : stellarStatementGroup.values()) {
      Set<String> variables = processor.variablesUsed(stellarStatement);
      if (variables != null) {
        stellarFields.addAll(variables);
      }
    }
    return stellarFields;
  }

  private Map<String, Object> getMessage( Set<String> stellarFields
                                        , JSONObject message
                                        )
  {

    Map<String, Object> messageSegment = new HashMap<>();
    for(String variable : stellarFields) {
      messageSegment.put(variable, message.get(variable));
    }
    return messageSegment;
  }
}
