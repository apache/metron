/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.common.dsl.functions;

import com.google.common.collect.Iterables;

import java.util.List;
import java.util.function.Function;

/**
 * A collection of functions that operate on Lists.
 */
public class ListFunctions {

  /**
   * Get the first element from a list.
   */
  public static Function<List<Object>, Object> GetLast = args -> {
    List<Object> arg1 = (List<Object>) args.get(0);
    return Iterables.getLast(arg1, null);
  };

  /**
   * Get the last element from a list.
   */
  public static Function<List<Object>, Object> GetFirst = args -> {
    List<Object> arg1 = (List<Object>) args.get(0);
    return Iterables.getFirst(arg1, null);
  };

  /**
   * Get a single element from a List.
   */
  public static Function<List<Object>, Object>  Get = args -> {
    List<Object> arg1 = (List<Object>) args.get(0);
    int offset = (Integer) args.get(1);
    if(offset < arg1.size()) {
      return Iterables.get(arg1, offset);
    }
    return null;
  };

  /**
   * Determines if a List is empty.
   */
  public static Function<List<Object>, Object> IsEmpty = args -> {
    if(args.size() == 0) {
      throw new IllegalStateException("IS_EMPTY expects one string arg");
    }
    String val = (String) args.get(0);
    return val == null || val.isEmpty() ? true : false;
  };
}
