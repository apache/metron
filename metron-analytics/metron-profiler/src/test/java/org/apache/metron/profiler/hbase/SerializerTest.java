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

package org.apache.metron.profiler.hbase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the Serializer.
 */
public class SerializerTest {

  @Test
  public void testInteger() {
    final int expected = 2;
    byte[] raw = Serializer.toBytes(expected);
    int actual = Serializer.fromBytes(raw, Integer.class);
    assertEquals(expected, actual);
  }

  @Test
  public void testDouble() {
    final double expected = 2.0;
    byte[] raw = Serializer.toBytes(expected);
    double actual = Serializer.fromBytes(raw, Double.class);
    assertEquals(expected, actual, 0.01);
  }

  @Test
  public void testShort() {
    final short expected = 2;
    byte[] raw = Serializer.toBytes(expected);
    short actual = Serializer.fromBytes(raw, Short.class);
    assertEquals(expected, actual);
  }

  @Test
  public void testLong() {
    final long expected = 2L;
    byte[] raw = Serializer.toBytes(expected);
    long actual = Serializer.fromBytes(raw, Long.class);
    assertEquals(expected, actual);
  }

  @Test
  public void testFloat() {
    final Float expected = 2.2F;
    byte[] raw = Serializer.toBytes(expected);
    float actual = Serializer.fromBytes(raw, Float.class);
    assertEquals(expected, actual, 0.01);
  }
}
