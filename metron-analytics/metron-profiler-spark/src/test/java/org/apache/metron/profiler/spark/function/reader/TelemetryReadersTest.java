/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.metron.profiler.spark.function.reader;

import org.apache.metron.profiler.spark.reader.ColumnEncodedTelemetryReader;
import org.apache.metron.profiler.spark.reader.TelemetryReaders;
import org.apache.metron.profiler.spark.reader.TextEncodedTelemetryReader;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.metron.profiler.spark.reader.TelemetryReaders.COLUMN_READER;
import static org.apache.metron.profiler.spark.reader.TelemetryReaders.TEXT_READER;

public class TelemetryReadersTest {

  @Test
  public void testTextReader() {
    Assert.assertTrue(TelemetryReaders.create(TEXT_READER.toString()) instanceof TextEncodedTelemetryReader);
  }

  @Test
  public void testColumnReader() {
    Assert.assertTrue(TelemetryReaders.create(COLUMN_READER.toString()) instanceof ColumnEncodedTelemetryReader);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidReader() {
    TelemetryReaders.create("invalid");
    Assert.fail("exception expected");
  }
}
