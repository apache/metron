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

package org.apache.metron.common.dsl.functions;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.metron.common.dsl.BaseStellarFunction;
import org.apache.metron.common.dsl.Stellar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class StringFunctions {

  @Stellar(name="REGEXP_MATCH"
          ,description = "Determines whether a regex matches a string"
          , params = {
             "string - The string to test"
            ,"pattern - The proposed regex pattern"
            }
          , returns = "True if the regex pattern matches the string and false otherwise.")
  public static class RegexpMatch extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if(list.size() < 2) {
        throw new IllegalStateException("REGEXP_MATCH expects two args: [string, pattern] where pattern is a regexp pattern");
      }
      String pattern = (String) list.get(1);
      String str = (String) list.get(0);
      if(str == null || pattern == null) {
        return false;
      }
      return str.matches(pattern);
    }
  }

  @Stellar(name="ENDS_WITH"
          ,description = "Determines whether a string ends with a prefix"
          , params = {
             "string - The string to test"
            ,"suffix - The proposed suffix"
            }
          , returns = "True if the string ends with the specified suffix and false otherwise.")
  public static class EndsWith extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> list) {
      if(list.size() < 2) {
        throw new IllegalStateException("ENDS_WITH expects two args: [string, suffix] where suffix is the string fragment that the string should end with");
      }
      String prefix = (String) list.get(1);
      String str = (String) list.get(0);
      if(str == null || prefix == null) {
        return false;
      }
      return str.endsWith(prefix);
    }
  }

  @Stellar(name="STARTS_WITH"
          ,description = "Determines whether a string starts with a prefix"
          , params = {
             "string - The string to test"
            ,"prefix - The proposed prefix"
            }
          , returns = "True if the string starts with the specified prefix and false otherwise."
          )
  public static class StartsWith extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if(list.size() < 2) {
        throw new IllegalStateException("STARTS_WITH expects two args: [string, prefix] where prefix is the string fragment that the string should start with");
      }
      String prefix = (String) list.get(1);
      String str = (String) list.get(0);
      if(str == null || prefix == null) {
        return false;
      }
      return str.startsWith(prefix);
    }
  }

  @Stellar( name="TO_LOWER"
          , description = "Transforms the first argument to a lowercase string"
          , params = { "input - String" }
          , returns = "String"
          )
  public static class ToLower extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> strings) {
      return strings.get(0)==null?null:strings.get(0).toString().toLowerCase();
    }
  }

  @Stellar( name="TO_UPPER"
          , description = "Transforms the first argument to an uppercase string"
          , params = { "input - String" }
          , returns = "String"
          )
  public static class ToUpper extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> strings) {
      return strings.get(0)==null?null:strings.get(0).toString().toUpperCase();
    }
  }

  @Stellar(name="TO_STRING"
          , description = "Transforms the first argument to a string"
          , params = { "input - Object" }
          , returns = "String"
          )
  public static class ToString extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> strings) {
      return strings.get(0)==null?null:strings.get(0).toString();
    }
  }

  @Stellar(name="TRIM"
          , description = "Trims whitespace from both sides of a string."
          , params = { "input - String" }
          , returns = "String"
          )
  public static class Trim extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> strings) {
      return strings.get(0)==null?null:strings.get(0).toString().trim();
    }
  }

  @Stellar( name="JOIN"
          , description="Joins the components of the list with the specified delimiter."
          , params = { "list - List of Strings", "delim - String delimiter"}
          , returns = "String"
          )
  public static class JoinFunction extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      List<Object> arg1 = (List<Object>) args.get(0);
      String delim = (String) args.get(1);
      return Joiner.on(delim).join(Iterables.filter(arg1, x -> x != null));
    }
  }

  @Stellar(name="SPLIT"
          , description="Splits the string by the delimiter."
          , params = { "input - String to split", "delim - String delimiter"}
          , returns = "List of Strings"
          )
  public static class SplitFunction extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      List ret = new ArrayList();
      Object o1 = args.get(0);
      if(o1 != null) {
        String arg1 = o1.toString();
        String delim = (String) args.get(1);
        Iterables.addAll(ret, Splitter.on(delim).split(arg1));
      }
      return ret;
    }
  }

  @Stellar(name="GET_LAST"
          , description="Returns the last element of the list"
          , params = { "input - List"}
          , returns = "Last element of the list"
          )
  public static class GetLast extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      List<Object> arg1 = (List<Object>) args.get(0);
      return Iterables.getLast(arg1, null);
    }
  }

  @Stellar(name="GET_FIRST"
          , description="Returns the first element of the list"
          , params = { "input - List"}
          , returns = "First element of the list"
          )
  public static class GetFirst extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      List<Object> arg1 = (List<Object>) args.get(0);
      return Iterables.getFirst(arg1, null);
    }
  }

  @Stellar(name="GET"
          , description="Returns the i'th element of the list "
          , params = { "input - List", "i - the index (0-based)"}
          , returns = "First element of the list"
          )
  public static class Get extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      List<Object> arg1 = (List<Object>) args.get(0);
      int offset = (Integer) args.get(1);
      if(offset < arg1.size()) {
        return Iterables.get(arg1, offset);
      }
      return null;
    }
  }
}
