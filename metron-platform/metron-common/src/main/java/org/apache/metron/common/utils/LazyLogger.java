/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.common.utils;

import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.function.Supplier;

/**
 * Extension of a Logger interface that supports lazy argument evaluation
 * Useful when some arguments are costly to evaluate, and you only want to incur
 * that cost if the logging level is actually enabled.
 *
 * Please note this interface is a temporary measure until the move to use SLF4J 2.x
 */
public interface LazyLogger extends Logger {
  Logger getLogger();

  void trace(String format, Supplier<Object> arg);

  void trace(String format, Supplier<Object> arg1, Supplier<Object> arg2);

  void trace(String format, Supplier<Object>... arguments);

  void trace(Marker marker, String format, Supplier<Object> arg);

  void trace(Marker marker, String format, Supplier<Object> arg1, Supplier<Object> arg2);

  void trace(Marker marker, String format, Supplier<Object>... arguments);

  void debug(String format, Supplier<Object> arg);

  void debug(String format, Supplier<Object> arg1, Supplier<Object> arg2);

  void debug(String format, Supplier<Object>... arguments);

  void debug(Marker marker, String format, Supplier<Object> arg);

  void debug(Marker marker, String format, Supplier<Object> arg1, Supplier<Object> arg2);

  void debug(Marker marker, String format, Supplier<Object>... arguments);

  void info(String format, Supplier<Object> arg);

  void info(String format, Supplier<Object> arg1, Supplier<Object> arg2);

  void info(String format, Supplier<Object>... arguments);

  void info(Marker marker, String format, Supplier<Object> arg);

  void info(Marker marker, String format, Supplier<Object> arg1, Supplier<Object> arg2);

  void info(Marker marker, String format, Supplier<Object>... arguments);

  void warn(String format, Supplier<Object> arg);

  void warn(String format, Supplier<Object> arg1, Supplier<Object> arg2);

  void warn(String format, Supplier<Object>... arguments);

  void warn(Marker marker, String format, Supplier<Object> arg);

  void warn(Marker marker, String format, Supplier<Object> arg1, Supplier<Object> arg2);

  void warn(Marker marker, String format, Supplier<Object>... arguments);

  void error(String format, Supplier<Object> arg);

  void error(String format, Supplier<Object> arg1, Supplier<Object> arg2);

  void error(String format, Supplier<Object>... arguments);

  void error(Marker marker, String format, Supplier<Object> arg);

  void error(Marker marker, String format, Supplier<Object> arg1, Supplier<Object> arg2);

  void error(Marker marker, String format, Supplier<Object>... arguments);
}
