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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class StellarResultTest {

  /**
   * Tests the 'success' method which is used to retrieve a StellarShellResult
   * indicating success.
   */
  @Test
  public void testSuccess() {
    final int expected = 2;

    // retrieve a result that indicates success
    StellarResult result = StellarResult.success(expected);
    assertNotNull(result);

    // validate the value
    assertTrue(result.getValue().isPresent());
    assertEquals(expected, result.getValue().get());

    // validate the exception
    assertFalse(result.getException().isPresent());

    // validate status
    assertEquals(StellarResult.Status.SUCCESS, result.getStatus());
    assertTrue(result.isSuccess());
    assertFalse(result.isError());
    assertFalse(result.isTerminate());
  }

  /**
   * Tests the 'error' method which is used to retrieve a StellarShellResult
   * indicating that an error occurred.
   */
  @Test
  public void testError() {
    final String expected = "my error message";

    // retrieve a result that indicates success
    StellarResult result = StellarResult.error(expected);
    assertNotNull(result);

    // validate the value
    assertFalse(result.getValue().isPresent());

    // validate the exception
    assertTrue(result.getException().isPresent());
    assertEquals(expected, result.getException().get().getMessage());

    // validate status
    assertEquals(StellarResult.Status.ERROR, result.getStatus());
    assertFalse(result.isSuccess());
    assertTrue(result.isError());
    assertFalse(result.isTerminate());
  }

  /**
   * Tests the 'terminate' method which is used to retrieve a StellarShellResult
   * indicating that a termination request was made.
   */
  @Test
  public void testTerminate() {

    // retrieve a result that indicates success
    StellarResult result = StellarResult.terminate();
    assertNotNull(result);

    // validate the value
    assertTrue(result.getValue().isPresent());

    // validate the exception
    assertFalse(result.getException().isPresent());

    // validate status
    assertEquals(StellarResult.Status.TERMINATE, result.getStatus());
    assertFalse(result.isSuccess());
    assertFalse(result.isError());
    assertTrue(result.isTerminate());
  }

  /**
   * Tests the 'noop' method which is used to retrieve a StellarShellResult
   * indicating that no operation occurred, nor was required.
   */
  @Test
  public void testNoop() {

    // retrieve a result that indicates success
    StellarResult result = StellarResult.noop();
    assertNotNull(result);

    // validate the value
    assertTrue(result.getValue().isPresent());

    // validate the exception
    assertFalse(result.getException().isPresent());

    // validate status
    assertEquals(StellarResult.Status.SUCCESS, result.getStatus());
    assertTrue(result.isSuccess());
    assertFalse(result.isError());
    assertFalse(result.isTerminate());
  }

  /**
   * A success result where the value is null is perfectly acceptable.
   */
  @Test
  public void testSuccessWithNull() {
    final Object expected = null;

    // retrieve a result that indicates success
    StellarResult result = StellarResult.success(expected);
    assertNotNull(result);

    // validate the value
    assertTrue(result.isValueNull());

    // validate the exception
    assertFalse(result.getException().isPresent());

    // validate status
    assertEquals(StellarResult.Status.SUCCESS, result.getStatus());
    assertTrue(result.isSuccess());
    assertFalse(result.isError());
    assertFalse(result.isTerminate());
  }

  /**
   * Tests the behavior of isValueNull() with error, noop and terminate conditions.
   */
  @Test
  public void testNonSuccessWithNull() {
    assertFalse(StellarResult.error(new Exception()).isValueNull());
    assertFalse(StellarResult.error("error msg").isValueNull());
    assertFalse(StellarResult.noop().isValueNull());
    assertFalse(StellarResult.terminate().isValueNull());
  }
}
