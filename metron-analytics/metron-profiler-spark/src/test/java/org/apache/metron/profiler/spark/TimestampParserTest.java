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

package org.apache.metron.profiler.spark;

import org.junit.Before;
import org.junit.Test;

import java.time.format.DateTimeParseException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TimestampParserTest {

  private TimestampParser parser;

  @Before
  public void setup() {
    parser = new TimestampParser();
  }

  @Test
  public void testEmpty() {
    Optional<Long> millis = parser.parse("");
    assertFalse(millis.isPresent());
  }

  @Test
  public void testBlank() {
    Optional<Long> millis = parser.parse("      ");
    assertFalse(millis.isPresent());
  }

  @Test
  public void testIsoInstantFormat() {
    // ISO-8601 instant format
    Optional<Long> millis = parser.parse("2011-12-03T10:15:30Z");
    assertTrue(millis.isPresent());
    assertEquals(1322907330000L, millis.get().longValue());
  }

  @Test(expected = DateTimeParseException.class)
  public void testInvalidFormat() {
    parser.parse("1537502400000");
    fail("Expected exception");
  }
}
