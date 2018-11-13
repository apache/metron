/*
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Result object MessageParser calls.
 * @param <T>
 */
public interface MessageParserResult<T> {
  /**
   * Returns the Message objects of {@code T}
   * @return {@code List}
   */
  List<T> getMessages();

  /**
   * Returns a map of raw message objects to the {@code Throwable} they triggered.
   * @return {@code Map}
   */
  Map<Object,Throwable> getMessageThrowables();

  /**
   * Returns a master {@code Throwable} for a parse call.  This represents a complete
   * call failure, as opposed to one associated with a message.
   * @return {@code Optional}{@code Throwable}
   */
  Optional<Throwable> getMasterThrowable();
}
