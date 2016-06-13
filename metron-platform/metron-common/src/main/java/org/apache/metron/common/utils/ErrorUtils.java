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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Values;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.metron.common.Constants;
import org.json.simple.JSONObject;

public class ErrorUtils {

	@SuppressWarnings("unchecked")
	public static JSONObject generateErrorMessage(String message, Throwable t)
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
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		
		error_message.put("message", message);
		error_message.put(Constants.SENSOR_TYPE, "error");
		error_message.put("exception", exception);
		error_message.put("stack", stackTrace);
		
		return error_message;
	}

	public static void handleError(OutputCollector collector, Throwable t, String errorStream) {
		JSONObject error = ErrorUtils.generateErrorMessage(t.getMessage(), t);
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
