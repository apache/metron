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

import static java.lang.String.format;

import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.metron.common.Constants;
import org.apache.metron.common.error.MetronError;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorUtils {
  private final static Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
     * Throw runtime exception with format "reason + cause message + cause Throwable".
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

  /**
   * Handles a {@link MetronError} that occurs.
   *
   * @param collector The Storm output collector being reported to
   * @param error The error that occurred
   */
  public static void handleError(OutputCollector collector, MetronError error)
  {
    collector.emit(Constants.ERROR_STREAM, new Values(error.getJSONObject()));
    Optional<Throwable> throwable = error.getThrowable();
    if (throwable.isPresent()) {
      collector.reportError(throwable.get());
    }

  }

  /**
   * Generates a string version of a thread dump.
   *
   * @return String of the thread dump
   */
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
