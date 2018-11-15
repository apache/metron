/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.metron.management;

import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.util.List;

import static java.lang.String.format;

/**
 * Contains utility functionality that is useful across all of the Stellar management functions.
 */
public class Functions {

  /**
   * Get an argument from the Stellar function arguments
   *
   * @param argName The name of the argument.
   * @param index The index within the list of arguments.
   * @param clazz The type expected.
   * @param args All of the arguments.
   * @param <T> The type of the argument expected.
   */
  public static <T> T getArg(String argName, int index, Class<T> clazz, List<Object> args) {
    if(index >= args.size()) {
      String msg = format("missing '%s'; expected at least %d argument(s), found %d", argName, index+1, args.size());
      throw new IllegalArgumentException(msg);
    }

    return ConversionUtils.convert(args.get(index), clazz);
  }

  /**
   * Returns true if an argument of a specific type at a given index exists.  Otherwise returns
   * false if an argument does not exist at the index or is not the expected type.
   *
   * @param argName The name of the argument.
   * @param index The index within the list of arguments.
   * @param clazz The type expected.
   * @param args All of the arguments.
   * @param <T> The type of argument expected.
   */
  public static <T> boolean hasArg(String argName, int index, Class<T> clazz, List<Object> args) {
    boolean result = false;

    if(args.size() > index) {
      if(clazz.isAssignableFrom(args.get(index).getClass())) {
        return true;
      }
    }

    return result;
  }
}
