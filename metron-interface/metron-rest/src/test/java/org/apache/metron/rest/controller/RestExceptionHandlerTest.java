/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.metron.rest.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.apache.metron.rest.model.RestError;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RestExceptionHandlerTest {

  private RestExceptionHandler restExceptionHandler;
  private HttpServletRequest request;
  private Throwable ex;

  @Before
  public void setUp() throws Exception {
    restExceptionHandler = new RestExceptionHandler();
    request = mock(HttpServletRequest.class);
    ex = mock(Throwable.class);
  }

  @Test
  public void handleControllerExceptionShouldProperlyReturnRestError() throws Exception {
    when(request.getAttribute("javax.servlet.error.status_code")).thenReturn(401);
    when(ex.getMessage()).thenReturn("unauthorized");
    when(ex.getCause()).thenReturn(null);

    ResponseEntity responseEntity = restExceptionHandler.handleControllerException(request, ex);
    assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
    RestError actualRestError = (RestError) responseEntity.getBody();
    assertEquals("unauthorized", actualRestError.getMessage());
    assertEquals("unauthorized", actualRestError.getFullMessage());
    assertEquals(401, actualRestError.getResponseCode());
  }

  @Test
  public void handleControllerExceptionShouldDefaultTo500() throws Exception {
    when(request.getAttribute("javax.servlet.error.status_code")).thenReturn(null);
    when(ex.getMessage()).thenReturn("some error");
    when(ex.getCause()).thenReturn(null);

    ResponseEntity responseEntity = restExceptionHandler.handleControllerException(request, ex);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
  }

  @Test
  public void handleControllerExceptionShouldReturnRootCause() throws Exception {
    when(request.getAttribute("javax.servlet.error.status_code")).thenReturn(500);
    when(ex.getMessage()).thenReturn("some error");
    Throwable rootCause = mock(Throwable.class);
    when(rootCause.getCause()).thenReturn(null);
    when(rootCause.getMessage()).thenReturn("root cause");
    when(ex.getCause()).thenReturn(rootCause);

    ResponseEntity responseEntity = restExceptionHandler.handleControllerException(request, ex);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    RestError actualRestError = (RestError) responseEntity.getBody();
    assertEquals("some error", actualRestError.getMessage());
    assertEquals("root cause", actualRestError.getFullMessage());
    assertEquals(500, actualRestError.getResponseCode());
  }
}
