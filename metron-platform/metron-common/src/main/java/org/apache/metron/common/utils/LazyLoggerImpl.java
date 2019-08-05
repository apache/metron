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

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Wrapper class around a slf4j Logger interface that adds lazy evaluated method arguments
 * Useful when some arguments are costly to evaluate, and you only want to incur
 * that cost if the logging level is actually enabled.
 * We can tag parameterised varargs arguments as safe here because we only assume they are Objects
 */
public class LazyLoggerImpl implements LazyLogger {
  private final Logger logger;

  LazyLoggerImpl(final Logger logger) {
    if (logger != null) {  // Explicitly NP check to remove Guava as a dependency
      this.logger = logger;
    } else {
      throw new NullPointerException("Null logger passed");
    }
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public String getName() {
    return logger.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public void trace(String msg) {
    logger.trace(msg);
  }

  @Override
  public void trace(String format, Object arg) {
    logger.trace(format, arg);
  }

  @Override
  public void trace(String format, Supplier<Object> arg) {
    if (logger.isTraceEnabled()) {
      logger.trace(format, arg.get());
    }
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    logger.trace(format, arg1, arg2);
  }

  @Override
  public void trace(String format, Supplier<Object> arg1, Supplier<Object> arg2) {
    if (logger.isTraceEnabled()) {
      logger.trace(format, arg1.get(), arg2.get());
    }
  }

  @Override
  public void trace(String format, Object... arguments) {
    logger.trace(format, arguments);
  }

  @Override
  @SafeVarargs
  public final void trace(String format, Supplier<Object>... arguments) {
    if (logger.isTraceEnabled()) {
      logger.trace(format, Arrays.stream(arguments).map(Supplier::get).toArray());
    }
  }

  @Override
  public void trace(String msg, Throwable t) {
    logger.trace(msg, t);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return logger.isTraceEnabled(marker);
  }

  @Override
  public void trace(Marker marker, String msg) {
    logger.trace(marker, msg);
  }

  @Override
  public void trace(Marker marker, String format, Object arg) {
    logger.trace(marker, format, arg);
  }

  @Override
  public void trace(Marker marker, String format, Supplier<Object> arg) {
    if (logger.isTraceEnabled(marker)) {
      logger.trace(marker, format, arg.get());
    }
  }

  @Override
  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    logger.trace(marker, format, arg1, arg2);
  }

  @Override
  public void trace(Marker marker, String format, Supplier<Object> arg1, Supplier<Object> arg2) {
    if (logger.isTraceEnabled(marker)) {
      logger.trace(marker, format, arg1.get(), arg2.get());
    }
  }

  @Override
  public void trace(Marker marker, String format, Object... arguments) {
    logger.trace(marker, format, arguments);
  }

  @Override
  @SafeVarargs
  public final void trace(Marker marker, String format, Supplier<Object>... arguments) {
    if (logger.isTraceEnabled(marker)) {
      logger.trace(marker, format, Arrays.stream(arguments).map(Supplier::get).toArray());
    }
  }

  @Override
  public void trace(Marker marker, String msg, Throwable t) {
    logger.trace(marker, msg, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public void debug(String msg) {
    logger.debug(msg);
  }

  @Override
  public void debug(String format, Object arg) {
    logger.debug(format, arg);
  }

  @Override
  public void debug(String format, Supplier<Object> arg) {
    if (logger.isDebugEnabled()) {
      logger.debug(format, arg.get());
    }
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    logger.debug(format, arg1, arg2);
  }

  @Override
  public void debug(String format, Supplier<Object> arg1, Supplier<Object> arg2) {
    if (logger.isDebugEnabled()) {
      logger.debug(format, arg1.get(), arg2.get());
    }
  }

  @Override
  public void debug(String format, Object... arguments) {
    logger.debug(format, arguments);
  }

  @Override
  @SafeVarargs
  public final void debug(String format, Supplier<Object>... arguments) {
    if (logger.isDebugEnabled()) {
      logger.debug(format, Arrays.stream(arguments).map(Supplier::get).toArray());
    }
  }

  @Override
  public void debug(String msg, Throwable t) {
    logger.debug(msg, t);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return logger.isDebugEnabled(marker);
  }

  @Override
  public void debug(Marker marker, String msg) {
    logger.debug(marker, msg);
  }

  @Override
  public void debug(Marker marker, String format, Object arg) {
    logger.debug(marker, format, arg);
  }

  @Override
  public void debug(Marker marker, String format, Supplier<Object> arg) {
    if (logger.isDebugEnabled(marker)) {
      logger.debug(marker, format, arg.get());
    }
  }

  @Override
  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    logger.debug(marker, format, arg1, arg2);
  }

  @Override
  public void debug(Marker marker, String format, Supplier<Object> arg1, Supplier<Object> arg2) {
    if (logger.isDebugEnabled(marker)) {
      logger.debug(marker, format, arg1.get(), arg2.get());
    }
  }

  @Override
  public void debug(Marker marker, String format, Object... arguments) {
    logger.debug(marker, format, arguments);
  }

  @Override
  @SafeVarargs
  public final void debug(Marker marker, String format, Supplier<Object>... arguments) {
    if (logger.isDebugEnabled(marker)) {
      logger.debug(marker, format, Arrays.stream(arguments).map(Supplier::get).toArray());
    }
  }

  @Override
  public void debug(Marker marker, String msg, Throwable t) {
    logger.debug(marker, msg, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public void info(String msg) {
    logger.info(msg);
  }

  @Override
  public void info(String format, Object arg) {
    logger.info(format, arg);
  }

  @Override
  public void info(String format, Supplier<Object> arg) {
    if (logger.isInfoEnabled()) {
      logger.info(format, arg.get());
    }
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    logger.info(format, arg1, arg2);
  }

  @Override
  public void info(String format, Supplier<Object> arg1, Supplier<Object> arg2) {
    if (logger.isInfoEnabled()) {
      logger.info(format, arg1.get(), arg2.get());
    }
  }

  @Override
  public void info(String format, Object... arguments) {
    logger.info(format, arguments);
  }

  @Override
  @SafeVarargs
  public final void info(String format, Supplier<Object>... arguments) {
    if (logger.isInfoEnabled()) {
      logger.info(format, Arrays.stream(arguments).map(Supplier::get).toArray());
    }
  }

  @Override
  public void info(String msg, Throwable t) {
    logger.info(msg, t);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return logger.isInfoEnabled(marker);
  }

  @Override
  public void info(Marker marker, String msg) {
    logger.info(marker, msg);
  }

  @Override
  public void info(Marker marker, String format, Object arg) {
    logger.info(marker, format, arg);
  }

  @Override
  public void info(Marker marker, String format, Supplier<Object> arg) {
    if (logger.isInfoEnabled(marker)) {
      logger.info(marker, format, arg.get());
    }
  }

  @Override
  public void info(Marker marker, String format, Object arg1, Object arg2) {
    logger.info(marker, format, arg1, arg2);
  }

  @Override
  public void info(Marker marker, String format, Supplier<Object> arg1, Supplier<Object> arg2) {
    if (logger.isInfoEnabled(marker)) {
      logger.info(marker, format, arg1.get(), arg2.get());
    }
  }

  @Override
  public void info(Marker marker, String format, Object... arguments) {
    logger.info(marker, format, arguments);
  }

  @Override
  @SafeVarargs
  public final void info(Marker marker, String format, Supplier<Object>... arguments) {
    if (logger.isInfoEnabled(marker)) {
      logger.info(marker, format, Arrays.stream(arguments).map(Supplier::get).toArray());
    }
  }

  @Override
  public void info(Marker marker, String msg, Throwable t) {
    logger.info(marker, msg, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public void warn(String msg) {
    logger.warn(msg);
  }

  @Override
  public void warn(String format, Object arg) {
    logger.warn(format, arg);
  }

  @Override
  public void warn(String format, Supplier<Object> arg) {
    if (logger.isWarnEnabled()) {
      logger.warn(format, arg.get());
    }
  }

  @Override
  public void warn(String format, Object... arguments) {
    logger.warn(format, arguments);
  }

  @Override
  @SafeVarargs
  public final void warn(String format, Supplier<Object>... arguments) {
    if (logger.isWarnEnabled()) {
      logger.warn(format, Arrays.stream(arguments).map(Supplier::get).toArray());
    }
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    logger.warn(format, arg1, arg2);
  }

  @Override
  public void warn(String format, Supplier<Object> arg1, Supplier<Object> arg2) {
    if (logger.isWarnEnabled()) {
      logger.warn(format, arg1.get(), arg2.get());
    }
  }

  @Override
  public void warn(String msg, Throwable t) {
    logger.warn(msg, t);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return logger.isWarnEnabled(marker);
  }

  @Override
  public void warn(Marker marker, String msg) {
    logger.warn(marker, msg);
  }

  @Override
  public void warn(Marker marker, String format, Object arg) {
    logger.warn(marker, format, arg);
  }

  @Override
  public void warn(Marker marker, String format, Supplier<Object> arg) {
    if (logger.isWarnEnabled(marker)) {
      logger.warn(marker, format, arg.get());
    }
  }

  @Override
  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    logger.warn(marker, format, arg1, arg2);
  }

  @Override
  public void warn(Marker marker, String format, Supplier<Object> arg1, Supplier<Object> arg2) {
    if (logger.isWarnEnabled(marker)) {
      logger.warn(marker, format, arg1.get(), arg2.get());
    }
  }

  @Override
  public void warn(Marker marker, String format, Object... arguments) {
    logger.warn(marker, format, arguments);
  }

  @Override
  @SafeVarargs
  public final void warn(Marker marker, String format, Supplier<Object>... arguments) {
    if (logger.isWarnEnabled(marker)) {
      logger.warn(marker, format, Arrays.stream(arguments).map(Supplier::get).toArray());
    }
  }

  @Override
  public void warn(Marker marker, String msg, Throwable t) {
    logger.warn(marker, msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public void error(String msg) {
    logger.error(msg);
  }

  @Override
  public void error(String format, Object arg) {
    logger.error(format, arg);
  }

  @Override
  public void error(String format, Supplier<Object> arg) {
    if (logger.isErrorEnabled()) {
      logger.error(format, arg.get());
    }
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    logger.error(format, arg1, arg2);
  }

  @Override
  public void error(String format, Supplier<Object> arg1, Supplier<Object> arg2) {
    if (logger.isErrorEnabled()) {
      logger.error(format, arg1.get(), arg2.get());
    }
  }

  @Override
  public void error(String format, Object... arguments) {
    logger.error(format, arguments);
  }

  @Override
  @SafeVarargs
  public final void error(String format, Supplier<Object>... arguments) {
    if (logger.isErrorEnabled()) {
      logger.error(format, Arrays.stream(arguments).map(Supplier::get).toArray());
    }
  }

  @Override
  public void error(String msg, Throwable t) {
    logger.error(msg, t);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return logger.isErrorEnabled(marker);
  }

  @Override
  public void error(Marker marker, String msg) {
    logger.error(marker, msg);
  }

  @Override
  public void error(Marker marker, String format, Object arg) {
    logger.error(marker, format, arg);
  }

  @Override
  public void error(Marker marker, String format, Supplier<Object> arg) {
    if (logger.isErrorEnabled(marker)) {
      logger.error(marker, format, arg.get());
    }
  }

  @Override
  public void error(Marker marker, String format, Object arg1, Object arg2) {
    logger.error(marker, format, arg1, arg2);
  }

  @Override
  public final void error(Marker marker, String format, Supplier<Object> arg1, Supplier<Object> arg2) {
    if (logger.isErrorEnabled(marker)) {
      logger.error(marker, format, arg1.get(), arg2.get());
    }
  }

  @Override
  public void error(Marker marker, String format, Object... arguments) {
    logger.error(marker, format, arguments);
  }

  @Override
  @SafeVarargs
  public final void error(Marker marker, String format, Supplier<Object>... arguments) {
    if (logger.isErrorEnabled(marker)) {
      logger.error(marker, format, Arrays.stream(arguments).map(Supplier::get).toArray());
    }
  }

  @Override
  public void error(Marker marker, String msg, Throwable t) {
    logger.error(marker, msg, t);
  }
}
