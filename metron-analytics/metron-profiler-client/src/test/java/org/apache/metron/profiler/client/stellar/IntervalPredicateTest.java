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

import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IntervalPredicateTest {
  @Test
  public void testBasicTest() {
    List<Interval> intervals = new ArrayList<Interval>() {{
      add(new Interval(0, 10));
      add(new Interval(20, 30));
      add(new Interval(40, 50));
    }};
    IntervalPredicate predicate = new IntervalPredicate.Identity(intervals);
    Assert.assertTrue(predicate.test(0L));
    Assert.assertTrue(predicate.test(10L));
    Assert.assertTrue(predicate.test(5L));
    Assert.assertFalse(predicate.test(51L));
    Assert.assertFalse(predicate.test(15L));
  }

  @Test
  public void testWithOverlap() {
    List<Interval> intervals = new ArrayList<Interval>() {{
      add(new Interval(0, 10));
      add(new Interval(5, 30));
      add(new Interval(40, 50));
    }};
    IntervalPredicate predicate = new IntervalPredicate.Identity(intervals);
    Assert.assertTrue(predicate.test(0L));
    Assert.assertTrue(predicate.test(5L));
    Assert.assertTrue(predicate.test(30L));
    Assert.assertTrue(predicate.test(10L));
    Assert.assertFalse(predicate.test(51L));
    Assert.assertTrue(predicate.test(15L));
    Assert.assertFalse(predicate.test(31L));
    Assert.assertTrue(predicate.test(45L));
  }

  @Test
  public void testTrivialCase() {
    List<Interval> intervals = new ArrayList<Interval>() {{
      add(new Interval(0, 10));
    }};
    IntervalPredicate predicate = new IntervalPredicate.Identity(intervals);
    Assert.assertTrue(predicate.test(0L));
    Assert.assertTrue(predicate.test(5L));
    Assert.assertTrue(predicate.test(10L));
    Assert.assertFalse(predicate.test(51L));
    Assert.assertFalse(predicate.test(15L));
  }

  @Test
  public void testDegenerateCase() {
    List<Interval> intervals = new ArrayList<Interval>() {{
      add(new Interval(10, 10));
    }};
    IntervalPredicate predicate = new IntervalPredicate.Identity(intervals);
    Assert.assertFalse(predicate.test(0L));
    Assert.assertFalse(predicate.test(5L));
    Assert.assertTrue(predicate.test(10L));
    Assert.assertFalse(predicate.test(11L));
  }
}
