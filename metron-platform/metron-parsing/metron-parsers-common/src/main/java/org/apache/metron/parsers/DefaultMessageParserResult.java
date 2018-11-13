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

package org.apache.metron.parsers;

import org.apache.metron.parsers.interfaces.MessageParserResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultMessageParserResult<T> implements MessageParserResult<T> {
  private List<T> messages = new ArrayList<>();
  private Map<Object, Throwable> errors = new HashMap<>();
  private Throwable masterThrowable;

  public DefaultMessageParserResult() {
  }

  public DefaultMessageParserResult(Throwable masterThrowable) {
    this.masterThrowable = masterThrowable;
  }

  public DefaultMessageParserResult(List<T> list) {
    messages.addAll(list);
  }

  public DefaultMessageParserResult(Map<Object, Throwable> map) {
    errors.putAll(map);
  }

  public DefaultMessageParserResult(List<T> list, Map<Object, Throwable> map) {
    messages.addAll(list);
    errors.putAll(map);
  }

  public void addMessage(T message) {
    messages.add(message);
  }

  public void addError(Object message, Throwable throwable) {
    errors.put(message, throwable);
  }

  @Override
  public List<T> getMessages() {
    return messages;
  }

  @Override
  public Map<Object, Throwable> getMessageThrowables() {
    return errors;
  }

  @Override
  public Optional<Throwable> getMasterThrowable() {
    return Optional.ofNullable(masterThrowable);
  }
}
