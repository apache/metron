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

package org.apache.metron.common.performance;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

public class PerformanceLoggerTest {

  private int thresholdPercent = 10;
  private Supplier<Map<String, Object>> configSupplier;
  @Mock
  private Timing timing;
  @Mock
  private ThresholdCalculator thresholdCalc;
  @Mock
  private Logger logger;
  private PerformanceLogger perfLogger;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    configSupplier = () -> ImmutableMap.of("performance.logging.percent.records", thresholdPercent);
    when(thresholdCalc.isPast(thresholdPercent)).thenReturn(false).thenReturn(false)
        .thenReturn(true);
    when(logger.isDebugEnabled()).thenReturn(true);
    when(timing.exists("t1")).thenReturn(true);
    perfLogger = new PerformanceLogger(configSupplier, logger, thresholdCalc, timing);
  }

  @Test
  public void logs_on_threshold() {
    when(timing.getElapsed("t1")).thenReturn(111L).thenReturn(222L).thenReturn(333L);
    perfLogger.mark("t1");
    perfLogger.log("t1");
    perfLogger.log("t1");
    perfLogger.log("t1");
    verify(timing).mark("t1");
    verify(logger, times(1)).debug(anyString(), any(), eq(111L), eq(""));
  }

  @Test
  public void logs_on_threshold_with_message() {
    when(timing.getElapsed("t1")).thenReturn(111L).thenReturn(222L).thenReturn(333L);
    perfLogger.mark("t1");
    perfLogger.log("t1", "my message");
    perfLogger.log("t1", "my message");
    perfLogger.log("t1", "my message");
    verify(timing).mark("t1");
    verify(logger, times(1)).debug(anyString(), any(), eq(111L), eq("my message"));
  }

  @Test
  public void warns_when_logging_nonexisting_marks() {
    when(thresholdCalc.isPast(thresholdPercent)).thenReturn(true);
    when(timing.getElapsed("t1")).thenReturn(111L);
    when(timing.getElapsed("t2")).thenReturn(222L);
    when(timing.getElapsed("t3")).thenReturn(333L);
    when(timing.exists("t1")).thenReturn(true);
    when(timing.exists("t2")).thenReturn(false);
    when(timing.exists("t3")).thenReturn(false);
    perfLogger.mark("t1");
    perfLogger.log("t1", "my message");
    perfLogger.log("t2", "my message");
    perfLogger.log("t3", "my message");
    verify(timing).mark("t1");
    verify(timing, never()).mark("t2");
    verify(timing, never()).mark("t3");
    verify(logger).debug(anyString(), any(), eq(111L), eq("my message"));
    verify(logger)
        .debug(anyString(), eq("WARNING - MARK NOT SET"), eq(222L), eq("my message"));
    verify(logger)
        .debug(anyString(), eq("WARNING - MARK NOT SET"), eq(333L), eq("my message"));
  }

  @Test
  public void logs_with_multiple_markers() {
    when(thresholdCalc.isPast(thresholdPercent)).thenReturn(true);
    when(timing.getElapsed("t1")).thenReturn(111L);
    when(timing.getElapsed("t2")).thenReturn(222L);
    perfLogger.mark("t1");
    perfLogger.mark("t2");
    perfLogger.log("t2", "my message 2");
    perfLogger.log("t1", "my message 1");
    verify(timing).mark("t1");
    verify(timing).mark("t2");
    verify(logger).debug(anyString(), any(), eq(111L), eq("my message 1"));
    verify(logger).debug(anyString(), any(), eq(222L), eq("my message 2"));
  }

  @Test
  public void defaults_to_1_percent_threshold() {
    configSupplier = () -> new HashMap<>();
    when(thresholdCalc.isPast(1)).thenReturn(false).thenReturn(false)
        .thenReturn(true);
    when(timing.getElapsed("t1")).thenReturn(111L).thenReturn(222L).thenReturn(333L);
    perfLogger.mark("t1");
    perfLogger.log("t1", "my message");
    perfLogger.log("t1", "my message");
    perfLogger.log("t1", "my message");
    verify(timing).mark("t1");
    verify(logger, times(1)).debug(anyString(), any(), eq(111L), eq("my message"));
  }

  @Test
  public void does_not_log_when_debugging_disabled() {
    when(logger.isDebugEnabled()).thenReturn(false);
    when(timing.getElapsed("t1")).thenReturn(111L).thenReturn(222L).thenReturn(333L);
    perfLogger.mark("t1");
    perfLogger.log("t1", "my message");
    perfLogger.log("t1", "my message");
    perfLogger.log("t1", "my message");
    verify(timing).mark("t1");
    verify(logger, times(0)).debug(anyString(), any(), eq(111L), eq("my message"));
  }

  @Test
  public void logs_formatted_message_provided_format_args() {
    when(thresholdCalc.isPast(thresholdPercent)).thenReturn(true);
    when(timing.getElapsed("t1")).thenReturn(111L).thenReturn(222L).thenReturn(333L)
        .thenReturn(444L);
    perfLogger.mark("t1");
    perfLogger.log("t1", "my {} message", "1");
    perfLogger.log("t1", "my {} message {}", "1", "2");
    perfLogger.log("t1", "my {} message {} {}", "1", "2", "3");
    perfLogger.log("t1", "my {} message {} {} {}", "1", "2", "3", "4");
    verify(timing).mark("t1");
    verify(logger, times(1)).debug(anyString(), any(), eq(111L), eq("my 1 message"));
    verify(logger, times(1)).debug(anyString(), any(), eq(222L), eq("my 1 message 2"));
    verify(logger, times(1)).debug(anyString(), any(), eq(333L), eq("my 1 message 2 3"));
    verify(logger, times(1)).debug(anyString(), any(), eq(444L), eq("my 1 message 2 3 4"));
  }

  @Test
  public void isDebugEnabled_returns_true_when_debugging_is_set() {
    when(logger.isDebugEnabled()).thenReturn(true);
    assertThat(perfLogger.isDebugEnabled(), equalTo(true));
  }

}
