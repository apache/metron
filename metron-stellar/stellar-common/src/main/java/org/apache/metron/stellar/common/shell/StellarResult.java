/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.stellar.common.shell;

import java.util.Optional;

/**
 * The result of executing a Stellar expression within a StellarShellExecutor.
 */
public class StellarResult {

  /**
   * Indicates that a Stellar expression resulted in either
   * success or an error.
   */
  enum Status {
    SUCCESS,
    ERROR,
    TERMINATE
  }

  /**
   * Indicates either success or failure of executing the expression.
   */
  private Status status;

  /**
   * The result of executing the expression.  Only valid when execution is successful.
   */
  private Optional<Object> value;

  /**
   * The error that occurred when executing the expression.  Only valid when execution results in an error.
   */
  private Optional<Throwable> exception;

  /**
   * Indicates if the value is null;
   *
   * A null is a valid result, but cannot be unwrapped from an Optional.  Because of this
   * a boolean is used to indicate if the result is a success and the value is null.
   */
  private boolean isValueNull;

  /**
   * Private constructor to construct a result indicate success. Use the static methods; success.
   *
   * @param status Indicates success or failure.
   * @param value The value of executing the expression.
   */
  private StellarResult(Status status, Object value) {
    this.status = status;
    this.value = Optional.ofNullable(value);
    this.exception = Optional.empty();
    this.isValueNull = (value == null) && (status == Status.SUCCESS);
  }

  /**
   * Private constructor to construct a result indicating an error occurred. Use the static method; error.
   *
   * @param status Indicates success or failure.
   * @param exception The exception that occurred when executing the expression.
   */
  private StellarResult(Status status, Throwable exception) {
    this.status = status;
    this.value = Optional.empty();
    this.exception = Optional.of(exception);
    this.isValueNull = false;
  }

  /**
   * Create a result indicating the execution of an expression was successful.
   *
   * @param value The result of executing the expression.
   * @return A Result indicating success.
   */
  public static StellarResult success(Object value) {
    return new StellarResult(Status.SUCCESS, value);
  }

  /**
   * Create a result indicating that the execution of an expression was not successful.
   *
   * @param exception The exception that occurred while executing the expression.
   * @return A Result indicating that an error occurred.
   */
  public static StellarResult error(Throwable exception) {
    return new StellarResult(Status.ERROR, exception);
  }

  /**
   * Create a result indicating that the execution of an expression was not successful.
   *
   * @param errorMessage An error message.
   * @return A Result indicating that an error occurred.
   */
  public static StellarResult error(String errorMessage) {
    return new StellarResult(Status.ERROR, new IllegalArgumentException(errorMessage));
  }

  /**
   * Indicates an empty result; one that is successful yet has no result.  For example,
   * executing a comment.
   *
   * @return An empty result.
   */
  public static StellarResult noop() {
    return new StellarResult(Status.SUCCESS, "");
  }

  /**
   * Indicates that the user would like to terminate the session.
   *
   * @return A result indicating that the session should be terminated.
   */
  public static StellarResult terminate() {
    return new StellarResult(Status.TERMINATE, "");
  }

  /**
   * @return True, if the result indicates success.  Otherwise, false.
   */
  public boolean isSuccess() {
    return status == Status.SUCCESS;
  }

  /**
   * @return True, if the result indicates an error.  Otherwise, false.
   */
  public boolean isError() {
    return status == Status.ERROR;
  }

  /**
   * @return True, if status indicates terminate was requested.  Otherwise, false.
   */
  public boolean isTerminate() {
    return status == Status.TERMINATE;
  }

  /**
   * @return True, if the value is null.  Otherwise, false.
   */
  public boolean isValueNull() {
    return isValueNull;
  }

  /**
   * @return The status which indicates success or failure.
   */
  public Status getStatus() {
    return status;
  }

  /**
   * @return An optional value that only applies when status is success.
   */
  public Optional<Object> getValue() {
    return value;
  }

  /**
   * @return An optional exception that only applies when status is error.
   */
  public Optional<Throwable> getException() {
    return exception;
  }

  @Override
  public String toString() {
    return "StellarResult{" +
            "status=" + status +
            ", value=" + value +
            ", exception=" + exception +
            ", isValueNull=" + isValueNull +
            '}';
  }
}
