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
package org.apache.metron.common.message;

import org.apache.metron.common.utils.ConversionUtils;

import java.util.function.Function;

public enum MessageGetters {

  BYTES_FROM_POSITION((String arg) -> new BytesFromPosition(ConversionUtils.convert(arg, Integer.class))),
  JSON_FROM_POSITION((String arg) -> new JSONFromPosition(ConversionUtils.convert(arg, Integer.class))),
  JSON_FROM_FIELD((String arg) -> new JSONFromField(arg)),
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
