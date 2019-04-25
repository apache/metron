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
package org.apache.metron.storm.common.utils;

import org.apache.metron.common.Constants;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.test.error.MetronErrorJSONMatcher;
import org.apache.storm.task.OutputCollector;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ErrorUtilsTest {

  @Test
  public void handleErrorShouldEmitAndReportError() throws Exception {
    Throwable e = new Exception("error");
    MetronError error = new MetronError().withMessage("error message").withThrowable(e);
    OutputCollector collector = mock(OutputCollector.class);

    StormErrorUtils.handleError(collector, error);
    verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM), argThat(new MetronErrorJSONMatcher(error.getJSONObject())));
    verify(collector, times(1)).reportError(any());
  }
}
