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

package org.apache.metron.profiler.spark;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;

/**
 * Parses an input string and returns a timestamp in epoch milliseconds.
 */
public class TimestampParser {

  /**
   * Parses an input string and returns an optional timestamp in epoch milliseconds.
   *
   * @param inputString The input defining a timestamp.
   * @return A timestamp in epoch milliseconds.
   */
  public Optional<Long> parse(String inputString) {
    Optional<Long> epochMilli = Optional.empty();

    // a blank is acceptable and treated as undefined
    if (StringUtils.isNotBlank(inputString)) {
      epochMilli = Optional.of(new DateTimeFormatterBuilder()
              .append(DateTimeFormatter.ISO_INSTANT)
              .toFormatter()
              .parse(inputString, Instant::from)
              .toEpochMilli());
    }

    return epochMilli;
  }

}
