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

package org.apache.metron.common.performance;

import java.util.Map;
import java.util.function.Supplier;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class PerformanceLogger {

  private static final String LOG_PERCENT = "performance.logging.percent.records";
  private static final Integer LOG_PERCENT_DEFAULT = 1;
  private Supplier<Map<String, Object>> configSupplier;
  private ThresholdCalculator thresholdCalc;
  private Timing timing;
  private Logger logger;

  /**
   * Minimum constructor for this class.
   * <p>
   *   Options:
   * </p>
   * <ul>
   *   <li>performance.logging.percent.records = Integer value 1-100 indicating percent probability
   *        that a logging statement should trigger writing timing info.
   *   </li>
   * </ul>
   *
   * @param configSupplier provides configuration for the logger as a Map&lt;String, Object&gt;
   * @param loggerName     name for the underlying logger. This name is used when enabling/disabling
   *     the logger.
   */
  public PerformanceLogger(Supplier<Map<String, Object>> configSupplier, String loggerName) {
    this(configSupplier, LoggerFactory.getLogger(loggerName), new ThresholdCalculator(),
        new Timing());
  }

  /**
   * Constructor that allows more fine grained control.
   *
   * @param configSupplier provides configuration for the logger as a Map&lt;String, Object&gt;
   * @param logger The actual logger to be used
   * @param thresholdCalc The {@link ThresholdCalculator} to use
   * @param timing The {@link Timing} to use
   */
  public PerformanceLogger(Supplier<Map<String, Object>> configSupplier, Logger logger,
      ThresholdCalculator thresholdCalc, Timing timing) {
    this.configSupplier = configSupplier;
    this.thresholdCalc = thresholdCalc;
    this.timing = timing;
    this.logger = logger;
    this.logger.info("{} set to {}", LOG_PERCENT, getPercentThreshold());
  }

  /**
   * Marks a timer start. Works in conjunction with the log methods. Calling log after
   * calling mark will log elapsed time for the provided markName.
   *
   * @param markName The name of the mark to use
   */
  public void mark(String markName) {
    timing.mark(markName);
  }

  /**
   * Log a message at DEBUG level for the given markName.
   * Warns when logging for a markName that hasn't been set.
   *
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the DEBUG level.</p>
   *
   * @param markName  name of the marked timer to log elapsed time for
   */
  public void log(String markName) {
    if (okToLog()) {
      log(markName, "");
    }
  }

  /**
   * Log a message at DEBUG level for the given markName according to the specified message.
   * Warns when logging for a markName that hasn't been set.
   *
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the DEBUG level.</p>
   *
   * @param markName  name of the marked timer to log elapsed time for
   * @param message   message to log
   */
  public void log(String markName, String message) {
    if (okToLog()) {
      if (timing.exists(markName)) {
        logger.debug("markName={},time(ns)={},message={}", markName, timing.getElapsed(markName),
            message);
      } else {
        logger.debug("markName={},time(ns)={},message={}", "WARNING - MARK NOT SET",
            timing.getElapsed(markName), message);
      }
    }
  }

  private boolean okToLog() {
    return logger.isDebugEnabled() && thresholdCalc.isPast(getPercentThreshold());
  }

  private Integer getPercentThreshold() {
    return ConversionUtils.convert(getProperty(LOG_PERCENT, LOG_PERCENT_DEFAULT), Integer.class);
  }

  private Object getProperty(String key, Object defaultValue) {
    return configSupplier.get().getOrDefault(key, defaultValue);
  }


  /**
   * Log a message at DEBUG level for the given markName according to the specified format
   * and argument. Warns when logging for a markName that hasn't been set.
   *
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the DEBUG level.</p>
   *
   * @param markName  name of the marked timer to log elapsed time for
   * @param format    the format string
   * @param arg       argument to the format String
   */
  public void log(String markName, String format, Object arg) {
    if (okToLog()) {
      FormattingTuple formattedMessage = MessageFormatter.format(format, arg);
      log(markName, formattedMessage.getMessage());
    }
  }

  /**
   * Log a message at DEBUG level according to the specified format and argument.
   *
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the DEBUG level.</p>
   *
   * @param markName  name of the marked timer to log elapsed time for
   * @param format    the format string
   * @param arg1      first argument to the format String
   * @param arg2      second argument to the format String
   */
  public void log(String markName, String format, Object arg1, Object arg2) {
    if (okToLog()) {
      FormattingTuple formattedMessage = MessageFormatter.format(format, arg1, arg2);
      log(markName, formattedMessage.getMessage());
    }
  }

  /**
   * Log a message at DEBUG level according to the specified format and arguments.
   *
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the DEBUG level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for DEBUG. The variants taking
   * {@link #log(String, String, Object) one} and {@link #log(String, String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *
   * @param markName  name of the marked timer to log elapsed time for
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void log(String markName, String format, Object... arguments) {
    if (okToLog()) {
      FormattingTuple formattedMessage = MessageFormatter.arrayFormat(format, arguments);
      log(markName, formattedMessage.getMessage());
    }
  }

  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

}
