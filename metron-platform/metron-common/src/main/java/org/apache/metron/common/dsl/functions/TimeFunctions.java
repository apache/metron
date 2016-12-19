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

import org.apache.metron.common.dsl.BaseStellarFunction;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.utils.ConversionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.upperCase;

/**
 * Stellar functions that operate on time.
 */
public class TimeFunctions {

  @Stellar(name = "MILLIS",
          description = "Converts a time duration to milliseconds.",
          params = { "duration - The duration of time. ",
                  "units - The units of the time duration; seconds, minutes, hours" },
          returns = "The duration in milliseconds as a Long.")
  public static class Millis extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {

      long duration = getArg(0, args, Long.class);
      TimeUnit units = TimeUnit.valueOf(upperCase(getArg(1, args, String.class)));
      return units.toMillis(duration);
    }
  }

  /**
   * Get an argument from a list of arguments.
   * @param index The index within the list of arguments.
   * @param args All of the arguments.
   * @param clazz The type expected.
   * @param <T> The type of the argument expected.
   */
  private static <T> T getArg(int index, List<Object> args, Class<T> clazz) {
    if(index >= args.size()) {
      throw new IllegalArgumentException(format("expected at least %d argument(s), found %d", index+1, args.size()));
    }

    return ConversionUtils.convert(args.get(index), clazz);
  }
}
