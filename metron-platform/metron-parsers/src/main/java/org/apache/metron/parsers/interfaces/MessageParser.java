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
package org.apache.metron.parsers.interfaces;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MessageParser<T> extends Configurable {
  /**
   * Initialize the message parser.  This is done once.
   */
  void init();

  /**
   * Take raw data and convert it to a list of messages.
   *
   * @param rawMessage
   * @return If null is returned, this is treated as an empty list.
   */
  List<T> parse(byte[] rawMessage);

  /**
   * Take raw data and convert it to an optional list of messages.
   * @param parseMessage
   * @return If null is returned, this is treated as an empty list.
   */
  default Optional<List<T>> parseOptional(byte[] parseMessage) {
    return Optional.ofNullable(parse(parseMessage));
  }

  /**
   * Validate the message to ensure that it's correct.
   * @param message
   * @return true if the message is valid, false if not
   */
  boolean validate(T message);

}
