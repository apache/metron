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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A collection of functions that operate on Strings.
 */
public class StringFunctions {

  /**
   * Convert string to lower case.
   */
  public static Function<List<Object>, Object> ToLower = strings ->
          strings.get(0) == null ? null : strings.get(0).toString().toLowerCase();

  /**
   * Convert string to upper case.
   */
  public static Function<List<Object>, Object> ToUpper = strings ->
          strings.get(0) == null?null:strings.get(0).toString().toUpperCase();

  /**
   * Trim whitespace from a string.
   */
  public static Function<List<Object>, Object> Trim = strings ->
          strings.get(0) == null ? null : strings.get(0).toString().trim();

  /**
   * Join a list of strings into a single string using a delimiter.
   */
  public static Function<List<Object>, Object> Join = args -> {
    List<Object> arg1 = (List<Object>) args.get(0);
    String delim = (String) args.get(1);
    return Joiner.on(delim).join(Iterables.filter(arg1, x -> x != null));
  };

  /**
   * Split a string based on a delimiter.
   */
  public static Function<List<Object>, Object> Split = args -> {
    List ret = new ArrayList();
    Object o1 = args.get(0);
    if(o1 != null) {
      String arg1 = o1.toString();
      String delim = (String) args.get(1);
      Iterables.addAll(ret, Splitter.on(delim).split(arg1));
    }
    return ret;
  };

  /**
   * Does a String start with a given prefix?
   *
   *  STARTS_WITH (string, prefix)
   */
  public static Function<List<Object>, Object> StartsWith = args -> {
    if(args.size() < 2) {
      throw new IllegalStateException("STARTS_WITH expects two args: [string, prefix] where prefix is the string fragment that the string should start with");
    }

    String str = (String) args.get(0);
    String prefix = (String) args.get(1);

    return (str == null || prefix == null) ? false: str.startsWith(prefix);
  };

  /**
   * Does a String end with a given suffix?
   *
   *  ENDS_WITH (string, suffix)
   */
  public static Function<List<Object>, Object> EndsWith = args -> {
    if(args.size() < 2) {
      throw new IllegalStateException("ENDS_WITH expects two args: [string, suffix] where suffix is the string fragment that the string should end with");
    }

    String suffix = (String) args.get(1);
    String str = (String) args.get(0);

    return (str == null || suffix == null) ? false: str.endsWith(suffix);
  };

  /**
   * Does a string match a given regular expression?
   *
   *  REGEXP_MATCH (string, pattern)
   */
  public static Function<List<Object>, Object> RegexMatch = args -> {
    if(args.size() < 2) {
      throw new IllegalStateException("REGEXP_MATCH expects two args: [string, pattern] where pattern is a regexp pattern");
    }

    String pattern = (String) args.get(1);
    String str = (String) args.get(0);

    return (str == null || pattern == null) ? false: str.matches(pattern);
  };

}
