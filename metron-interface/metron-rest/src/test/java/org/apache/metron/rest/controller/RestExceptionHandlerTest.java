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
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.metron.rest.model.RestError;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RestExceptionHandlerTest {

  private RestExceptionHandler restExceptionHandler;
  private HttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    restExceptionHandler = new RestExceptionHandler();
    request = mock(HttpServletRequest.class);
  }

  @Test
  public void handleControllerExceptionShouldProperlyReturnRestError() throws Exception {
    when(request.getAttribute("javax.servlet.error.status_code")).thenReturn(401);
    Throwable throwable = new RuntimeException("unauthorized");

    ResponseEntity responseEntity = restExceptionHandler.handleControllerException(request, throwable);
    assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
    RestError actualRestError = (RestError) responseEntity.getBody();
    assertEquals("unauthorized", actualRestError.getMessage());
    assertEquals("RuntimeException: unauthorized", actualRestError.getFullMessage());
    assertEquals(401, actualRestError.getResponseCode());
  }

  @Test
  public void handleControllerExceptionShouldDefaultTo500() throws Exception {
    when(request.getAttribute("javax.servlet.error.status_code")).thenReturn(null);
    Throwable throwable = new RuntimeException("some error");

    ResponseEntity responseEntity = restExceptionHandler.handleControllerException(request, throwable);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
  }

  @Test
  public void handleControllerExceptionShouldReturnRootCause() throws Exception {
    when(request.getAttribute("javax.servlet.error.status_code")).thenReturn(500);
    Throwable throwable = new RuntimeException("some error", new RuntimeException("some root cause"));

    ResponseEntity responseEntity = restExceptionHandler.handleControllerException(request, throwable);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    RestError actualRestError = (RestError) responseEntity.getBody();
    assertEquals("some error", actualRestError.getMessage());
    assertEquals("RuntimeException: some root cause", actualRestError.getFullMessage());
    assertEquals(500, actualRestError.getResponseCode());
  }
}
