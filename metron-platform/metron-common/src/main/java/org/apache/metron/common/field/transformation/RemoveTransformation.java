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

package org.apache.metron.common.field.transformation;

import org.apache.metron.stellar.dsl.*;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.common.StellarPredicateProcessor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RemoveTransformation implements FieldTransformation {
  public static final String CONDITION_CONF = "condition";
  public static final StellarPredicateProcessor PASSTHROUGH_PROCESSOR = new StellarPredicateProcessor() {
    @Override
    public Boolean parse(String rule, VariableResolver resolver, FunctionResolver functionResolver, Context context) {
      return true;
    }

    @Override
    public boolean validate(String rule) throws ParseException {
      return true;
    }

    @Override
    public boolean validate(String rule, boolean throwException, Context context) throws ParseException {
      return true;
    }
  };
  private String getCondition(Map<String, Object> fieldMappingConfig) {
    Object conditionObj = fieldMappingConfig.get(CONDITION_CONF);

    if(conditionObj == null || !(conditionObj instanceof String)) {
      return null;
    }
    return conditionObj.toString();
  }

  private StellarPredicateProcessor getPredicateProcessor(String condition)
  {
    if(condition == null) {
      return PASSTHROUGH_PROCESSOR;
    }
    else {
      return new StellarPredicateProcessor();
    }
  }

  @Override
  public Map<String, Object> map( Map<String, Object> input
                                , final List<String> outputFields
                                , LinkedHashMap<String, Object> fieldMappingConfig
                                , Context context
                                , Map<String, Object>... sensorConfig
                                ) {
    String condition = getCondition(fieldMappingConfig);
    StellarPredicateProcessor processor = getPredicateProcessor(condition);
    if(processor.parse(condition, new MapVariableResolver(input), StellarFunctions.FUNCTION_RESOLVER(), context)) {
      return new HashMap<String, Object>() {{
        for(String outputField : outputFields) {
          put(outputField, null);
        }
      }};
    }
    return null;
  }
}
