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

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * Factory class for a logger wrapper that supports lazy evaluation of arguments
 * Useful when some arguments are costly to evaluate, and you only want to incur
 * that cost if the logging level is actually enabled.
 */
public class LazyLoggerFactory {
  /**
   * Return a logger named according to the name parameter using the statically
   * bound {@link ILoggerFactory} instance.
   *
   * @param name The name of the logger.
   * @return logger
   */
  public static LazyLogger getLogger(String name) {
    final Logger logger = org.slf4j.LoggerFactory.getLogger(name);
    if (logger != null) {  // explicit NP check to remove guava dependency
      return new LazyLoggerImpl(logger);
    } else {
      throw new NullPointerException(String.format("Logger not returned for class %s",
              name == null ? "Null String" : name));
    }
  }

  /**
   * Return a logger named corresponding to the class passed as parameter, using
   * the statically bound {@link ILoggerFactory} instance.
   *
   * @param clazz the returned logger will be named after clazz
   * @return logger
   */
  public static LazyLogger getLogger(Class clazz) {
    return getLogger(clazz.getName());
  }


  /**
   * Return a lazylogger wrapping the passed Logger instance
   *
   * @param logger the returned logger will be named after clazz
   * @return logger
   */
  public static LazyLogger getLogger(Logger logger) {
    if (logger != null) {
      return new LazyLoggerImpl(logger);
    } else {
      throw new NullPointerException("Null logger passed");
    }
  }
}
