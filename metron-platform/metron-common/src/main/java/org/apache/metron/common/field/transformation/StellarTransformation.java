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

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.MapVariableResolver;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.stellar.StellarProcessor;

import java.util.*;

public class StellarTransformation implements FieldTransformation {
  @Override
  public Map<String, Object> map( Map<String, Object> input
                                , List<String> outputField
                                , LinkedHashMap<String, Object> fieldMappingConfig
                                , Map<String, Object> sensorConfig
                                , Context context
                                )
  {
    Map<String, Object> ret = new HashMap<>();
    Map<String, Object> intermediateVariables = new HashMap<>();
    Set<String> outputs = new HashSet<>(outputField);
    VariableResolver resolver = new MapVariableResolver(ret, intermediateVariables, input, sensorConfig);
    StellarProcessor processor = new StellarProcessor();
    for(Map.Entry<String, Object> kv : fieldMappingConfig.entrySet()) {
      String oField = kv.getKey();
      Object transformObj = kv.getValue();
      if(transformObj != null) {
        try {
          Object o = processor.parse(transformObj.toString(), resolver, StellarFunctions.FUNCTION_RESOLVER(), context);
          if (o != null) {
            if(outputs.contains(oField)) {
              ret.put(oField, o);
            }
            else {
              intermediateVariables.put(oField, o);
            }
          }
        }
        catch(Exception ex) {
          throw new IllegalStateException( "Unable to process transformation: " + transformObj.toString()
                                         + " for " + oField + " because " + ex.getMessage()
                                         , ex
                                         );
        }
      }
    }
    return ret;
  }
}
