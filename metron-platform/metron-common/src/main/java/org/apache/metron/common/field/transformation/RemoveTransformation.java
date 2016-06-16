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

import org.apache.metron.common.dsl.MapVariableResolver;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.query.PredicateProcessor;
import org.apache.metron.common.dsl.VariableResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoveTransformation implements FieldTransformation {
  public static final String CONDITION_CONF = "condition";
  public static final PredicateProcessor PASSTHROUGH_PROCESSOR = new PredicateProcessor() {
    @Override
    public boolean parse(String rule, VariableResolver resolver) {
      return true;
    }

    @Override
    public boolean validate(String rule) throws ParseException {
      return true;
    }

    @Override
    public boolean validate(String rule, boolean throwException) throws ParseException {
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

  private PredicateProcessor getPredicateProcessor(String condition)
  {
    if(condition == null) {
      return PASSTHROUGH_PROCESSOR;
    }
    else {
      return new PredicateProcessor();
    }
  }

  @Override
  public Map<String, Object> map( Map<String, Object> input
                                , final List<String> outputFields
                                , Map<String, Object> fieldMappingConfig
                                , Map<String, Object> sensorConfig
                                ) {
    String condition = getCondition(fieldMappingConfig);
    PredicateProcessor processor = getPredicateProcessor(condition);
    if(processor.parse(condition, new MapVariableResolver(input))) {
      return new HashMap<String, Object>() {{
        for(String outputField : outputFields) {
          put(outputField, null);
        }
      }};
    }
    return null;
  }
}
