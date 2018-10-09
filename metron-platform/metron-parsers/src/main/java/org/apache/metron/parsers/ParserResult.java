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
package org.apache.metron.parsers;

import org.apache.metron.common.error.MetronError;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.Objects;

public class ParserResult {

  private String sensorType;
  private JSONObject message;
  private MetronError error;
  private byte[] originalMessage;

  public ParserResult(String sensorType, JSONObject message, byte[] originalMessage) {
    this.sensorType = sensorType;
    this.message = message;
    this.originalMessage = originalMessage;
  }

  public ParserResult(String sensorType, MetronError error, byte[] originalMessage) {
    this.sensorType = sensorType;
    this.error = error;
    this.originalMessage = originalMessage;
  }

  public String getSensorType() {
    return sensorType;
  }

  public JSONObject getMessage() {
    return message;
  }

  public MetronError getError() {
    return error;
  }

  public byte[] getOriginalMessage() {
    return originalMessage;
  }

  public boolean isError() {
    return error != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ParserResult parserResult = (ParserResult) o;
    return Objects.equals(sensorType, parserResult.getSensorType()) &&
            Objects.equals(message, parserResult.getMessage()) &&
            Objects.equals(error, parserResult.getError()) &&
            Arrays.equals(originalMessage, parserResult.getOriginalMessage());
  }

  @Override
  public int hashCode() {
    int result = sensorType != null ? sensorType.hashCode() : 0;
    result = 31 * result + (message != null ? message.hashCode() : 0);
    result = 31 * result + (error != null ? error.hashCode() : 0);
    result = 31 * result + (originalMessage != null ? Arrays.hashCode(originalMessage) : 0);
    return result;
  }
}
