/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.stellar.common.timing;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;

public class StackWatchTest {

  @Test
  public void testRootNameEmptyConstructor() {
    StackWatch watch = new StackWatch();
    Assert.assertEquals(watch.DEFAULT_ROOT_NAME, watch.getRootName());
  }

  @Test
  public void testRootName() {
    StackWatch watch = new StackWatch(null);
    Assert.assertEquals(watch.DEFAULT_ROOT_NAME, watch.getRootName());
  }

  @Test
  public void start() {
    final StopWatch stopWatch = new StopWatch();
    StackWatch watch = new StackWatch("testStackWatch");
    watch.start();
    stopWatch.start();
    stopWatch.stop();
    watch.stop();
    watch.visit(new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        Assert.assertTrue(node.getStopWatch().getNanoTime() > stopWatch.getNanoTime());
      }
    });
  }

  @Test(expected = IllegalStateException.class)
  public void testStartASecondTimeThrowsException() {
    StackWatch watch = new StackWatch("testStackWatch");
    watch.start();
    watch.stop();
    watch.start();
  }

  @Test
  public void startTiming() {
    final StopWatch stopWatch = new StopWatch();
    StackWatch watch = new StackWatch("testStackWatch");
    watch.start();
    watch.startTiming("one");
    stopWatch.start();
    stopWatch.stop();
    watch.stopTiming();
    watch.stop();
    watch.visit(new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        if (level > 0) {
          Assert.assertTrue(node.getStopWatch().getNanoTime() > stopWatch.getNanoTime());
        }
      }
    });
  }

  @Test
  public void stopTiming() throws Exception {
    StackWatch watch = new StackWatch("testStackWatch");
    watch.start();
    watch.startTiming("one");
    Thread.sleep(100);
    watch.stopTiming();
    watch.stop();
    final ArrayList<Long> times = new ArrayList<>();
    watch.visit(new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        if (level > 0) {
          times.add(node.getStopWatch().getNanoTime());
        }
      }
    });

    watch.visit(new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        if (level > 0) {
          times.add(node.getStopWatch().getNanoTime());
        }
      }
    });

    Assert.assertEquals(times.size(), 2);
    Assert.assertEquals(times.get(0), times.get(1));
  }

  @Test(expected = IllegalStateException.class)
  public void stopWithoutStopTimingThrowsException() {
    StackWatch watch = new StackWatch("testStackWatch");
    watch.start();
    watch.startTiming("one");
    watch.stop();
  }

  @Test(expected = IllegalStateException.class)
  public void clearBeforeStopThrowsException() {
    StackWatch watch = new StackWatch("testStackWatch");
    watch.start();
    watch.startTiming("one");
    watch.stopTiming();
    watch.clear();
    watch.stop();
  }

  @Test
  public void testStackWatch() throws Exception {
    // General test, call three top level functions, the first of two having
    // nested calls
    StackWatch watch = new StackWatch("testStackWatch");
    watch.start();
    // root timing
    watch.startTiming("Test");
    functionOne(watch);
    functionTwo(watch);
    functionThree(watch);
    watch.stopTiming();
    watch.stop();
    final ArrayList<Integer> levels = new ArrayList<>();
    watch.visit(new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        levels.add(level);
      }
    });
    // validate that we have the right number of 'timings'
    assertEquals(levels.size(), 9);
  }

  @Test
  public void testStackWatchWithoutStarting() throws Exception {
    // General test, call three top level functions, the first of two having
    // nested calls
    StackWatch watch = new StackWatch("testStackWatch");
    // root timing
    watch.startTiming("Test");
    functionOne(watch);
    functionTwo(watch);
    functionThree(watch);
    watch.stopTiming();
    watch.stop();
    final ArrayList<Integer> levels = new ArrayList<>();
    watch.visit(new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        levels.add(level);
      }
    });
    // validate that we have the right number of 'timings'
    assertEquals(levels.size(), 9);
  }

  @Test
  public void testStackWatchFiltered() throws Exception {
    StackWatch watch = new StackWatch("testStackWatch");
    final String[] filter = new String[]{"ThreeFunc"};
    watch.startTiming("Test");
    functionOne(watch);
    functionTwo(watch);
    functionThree(watch);
    watch.stopTiming();
    watch.stop();
    final ArrayList<Integer> levels = new ArrayList<>();
    watch.visit(new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        String[] tags = node.getTags();
        if (Arrays.asList(tags).containsAll(Arrays.asList(filter))) {
          levels.add(level);
        }
      }
    });

    // validate that we have the right number of 'timings'
    // there is only one ThreeFunc
    assertEquals(levels.size(), 1);
  }

  @Test
  public void testNonStartOuter() throws Exception {
    // Test a case where we are doing timings, but don't give it an 'outer' time
    StackWatch watch = new StackWatch("testStackWatch");
    functionOne(watch);
    functionTwo(watch);
    functionThree(watch);
    watch.stop();

    final ArrayList<Integer> levels = new ArrayList<>();
    watch.visit(new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        levels.add(level);
      }
    });
    assertEquals(levels.size(), 8);
  }

  @Test(expected = IllegalStateException.class)
  public void testMissMatchedStopThrowsException() throws Exception {
    StackWatch watch = new StackWatch("testStackWatch");
    functionOne(watch);
    functionTwo(watch);
    functionThree(watch);
    // we are stopping when we didn't explicitly start, so this will stop the root
    // and empty the queue
    watch.stopTiming();
    // exception
    watch.stop();
  }

  @Test
  public void testDidNotStopAll() throws Exception {
    StackWatch watch = new StackWatch("testStackWatch");
    watch.startTiming("Test");
    functionOne(watch);
    functionTwo(watch);
    functionThree(watch);
    functionNoStop(watch);
    watch.stopTiming();
    final ArrayList<Integer> levels = new ArrayList<>();
    watch.visit(new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        levels.add(level);
      }
    });

    assertEquals(levels.size(), 10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullNameThrowsException() {
    StackWatch watch = new StackWatch("testStackWatch");
    watch.startTiming(null);
  }

  @Test(expected = IllegalStateException.class)
  public void testParentNotRunningThrowsException() throws Exception {
    StackWatch watch = new StackWatch("testStackWatch");
    watch.startTiming("test");
    functionOneCloseParent(watch);
  }

  @Test(expected = IllegalStateException.class)
  public void testStartingSecondSetOfTimingsThrowsException() throws Exception {
    StackWatch watch = new StackWatch("testStackWatch");
    watch.startTiming("Test");
    functionOne(watch);
    watch.stopTiming();
    watch.stop();
    watch.startTiming("More Test");
  }

  private void functionOne(StackWatch watch) throws Exception {
    watch.startTiming("One", "OneFunc");
    Thread.sleep(50);
    functionOneOne(watch);
    watch.stopTiming();
  }

  private void functionOneCloseParent(StackWatch watch) throws Exception {
    watch.startTiming("One", "OneFunc");
    Thread.sleep(50);
    watch.visit(new TimingRecordNodeVisitor() {
      @Override
      public void visitRecord(int level, TimingRecordNode node) {
        node.getStopWatch().stop();
      }
    });
    functionOneOne(watch);
  }

  private void functionOneOne(StackWatch watch) throws Exception {
    watch.startTiming("OneOne", "OneFunc");
    Thread.sleep(50);
    functionOneTwo(watch);
    watch.stopTiming();

  }

  private void functionOneTwo(StackWatch watch) throws Exception {
    watch.startTiming("OneTwo", "OneFunc");
    Thread.sleep(50);
    watch.stopTiming();
  }

  private void functionTwo(StackWatch watch) throws Exception {
    watch.startTiming("Two", "TwoFunc");
    Thread.sleep(50);
    functionTwoOne(watch);
    watch.stopTiming();
  }

  private void functionTwoOne(StackWatch watch) throws Exception {
    watch.startTiming("TwoOne", "TwoFunc");
    Thread.sleep(50);
    functionTwoTwo(watch);
    watch.stopTiming();
  }

  private void functionTwoTwo(StackWatch watch) throws Exception {
    watch.startTiming("TwoTwo", "TwoFunc");
    Thread.sleep(50);
    watch.stopTiming();
  }

  private void functionThree(StackWatch watch) throws Exception {
    watch.startTiming("Three", "ThreeFunc");
    Thread.sleep(50);
    watch.stopTiming();
  }

  private void functionNoStop(StackWatch watch) throws Exception {
    watch.startTiming("NoStop");
    Thread.sleep(50);
  }
}

