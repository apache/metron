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
package org.apache.metron.storm.common.message;

import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.util.function.Function;

/**
 * MessageGetters is a convenience enum for looking up the various implementations of MessageGetStrategy. The MessageGetStrategy
 * abstraction returns a value from a tuple.  The implementations include:
 * <ul>
 *   <li>BYTES_FROM_POSITION - gets a byte array from the provided position</li>
 *   <li>JSON_FROM_POSITION - gets a byte array from the provided position then converts to a string and parses the string to JSON</li>
 *   <li>JSON_FROM_FIELD - gets a JSONObject from the provided field</li>
 *   <li>OBJECT_FROM_FIELD - gets an Object from the provided field</li>
 *   <li>DEFAULT_BYTES_FROM_POSITION - gets a byte array from position 0</li>
 *   <li>DEFAULT_JSON_FROM_POSITION - gets a byte array from position 0 then converts to a string and parses the string to JSON</li>
 *   <li>DEFAULT_JSON_FROM_FIELD - gets a JSONObject from the "message" field</li>
 *   <li>DEFAULT_OBJECT_FROM_FIELD - gets an Object from the "message" field</li>
 * </ul>
 *
 */
public enum MessageGetters {

  BYTES_FROM_POSITION((String arg) -> new BytesFromPosition(ConversionUtils.convert(arg, Integer.class))),
  JSON_FROM_POSITION((String arg) -> new JSONFromPosition(ConversionUtils.convert(arg, Integer.class))),
  JSON_FROM_FIELD((String arg) -> new JSONFromField(arg)),
  JSON_FROM_FIELD_BY_REFERENCE((String arg) -> new JSONFromFieldByReference(arg)),
  OBJECT_FROM_FIELD((String arg) -> new ObjectFromField(arg)),
  DEFAULT_BYTES_FROM_POSITION(new BytesFromPosition()),
  DEFAULT_JSON_FROM_POSITION(new JSONFromPosition()),
  DEFAULT_JSON_FROM_FIELD(new JSONFromField()),
  DEFAULT_OBJECT_FROM_FIELD(new ObjectFromField());

  Function<String, MessageGetStrategy> messageGetStrategyFunction;
  MessageGetStrategy messageGetStrategy;

  MessageGetters(MessageGetStrategy messageGetStrategy) {
    this.messageGetStrategy = messageGetStrategy;
  }

  MessageGetters(Function<String, MessageGetStrategy> messageGetStrategy) {
    this.messageGetStrategyFunction = messageGetStrategy;
  }

  public MessageGetStrategy get(String arg) {
    return messageGetStrategyFunction.apply(arg);
  }

  public MessageGetStrategy get() {
    return messageGetStrategy;
  }
}
