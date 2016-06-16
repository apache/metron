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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.metron.common.Constants;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

public class ErrorUtils {
  private final static Logger LOGGER = LoggerFactory.getLogger(ErrorUtils.class);

  public enum RuntimeErrors {
    ILLEGAL_ARG(t -> new IllegalArgumentException(formatReason(t), t.getRight().orElse(null))),
    ILLEGAL_STATE(t -> new IllegalStateException(formatReason(t), t.getRight().orElse(null)));

    Function<Pair<String, Optional<Throwable>>, RuntimeException> func;

    RuntimeErrors(Function<Pair<String, Optional<Throwable>>, RuntimeException> func) {
      this.func = func;
    }

    /**
     * Throw runtime exception with "reason".
     *
     * @param reason Message to include in exception
     */
    public void throwRuntime(String reason) {
      throwRuntime(reason, Optional.empty());
    }

    /**
     * Throw runtime exception with format "reason + cause message + cause Throwable"
     *
     * @param reason Message to include in exception
     * @param t Wrapped exception
     */
    public void throwRuntime(String reason, Throwable t) {
      throwRuntime(reason, Optional.of(t));
    }

    /**
     * Throw runtime exception with format "reason + cause message + cause Throwable".
     * If the optional Throwable is empty/null, the exception will only include "reason".
     *
     * @param reason Message to include in exception
     * @param t Optional wrapped exception
     */
    public void throwRuntime(String reason, Optional<Throwable> t) {
      throw func.apply(Pair.of(reason, t));
    }

    private static String formatReason(Pair<String, Optional<Throwable>> p) {
      return formatReason(p.getLeft(), p.getRight());
    }

    private static String formatReason(String reason, Optional<Throwable> t) {
      if (t.isPresent()) {
        return format("%s - reason:%s", reason, t.get());
      } else {
        return format("%s", reason);
      }
    }
  }

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


	public static String generateThreadDump() {
		final StringBuilder dump = new StringBuilder();
		final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
		for (ThreadInfo threadInfo : threadInfos) {
			dump.append('"');
			dump.append(threadInfo.getThreadName());
			dump.append("\" ");
			final Thread.State state = threadInfo.getThreadState();
			dump.append("\n   java.lang.Thread.State: ");
			dump.append(state);
			final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
			for (final StackTraceElement stackTraceElement : stackTraceElements) {
				dump.append("\n        at ");
				dump.append(stackTraceElement);
			}
			dump.append("\n\n");
		}
		return dump.toString();
	}
}
