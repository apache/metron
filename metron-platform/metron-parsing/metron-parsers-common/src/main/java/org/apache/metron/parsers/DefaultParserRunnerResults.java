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

import org.apache.metron.common.error.MetronError;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of ParserRunnerResults.
 */
public class DefaultParserRunnerResults implements ParserRunnerResults<JSONObject> {

  private List<JSONObject> messages = new ArrayList<>();
  private List<MetronError> errors = new ArrayList<>();

  public List<JSONObject> getMessages() {
    return messages;
  }

  public List<MetronError> getErrors() {
    return errors;
  }

  public void addMessage(JSONObject message) {
    this.messages.add(message);
  }

  public void addError(MetronError error) {
    this.errors.add(error);
  }

  public void addErrors(List<MetronError> errors) {
    this.errors.addAll(errors);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ParserRunnerResults parserResult = (ParserRunnerResults) o;
    return Objects.equals(messages, parserResult.getMessages()) &&
            Objects.equals(errors, parserResult.getErrors());
  }

  @Override
  public int hashCode() {
    int result = messages != null ? messages.hashCode() : 0;
    result = 31 * result + (errors != null ? errors.hashCode() : 0);
    return result;
  }
}