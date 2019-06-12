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

package org.apache.metron.storm.common.utils;

import org.apache.metron.common.Constants;
import org.apache.metron.common.error.MetronError;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Values;

import java.util.Optional;

public class StormErrorUtils {
  /**
   * Handles a {@link MetronError} that occurs.
   *
   * @param collector The Storm output collector being reported to
   * @param error The error that occurred
   */
  public static void handleError(OutputCollector collector, MetronError error)
  {
    collector.emit(Constants.ERROR_STREAM, new Values(error.getJSONObject()));
    Optional<Throwable> throwable = error.getThrowable();
    if (throwable.isPresent()) {
      collector.reportError(throwable.get());
    }

  }
}
