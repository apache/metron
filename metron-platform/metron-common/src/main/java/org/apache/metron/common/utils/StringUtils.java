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

package org.apache.metron.common.utils;

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.Optional;

public class StringUtils {

  /**
   * Joins string optionals potentially containing string with a delimiter.
   *
   * @param delim The delimiter
   * @param parts The string optionals to join
   * @return The joined string, without any missing optionals.
   */
  public static String join(String delim, Optional<String>... parts) {
    return Joiner.on(delim).join(
            Arrays.asList(parts).stream().filter(
                    part -> part.isPresent()
            ).map( part -> part.get())
             .toArray()
                                );
  }

  /**
   * Strips specified number of lines from beginning for String val.
   * @param val The input string
   * @param numLines The number of lines to remove
   */
  public static String stripLines(String val, int numLines) {
    int start = org.apache.commons.lang3.StringUtils.ordinalIndexOf(val, System.lineSeparator(), numLines);
    start = start >= 0 ? start : 0;
    return val.substring(start);
  }

}
