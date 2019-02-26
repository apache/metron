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

import com.google.common.collect.Iterables;
import org.apache.metron.stellar.common.utils.PatternCache;
import org.apache.metron.stellar.dsl.Context;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexSelectTransformation implements FieldTransformation {
  @Override
  public Map<String, Object> map( Map<String, Object> input
                                , List<String> outputField
                                , LinkedHashMap<String, Object> fieldMappingConfig
                                , Context context
                                , Map<String, Object>... sensorConfig
                                ) {
    String outField = null;
    if(!(outputField == null || outputField.isEmpty())) {
      outField = outputField.get(0);
    }
    String inVal = null;
    if(!(input == null || input.isEmpty() || input.size() > 1)) {
      Object inValObj = Iterables.getFirst(input.entrySet(), null).getValue();
      if(inValObj != null) {
        inVal = inValObj.toString();
      }
    }
    Map<String, Object> ret = new HashMap<>(1);
    if(outField == null || inVal == null) {
      //in the situation where we don't have valid input or output, then we want to do nothing
      return ret;
    }
    for(Map.Entry<String, Object> valToRegex : fieldMappingConfig.entrySet()) {
      if(isMatch(valToRegex.getValue(), inVal)) {
        ret.put(outField, valToRegex.getKey());
        break;
      }
    }
    return ret;
  }

  /**
   * Return true if there is a regex match or false otherwise.
   * @param regexes This could be either a list of regexes or a single regex
   * @param field The field to match
   * @return True if match, false otherwise
   */
  private static boolean isMatch(Object regexes, String field) {
    if(regexes instanceof String) {
      return isMatch((String)regexes, field);
    }
    else if(regexes instanceof List) {
      for(Object regex : (List)regexes) {
        if(isMatch(regex.toString(), field)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isMatch(String regex, String field) {
    try {
      Pattern p = PatternCache.INSTANCE.getPattern(regex);
      if (p == null) {
        return false;
      }
      return p.asPredicate().test(field);
    }
    catch(PatternSyntaxException pse) {
      return false;
    }
  }

}
