/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.stellar.dsl.functions;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.common.utils.PatternCache;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

public class RegExFunctions {

  @Stellar(name = "REGEXP_MATCH",
      description = "Determines whether a regex matches a string, if a list of patterns is passed, then the matching is an OR operation",
      params = {
          "string - The string to test",
          "pattern - The proposed regex pattern or a list of proposed regex patterns"
      },
      returns = "True if the regex pattern matches the string and false if otherwise.")
  public static class RegexpMatch extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if (list.size() < 2) {
        throw new IllegalStateException(
            "REGEXP_MATCH expects two args: [string, pattern] where pattern is a regexp pattern or a list of regexp patterns");
      }
      Object patternObject = list.get(1);
      String str = (String) list.get(0);
      if (str == null || patternObject == null) {
        return false;
      }
      if (patternObject instanceof String) {
        return PatternCache.INSTANCE.getPattern((String)patternObject).matcher(str).matches();
      } else if (patternObject instanceof Iterable) {
        boolean matches = false;
        for (Object thisPatternObject : (Iterable)patternObject) {
          if (thisPatternObject == null) {
            continue;
          }
          if (PatternCache.INSTANCE.getPattern(thisPatternObject.toString()).matcher(str).matches()) {
            matches = true;
            break;
          }
        }
        return matches;
      }
      return false;
    }
  }

  @Stellar(name = "REGEXP_GROUP_VAL",
      description = "Returns the value of a group in a regex against a string",
      params = {
          "string - The string to test",
          "pattern - The proposed regex pattern",
          "group - integer that selects what group to select, starting at 1"
      },
      returns = "The value of the group, or null if not matched or no group at index")
  public static class RegexpGroupValue extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if (list.size() != 3) {
        throw new IllegalStateException(
            "REGEXP_GROUP_VAL expects three args: [string, pattern, int]" + ""
                + "where pattern is a regexp pattern");
      }
      String stringPattern = (String) list.get(1);
      String str = (String) list.get(0);
      Integer groupNumber = ConversionUtils.convert(list.get(2), Integer.class);

      if (groupNumber == null) {
        // group number was not a valid integer
        return null;
      }

      if (groupNumber == 0) {
        // 0, by default is the entire input
        // default to returning a non-null
        return str;
      }

      if (str == null || stringPattern == null) {
        return null;
      }
      Pattern pattern = PatternCache.INSTANCE.getPattern(stringPattern);
      Matcher matcher = pattern.matcher(str);
      if (!matcher.matches()) {
        return null;
      }

      int groupCount = matcher.groupCount();
      if (groupCount == 0 || groupCount < groupNumber) {
        return null;
      }
      return matcher.group(groupNumber);
    }
  }

  @Stellar(name = "REGEXP_REPLACE",
      description = "Replace all occurences of the regex pattern within the string by value",
      params = {
          "string - The input string",
          "pattern - The regex pattern to be replaced. Special characters must be escaped (e.g. \\\\d)",
          "value - The value to replace the regex pattern"
      },
      returns = "The modified input string with replaced values")
  public static class RegexpReplace extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if (list.size() != 3) {
        throw new IllegalStateException(
            "REGEXP_REPLACE expects three args: [string, pattern, value]"
                + " where pattern is a regexp pattern");
      }
      String str = (String) list.get(0);
      String stringPattern = (String) list.get(1);
      String value = (String) list.get(2);

      if (StringUtils.isEmpty(str)) {
        return null;
      }

      if (StringUtils.isEmpty(stringPattern) || StringUtils.isEmpty(value)) {
        return str;
      }

      Pattern pattern = PatternCache.INSTANCE.getPattern(stringPattern);
      Matcher matcher = pattern.matcher(str);
      return matcher.replaceAll(value);
    }
  }
}
