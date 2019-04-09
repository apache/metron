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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.metron.common.Constants.ERROR_TYPE;
import static org.apache.metron.common.Constants.ErrorFields;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.apache.metron.common.Constants.ErrorType;
import org.apache.metron.common.utils.HashUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class MetronError {

  private String message;
  private Throwable throwable;
  private Set<String> sensorTypes = Collections.singleton(ERROR_TYPE);
  private ErrorType errorType = ErrorType.DEFAULT_ERROR;
  private Set<String> errorFields;
  private List<Object> rawMessages;
  private Map<String, Object> metadata = new HashMap<>();

  public MetronError withMessage(String message) {
    this.message = message;
    return this;
  }

  public MetronError withThrowable(Throwable throwable) {
    this.throwable = throwable;
    return this;
  }

  public MetronError withSensorType(Set<String> sensorTypes) {
    this.sensorTypes = sensorTypes;
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

  public MetronError withMetadata(Map<String, Object> metadata) {
    this.metadata.putAll(metadata);
    return this;
  }

  /**
   * Adds a rawMessage to the error. Calls can be chained, as the method returns this.
   *
   * @param rawMessage The raw message to add
   * @return this, to allow for call chaining
   */
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

  /**
   * Serializes the MetronError into a JSON object.
   *
   * @return The resulting json object
   */
  @SuppressWarnings({"unchecked"})
  public JSONObject getJSONObject() {
    JSONObject errorMessage = new JSONObject();
    errorMessage.put(Constants.GUID, UUID.randomUUID().toString());
    errorMessage.put(Constants.SENSOR_TYPE, Constants.ERROR_TYPE);
    errorMessage.put(ErrorFields.ERROR_TYPE.getName(), errorType.getType());
    addFailedSensorType(errorMessage);
    addMessageString(errorMessage);
		addStacktrace(errorMessage);
    addTimestamp(errorMessage);
    addHostname(errorMessage);
    addRawMessages(errorMessage);
    addErrorHash(errorMessage);
    addMetadata(errorMessage);

    return errorMessage;
  }

  private void addFailedSensorType(JSONObject errorMessage) {
    if (sensorTypes.size() == 1) {
      errorMessage.put(ErrorFields.FAILED_SENSOR_TYPE.getName(), sensorTypes.iterator().next());
    } else {
      errorMessage.put(ErrorFields.FAILED_SENSOR_TYPE.getName(), new JSONArray().addAll(sensorTypes));
    }
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

  private void addMetadata(JSONObject errorMessage) {
    if(metadata != null && metadata.keySet().size() > 0) {
      // add each metadata element directly to the message. each metadata key already has
      // a standard prefix, no need to add another prefix to avoid collisions. this mimics
      // the behavior of merging metadata.
      errorMessage.putAll(metadata);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MetronError)) return false;
    MetronError that = (MetronError) o;
    return Objects.equals(message, that.message) &&
            Objects.equals(throwable, that.throwable) &&
            Objects.equals(sensorTypes, that.sensorTypes) &&
            errorType == that.errorType &&
            Objects.equals(errorFields, that.errorFields) &&
            Objects.equals(rawMessages, that.rawMessages) &&
            Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, throwable, sensorTypes, errorType, errorFields, rawMessages, metadata);
  }
}
