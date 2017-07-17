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
package org.apache.metron.rest.service.impl;

import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.StellarFunctionInfo;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.common.field.transformation.FieldTransformations;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.rest.model.StellarFunctionDescription;
import org.apache.metron.rest.model.SensorParserContext;
import org.apache.metron.rest.service.StellarService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StellarServiceImpl implements StellarService {

  private CuratorFramework client;

  @Autowired
  public StellarServiceImpl(CuratorFramework client) {
    this.client = client;
    try {
      ConfigurationsUtils.setupStellarStatically(this.client);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to setup stellar statically: " + e.getMessage(), e);
    }
  }

  @Override
  public Map<String, Boolean> validateRules(List<String> rules) {
    Map<String, Boolean> results = new HashMap<>();
    StellarProcessor stellarProcessor = new StellarProcessor();
    for(String rule: rules) {
      try {
        boolean result = stellarProcessor.validate(rule, Context.EMPTY_CONTEXT());
        results.put(rule, result);
      } catch (ParseException e) {
        results.put(rule, false);
      }
    }
    return results;
  }

  @Override
  public Map<String, Object> applyTransformations(SensorParserContext sensorParserContext) {
    JSONObject sampleJson = new JSONObject(sensorParserContext.getSampleData());
    sensorParserContext.getSensorParserConfig().getFieldTransformations().forEach(fieldTransformer -> {
              fieldTransformer.transformAndUpdate(sampleJson, Context.EMPTY_CONTEXT(), sensorParserContext.getSensorParserConfig().getParserConfig());
            }
    );
    return sampleJson;
  }

  @Override
  public FieldTransformations[] getTransformations() {
    return FieldTransformations.values();
  }

  @Override
  public List<StellarFunctionDescription> getStellarFunctions() {
    List<StellarFunctionDescription> stellarFunctionDescriptions = new ArrayList<>();
    Iterable<StellarFunctionInfo> stellarFunctionsInfo = StellarFunctions.FUNCTION_RESOLVER().getFunctionInfo();
    stellarFunctionsInfo.forEach(stellarFunctionInfo -> {
      stellarFunctionDescriptions.add(new StellarFunctionDescription(
              stellarFunctionInfo.getName(),
              stellarFunctionInfo.getDescription(),
              stellarFunctionInfo.getParams(),
              stellarFunctionInfo.getReturns()));
    });
    return stellarFunctionDescriptions;
  }

  @Override
  public List<StellarFunctionDescription> getSimpleStellarFunctions() {
    List<StellarFunctionDescription> stellarFunctionDescriptions = getStellarFunctions();
    return stellarFunctionDescriptions.stream().filter(stellarFunctionDescription ->
            stellarFunctionDescription.getParams().length == 1).sorted((o1, o2) -> o1.getName().compareTo(o2.getName())).collect(Collectors.toList());
  }

}
