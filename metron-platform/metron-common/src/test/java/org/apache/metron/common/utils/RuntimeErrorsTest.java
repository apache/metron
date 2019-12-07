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

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuntimeErrorsTest {
  @Test
  public void illegal_arg_throws_exception_with_reason() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> RuntimeErrors.ILLEGAL_ARG.throwRuntime("illegal arg happened"));
    assertTrue(e.getMessage().contains("illegal arg happened"));
    assertThat(e.getCause(), nullValue(Throwable.class));
  }

  @Test
  public void illegal_arg_throws_exception_with_reason_and_cause() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                RuntimeErrors.ILLEGAL_ARG.throwRuntime(
                    "illegal arg happened", new IOException("bad io")));
    assertTrue(e.getMessage().contains("illegal arg happened"));
    assertThat(e.getCause(), instanceOf(IOException.class));
  }

  @Test
  public void illegal_state_throws_exception_with_reason() {
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> RuntimeErrors.ILLEGAL_STATE.throwRuntime("illegal state happened"));
    assertTrue(e.getMessage().contains("illegal state happened"));
    assertThat(e.getCause(), nullValue(Throwable.class));
  }

  @Test
  public void illegal_state_throws_exception_with_reason_and_cause() {
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () ->
                RuntimeErrors.ILLEGAL_STATE.throwRuntime(
                    "illegal state happened", new IOException("bad io")));
    assertTrue(e.getMessage().contains("illegal state happened"));
    assertThat(e.getCause(), instanceOf(IOException.class));
  }
}
