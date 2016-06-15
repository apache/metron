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
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.transformation.TransformationProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MTLTransformation implements FieldTransformation {
  @Override
  public Map<String, Object> map( Map<String, Object> input
                                , List<String> outputField
                                , Map<String, Object> fieldMappingConfig
                                , Map<String, Object> sensorConfig
                                )
  {
    Map<String, Object> ret = new HashMap<>();
    VariableResolver resolver = new MapVariableResolver(ret, input,fieldMappingConfig, sensorConfig);
    TransformationProcessor processor = new TransformationProcessor();
    for(String oField : outputField) {
      Object transformObj = fieldMappingConfig.get(oField);
      if(transformObj != null) {
        Object o = processor.parse(transformObj.toString(), resolver);
        if (o != null) {
          ret.put(oField, o);
        }
      }
    }
    return ret;
  }
}
