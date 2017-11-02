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
package org.apache.metron.common.error;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.apache.metron.common.Constants.ErrorType;
import org.apache.metron.common.utils.HashUtils;
import org.json.simple.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.metron.common.Constants.ERROR_TYPE;
import static org.apache.metron.common.Constants.ErrorFields;

public class MetronError {

  private String message;
  private Throwable throwable;
  private String sensorType = ERROR_TYPE;
  private ErrorType errorType = ErrorType.DEFAULT_ERROR;
  private Set<String> errorFields;
  private List<Object> rawMessages;

  public MetronError withMessage(String message) {
    this.message = message;
    return this;
  }

  public MetronError withThrowable(Throwable throwable) {
    this.throwable = throwable;
    return this;
  }

  public MetronError withSensorType(String sensorType) {
    this.sensorType = sensorType;
    return this;
  }

  public MetronError withErrorType(ErrorType errorType) {
    this.errorType = errorType;
    return this;
  }

  public MetronError withErrorFields(Set<String> errorFields) {
    this.errorFields = errorFields;
    return this;
  }


  public MetronError addRawMessage(Object rawMessage) {
    if (rawMessage != null) {
      if (this.rawMessages == null) {
        this.rawMessages = new ArrayList<>();
      }
      this.rawMessages.add(rawMessage);
    }
    return this;
  }

  public MetronError withRawMessages(List<Object> rawMessages) {
    this.rawMessages = rawMessages;
    return this;
  }

  public Optional<Throwable> getThrowable() {
    return throwable != null ? Optional.of(throwable) : Optional.empty();
  }

  @SuppressWarnings({"unchecked"})
  public JSONObject getJSONObject() {
    JSONObject errorMessage = new JSONObject();
    errorMessage.put(Constants.SENSOR_TYPE, "error");
    errorMessage.put(ErrorFields.FAILED_SENSOR_TYPE.getName(), sensorType);
    errorMessage.put(ErrorFields.ERROR_TYPE.getName(), errorType.getType());

    addMessageString(errorMessage);
		addStacktrace(errorMessage);
    addTimestamp(errorMessage);
    addHostname(errorMessage);
    addRawMessages(errorMessage);
    addErrorHash(errorMessage);

    return errorMessage;
  }

  @SuppressWarnings({"unchecked"})
  private void addMessageString(JSONObject errorMessage) {
    if (message != null) {
      errorMessage.put(ErrorFields.MESSAGE.getName(), message);
    } else if (throwable != null) {
      errorMessage.put(ErrorFields.MESSAGE.getName(), throwable.getMessage());
    }
  }

  @SuppressWarnings({"unchecked"})
  private void addStacktrace(JSONObject errorMessage) {
    if (throwable != null) {
      String stackTrace = ExceptionUtils.getStackTrace(throwable);
      String exception = throwable.toString();
      errorMessage.put(ErrorFields.EXCEPTION.getName(), exception);
      errorMessage.put(ErrorFields.STACK.getName(), stackTrace);
    }
  }

  @SuppressWarnings({"unchecked"})
  private void addTimestamp(JSONObject errorMessage) {
    errorMessage.put(ErrorFields.TIMESTAMP.getName(), System.currentTimeMillis());
  }

  @SuppressWarnings({"unchecked"})
  private void addHostname(JSONObject errorMessage) {
    try {
      errorMessage.put(ErrorFields.HOSTNAME.getName(), InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException ex) {
      // Leave the hostname field off if it cannot be found
    }
  }

  @SuppressWarnings({"unchecked"})
  private void addRawMessages(JSONObject errorMessage) {
    if(rawMessages != null) {
      for(int i = 0; i < rawMessages.size(); i++) {
        Object rawMessage = rawMessages.get(i);
        // If multiple messages are included add an index to the field name, otherwise leave it off
        String rawMessageField = rawMessages.size() == 1 ? ErrorFields.RAW_MESSAGE.getName() : ErrorFields.RAW_MESSAGE.getName() + "_" + i;
        // It's unclear if we need a rawMessageBytes field so commenting out for now
        //String rawMessageBytesField = rawMessages.size() == 1 ? ErrorFields.RAW_MESSAGE_BYTES.getName() : ErrorFields.RAW_MESSAGE_BYTES.getName() + "_" + i;
        if(rawMessage instanceof byte[]) {
          errorMessage.put(rawMessageField, Bytes.toString((byte[])rawMessage));
          //errorMessage.put(rawMessageBytesField, com.google.common.primitives.Bytes.asList((byte[])rawMessage));
        } else if (rawMessage instanceof JSONObject) {
          JSONObject rawMessageJSON = (JSONObject) rawMessage;
          String rawMessageJSONString = rawMessageJSON.toJSONString();
          errorMessage.put(rawMessageField, rawMessageJSONString);
          //errorMessage.put(rawMessageBytesField, com.google.common.primitives.Bytes.asList(rawMessageJSONString.getBytes(UTF_8)));
        } else {
          errorMessage.put(rawMessageField, rawMessage.toString());
          //errorMessage.put(rawMessageBytesField, com.google.common.primitives.Bytes.asList(rawMessage.toString().getBytes(UTF_8)));
        }
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  private void addErrorHash(JSONObject errorMessage) {
    if (rawMessages != null && rawMessages.size() == 1) {
      Object rawMessage = rawMessages.get(0);
      if (rawMessage instanceof JSONObject) {
        JSONObject rawJSON = (JSONObject) rawMessage;
        if (errorFields != null) {
          errorMessage.put(ErrorFields.ERROR_FIELDS.getName(), String.join(",", errorFields));
          errorMessage.put(ErrorFields.ERROR_HASH.getName(), HashUtils.getMessageHash(rawJSON, errorFields));
        } else {
          errorMessage.put(ErrorFields.ERROR_HASH.getName(), HashUtils.getMessageHash(rawJSON));
        }
      } else if (rawMessage instanceof byte[]) {
        errorMessage.put(ErrorFields.ERROR_HASH.getName(), HashUtils.getMessageHash((byte[])rawMessage));
      } else {
        errorMessage.put(ErrorFields.ERROR_HASH.getName(), HashUtils.getMessageHash(rawMessage.toString().getBytes(UTF_8)));
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MetronError that = (MetronError) o;

    if (message != null ? !message.equals(that.message) : that.message != null)
      return false;
    if (throwable != null ? !throwable.equals(that.throwable) : that.throwable != null)
      return false;
    if (sensorType != null ? !sensorType.equals(that.sensorType) : that.sensorType != null)
      return false;
    if (errorType != null ? !errorType.equals(that.errorType) : that.errorType != null)
      return false;
    if (errorFields != null ? !errorFields.equals(that.errorFields) : that.errorFields != null)
      return false;
    return rawMessages != null ? rawMessages.equals(that.rawMessages) : that.rawMessages == null;

  }

  @Override
  public int hashCode() {
    int result = message != null ? message.hashCode() : 0;
    result = 31 * result + (throwable != null ? throwable.hashCode() : 0);
    result = 31 * result + (sensorType != null ? sensorType.hashCode() : 0);
    result = 31 * result + (errorType != null ? errorType.hashCode() : 0);
    result = 31 * result + (errorFields != null ? errorFields.hashCode() : 0);
    result = 31 * result + (rawMessages != null ? rawMessages.hashCode() : 0);
    return result;
  }

}
