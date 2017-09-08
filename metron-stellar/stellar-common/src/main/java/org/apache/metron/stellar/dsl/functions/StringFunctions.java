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

package org.apache.metron.stellar.dsl.functions;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringFunctions {

  @Stellar(name="ENDS_WITH"
          ,description = "Determines whether a string ends with a specified suffix"
          , params = {
             "string - The string to test"
            ,"suffix - The proposed suffix"
            }
          , returns = "True if the string ends with the specified suffix and false if otherwise")
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
          , returns = "True if the string starts with the specified prefix and false if otherwise"
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
          , returns = "Lowercase string"
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
          , returns = "Uppercase string"
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
          , description="Joins the components in the list of strings with the specified delimiter."
          , params = { "list - List of strings", "delim - String delimiter"}
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
          , returns = "List of strings"
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
          , params = { "input - List", "i - The index (0-based)"}
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

  private enum FillDirection{
    LEFT,
    RIGHT
  }

  @Stellar(name="FILL_LEFT"
          , description="Fills or pads a given string with a given character, to a given length on the left"
          , params = { "input - string", "fill - the fill character", "len - the required length"}
          , returns = "Filled String"
  )
  public static class FillLeft extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      if(args.size() < 3) {
        throw new IllegalStateException("FILL_LEFT expects three args: [string,char,length] where char is the fill character string and length is the required length of the result");
      }
      return fill(FillDirection.LEFT,args.get(0),args.get(1),args.get(2));
    }
  }

  @Stellar(name="FILL_RIGHT"
          , description="Fills or pads a given string with a given character, to a given length on the right"
          , params = { "input - string", "fill - the fill character", "len - the required length"}
          , returns = "Filled String"
  )
  public static class FillRight extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      if(args.size() < 3) {
        throw new IllegalStateException("FILL_RIGHT expects three args: [string,char,length] where char is the fill character string and length is the required length of the result");
      }
      return fill(FillDirection.RIGHT,args.get(0),args.get(1),args.get(2));
    }
  }

  private static Object fill(FillDirection direction, Object inputObject, Object fillObject, Object requiredLengthObject)throws ParseException{
    if(inputObject == null) {
      return null;
    }
    String input = inputObject.toString();

    if(requiredLengthObject == null || fillObject == null) {
       throw new IllegalStateException("Required Length and Fill String are both required");
    }

    String fill = fillObject.toString();
    if(org.apache.commons.lang.StringUtils.isEmpty(fill)){
      throw new IllegalStateException("The fill cannot be an empty string");
    }
    fill = fill.substring(0,1);
    Integer requiredLength = ConversionUtils.convert(requiredLengthObject,Integer.class);
    if(requiredLength == null){
      throw new IllegalStateException("Required Length  not a valid Integer: " + requiredLengthObject.toString());
    }

    if(direction == FillDirection.LEFT) {
      return org.apache.commons.lang.StringUtils.leftPad(input,requiredLength,fill);
    }
    return org.apache.commons.lang.StringUtils.rightPad(input,requiredLength,fill);
  }

  @Stellar( namespace="STRING"
          , name="ENTROPY"
          , description = "Computes the base-2 shannon entropy of a string"
          , params = { "input - String" }
          , returns = "The base-2 shannon entropy of the string (https://en.wikipedia.org/wiki/Entropy_(information_theory)#Definition).  The unit of this is bits."
  )
  public static class Entropy extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> strings) {
      /*
      Shannon entropy is defined as follows:
      \Eta(X) = - \sum(p(x_i)*log_2(p(x_i)), i=0, n-1) where x_i are distinct characters in the string.
       */
      Map<Character, Integer> frequency = new HashMap<>();
      if(strings.size() != 1) {
        throw new IllegalArgumentException("STRING_ENTROPY expects exactly one argument which is a string.");
      }
      String input = ConversionUtils.convert(strings.get(0), String.class);
      if(StringUtils.isEmpty(input)) {
        return 0.0;
      }
      for(int i = 0;i < input.length();++i) {
        char c = input.charAt(i);
        frequency.put(c, frequency.getOrDefault(c, 0) + 1);
      }
      double ret = 0.0;
      double log2 = Math.log(2);
      for(Integer f : frequency.values()) {
        double p = f.doubleValue()/input.length();
        ret -= p * Math.log(p) / log2;
      }
      return ret;
    }
  }

  @Stellar( name="FORMAT"
          , description = "Returns a formatted string using the specified format string and arguments. Uses Java's string formatting conventions."
          , params = { "format - string", "arguments... - object(s)" }
          , returns = "A formatted string."
  )
  public static class Format extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {

      if(args.size() == 0) {
        throw new IllegalArgumentException("[FORMAT] missing argument: format string");
      }

      String format = ConversionUtils.convert(args.get(0), String.class);
      Object[] formatArgs = args.subList(1, args.size()).toArray();

      return String.format(format, formatArgs);
    }
  }

  @Stellar( name="SUBSTRING"
          , description = "Returns a substring of a string"
          , params = {
                "input - The string to take the substring of",
                "start - The starting position (0-based and inclusive)",
                "end? - The ending position (0-based and exclusive)"
                     }
          , returns = "The substring of the input"
  )
  public static class Substring extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> strings) {

      if(strings == null || strings.size() < 2 ) {
        throw new IllegalArgumentException("[SUBSTRING] required 2 arguments: the input and the start position (inclusive)");
      }
      String var = strings.get(0) == null?null: (String) strings.get(0);
      Integer start = strings.get(1) == null?null:(Integer)strings.get(1);
      Integer end = null;
      if(strings.size() > 2) {
         end = strings.get(2) == null ? null : (Integer) strings.get(2);
      }
      if(var == null || start == null) {
        return null;
      }
      else if(var.length() == 0) {
        return var;
      }
      else {
        if(end == null) {
          return var.substring(start);
        }
        else {
          return var.substring(start, end);
        }
      }
    }
  }

  @Stellar( name="CHOMP"
          , description = "Removes one newline from end of a String if it's there, otherwise leave it alone. A newline is \"\\n\", \"\\r\", or \"\\r\\n\""
          , params = { "the String to chomp a newline from, may be null"}
          , returns = "String without newline, null if null String input"
  )
  public static class Chomp extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> strings) {

      if(strings == null || strings.size() == 0 ) {
        throw new IllegalArgumentException("[CHOMP] missing argument: string to be chopped");
      }
      String var = strings.get(0) == null?null: (String) strings.get(0);
      if(var == null) {
        return null;
      }
      else if(var.length() == 0) {
        return var;
      }
      else {
        return StringUtils.chomp(var);
      }
    }
  }
  @Stellar( name="CHOP"
          , description = "Remove the last character from a String"
          , params = { "the String to chop last character from, may be null"}
          , returns = "String without last character, null if null String input"
  )
  public static class Chop extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> strings) {

      if(strings == null || strings.size() == 0 ) {
        throw new IllegalArgumentException("[CHOP] missing argument: string to be chopped");
      }
      String var = strings.get(0) == null?null: (String) strings.get(0);
      if(var == null) {
        return null;
      }
      else if(var.length() == 0) {
        return var;
      }
      else {
        return StringUtils.chop(var);
      }
    }
  }

  @Stellar( name = "PREPEND_IF_MISSING"
          , description = "Prepends the prefix to the start of the string if the string does not already start with any of the prefixes"
          , params = {
          "str - The string."
          , "prefix - The string prefix to prepend to the start of the string"
          , "additionalprefix - Optional - Additional string prefix that is valid"
  }
          , returns = "A new String if prefix was prepended, the same string otherwise."
  )
  public static class PrependIfMissing extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> strings) {

      String prefixed;
      switch (strings.size()) {
        case 2: prefixed = StringUtils.prependIfMissing((String) strings.get(0), (String) strings.get(1));
          break;
        case 3: prefixed = StringUtils.prependIfMissing((String) strings.get(0), (String) strings.get(1), (String) strings.get(2));
          break;
        default: throw new IllegalArgumentException("[PREPEND_IF_MISSING] incorrect arguments: " + strings.toString() + "\nUsage: PREPEND_IF_MISSING <String> <prefix> [<prefix>...]");
      }
      return prefixed;
    }
  }

  @Stellar( name = "APPEND_IF_MISSING"
          , description = "Appends the suffix to the end of the string if the string does not already end with any of the suffixes"
          , params = {
          "str - The string."
          , "suffix - The string suffix to append to the end of the string"
          , "additionalsuffix - Optional - Additional string suffix that is a valid terminator"
  }
          , returns = "A new String if suffix was appended, the same string otherwise."
  )
  public static class AppendIfMissing extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> strings) {

      String suffixed;
      switch (strings.size()) {
        case 2:
          suffixed = StringUtils.appendIfMissing((String) strings.get(0), (String) strings.get(1));
          break;
        case 3:
          suffixed = StringUtils.appendIfMissing((String) strings.get(0), (String) strings.get(1), (String) strings.get(2));
          break;
        default:
          throw new IllegalArgumentException("[APPEND_IF_MISSING] incorrect arguments. Usage: APPEND_IF_MISSING <String> <prefix> [<prefix>...]");
      }
      return suffixed;
    }
  }

  @Stellar( name = "COUNT_MATCHES"
          , description = "Counts how many times the substring appears in the larger string"
          , params = {
          "str - the CharSequence to check, may be null"
          , "sub - the substring to count, may be null"
  }
          , returns = "the number of non-overlapping occurrences, 0 if either CharSequence is null"
  )
  public static class CountMatches extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> strings) {

      if(strings.size() != 2) {
        throw new IllegalArgumentException("[COUNT_MATCHES] incorrect arguments. Usage: COUNT_MATCHES <String> <substring>");
      }

      int matchcount;
      matchcount = StringUtils.countMatches((String) strings.get(0), (String) strings.get(1));
      return matchcount;
    }
  }

}
