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
package org.apache.metron.common.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Values;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.json.simple.JSONObject;

public class ErrorUtils {

  @SuppressWarnings("unchecked")
  public static JSONObject generateErrorMessage(String message, Throwable t)
  {
    return generateErrorMessage(message, t, Optional.empty(), Optional.empty());
  }
  public static JSONObject generateErrorMessage(String message
                                               , Throwable t
                                               , Optional<String> sensorType
                                               , Optional<Object> rawMessage
                                               )
  {
    JSONObject error_message = new JSONObject();
		
		/*
		 * Save full stack trace in object.
		 */
    String stackTrace = ExceptionUtils.getStackTrace(t);

    String exception = t.toString();

    error_message.put("time", System.currentTimeMillis());
    try {
      error_message.put("hostname", InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException ex) {

    }
    if(rawMessage.isPresent()) {
      if(rawMessage.get() instanceof byte[]) {
        error_message.put("rawMessage", Bytes.toString((byte[])rawMessage.get()));
        error_message.put("rawMessage_bytes", toByteArrayList((byte[])rawMessage.get()));
      }
      else {
        error_message.put("rawMessage", rawMessage.get());
      }
    }
    error_message.put("message", message);
    error_message.put(Constants.SENSOR_TYPE, StringUtils.join("_", sensorType, Optional.of("error")));
    error_message.put("exception", exception);
    error_message.put("stack", stackTrace);

    return error_message;
  }

  private static List<Byte> toByteArrayList(byte[] list) {
    List<Byte> ret = new ArrayList<>();
    for(byte b : list) {
      ret.add(b);
    }
    return ret;
  }

  public static void handleError(OutputCollector collector, Throwable t, String errorStream) {
    handleError(collector, t, errorStream, Optional.empty(), Optional.empty());
  }
  public static void handleError(OutputCollector collector
                                , Throwable t
                                , String errorStream
                                , Optional<String> sensorType
                                , Optional<Object> rawMessage
                                )
  {
    JSONObject error = ErrorUtils.generateErrorMessage(t.getMessage(), t, sensorType, rawMessage);
    collector.emit(errorStream, new Values(error));
    collector.reportError(t);
  }
}
