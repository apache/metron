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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;

public class ErrorUtilsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void illegal_arg_throws_exception_with_reason() throws Exception {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("illegal arg happened");
    exception.expectCause(nullValue(Throwable.class));
    ErrorUtils.RuntimeErrors.ILLEGAL_ARG.throwRuntime("illegal arg happened");
  }

  @Test
  public void illegal_arg_throws_exception_with_reason_and_cause() throws Exception {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("illegal arg happened");
    exception.expectCause(instanceOf(IOException.class));
    ErrorUtils.RuntimeErrors.ILLEGAL_ARG.throwRuntime("illegal arg happened", new IOException("bad io"));
  }

  @Test
  public void illegal_state_throws_exception_with_reason() throws Exception {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("illegal state happened");
    exception.expectCause(nullValue(Throwable.class));
    ErrorUtils.RuntimeErrors.ILLEGAL_STATE.throwRuntime("illegal state happened");
  }

  @Test
  public void illegal_state_throws_exception_with_reason_and_cause() throws Exception {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("illegal state happened");
    exception.expectCause(instanceOf(IOException.class));
    ErrorUtils.RuntimeErrors.ILLEGAL_STATE.throwRuntime("illegal state happened", new IOException("bad io"));
  }
}
