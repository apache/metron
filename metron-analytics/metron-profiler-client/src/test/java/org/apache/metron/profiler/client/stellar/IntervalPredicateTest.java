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
package org.apache.metron.profiler.client.stellar;

import org.apache.commons.lang3.Range;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntervalPredicateTest {
  @Test
  public void testBasicTest() {
    List<Range<Long>> intervals = new ArrayList<Range<Long>>() {{
      add(Range.between(0L, 10L));
      add(Range.between(20L, 30L));
      add(Range.between(40L, 50L));
    }};
    IntervalPredicate<Long> predicate = new IntervalPredicate.Identity(intervals);
    assertTrue(predicate.test(0L));
    assertTrue(predicate.test(10L));
    assertTrue(predicate.test(5L));
    assertFalse(predicate.test(51L));
    assertFalse(predicate.test(15L));
  }

  @Test
  public void testWithOverlap() {
    List<Range<Long>> intervals = new ArrayList<Range<Long>>() {{
      add(Range.between(0L, 10L));
      add(Range.between(5L, 30L));
      add(Range.between(40L, 50L));
    }};
    IntervalPredicate<Long> predicate = new IntervalPredicate.Identity(intervals);
    assertTrue(predicate.test(0L));
    assertTrue(predicate.test(5L));
    assertTrue(predicate.test(30L));
    assertTrue(predicate.test(10L));
    assertFalse(predicate.test(51L));
    assertTrue(predicate.test(15L));
    assertFalse(predicate.test(31L));
    assertTrue(predicate.test(45L));
  }

  @Test
  public void testTrivialCase() {
    List<Range<Long>> intervals = new ArrayList<Range<Long>>() {{
      add(Range.between(0L, 10L));
    }};
    IntervalPredicate<Long> predicate = new IntervalPredicate.Identity(intervals);
    assertTrue(predicate.test(0L));
    assertTrue(predicate.test(5L));
    assertTrue(predicate.test(10L));
    assertFalse(predicate.test(51L));
    assertFalse(predicate.test(15L));
  }

  @Test
  public void testDegenerateCase() {
    List<Range<Long>> intervals = new ArrayList<Range<Long>>() {{
      add(Range.between(10L, 10L));
    }};
    IntervalPredicate<Long> predicate = new IntervalPredicate.Identity(intervals);
    assertFalse(predicate.test(0L));
    assertFalse(predicate.test(5L));
    assertTrue(predicate.test(10L));
    assertFalse(predicate.test(11L));
  }
}
