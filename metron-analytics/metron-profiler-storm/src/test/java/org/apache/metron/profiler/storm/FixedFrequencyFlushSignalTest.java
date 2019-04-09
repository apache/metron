/*
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

package org.apache.metron.profiler.storm;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@code FixedFrequencyFlushSignal} class.
 */
public class FixedFrequencyFlushSignalTest {

  @Test
  public void testSignalFlush() {
    int flushFreq = 1000;
    FixedFrequencyFlushSignal signal = new FixedFrequencyFlushSignal(flushFreq);

    // not ready to flush; we have not seen any messages yet
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 5000 + 1000 = 6000; flush if anything >= flushTime
    signal.update(5000);
    assertFalse(signal.isTimeToFlush());

    // ready to flush; flushTime = min + flushFreq = 5000 + 1000 = 6000; max(5000,7000) >= 6000
    signal.update(7000);
    assertTrue(signal.isTimeToFlush());
  }

  @Test
  public void testOutOfOrderTimestamps() {
    int flushFreq = 5000;
    FixedFrequencyFlushSignal signal = new FixedFrequencyFlushSignal(flushFreq);

    // not ready to flush; flushTime = min + flushFreq = 5000 + 5000 = 10000; flush if anything >= flushTime
    signal.update(5000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 1000 + 5000 = 6000
    signal.update(1000);
    assertFalse(signal.isTimeToFlush());

    // ready to flush; flushTime = min + flushFreq = 1000 + 5000 = 6000; max(5000,1000,7000) >= 6000
    signal.update(7000);
    assertTrue(signal.isTimeToFlush());

    // ready to flush, still
    signal.update(3000);
    assertTrue(signal.isTimeToFlush());

    // ready to flush, still
    assertTrue(signal.isTimeToFlush());
  }

  @Test
  public void testOutOfOrderTimestampsNoFlush() {
    int flushFreq = 7000;
    FixedFrequencyFlushSignal signal = new FixedFrequencyFlushSignal(flushFreq);

    // not ready to flush; flushTime = min + flushFreq = 5000 + 7000 = 12000; flush if anything >= flushTime
    signal.update(5000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 1000 + 7000 = 8000
    signal.update(1000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 1000 + 7000 = 8000
    signal.update(7000);
    assertFalse(signal.isTimeToFlush());

    // ready to flush; flushTime = min + flushFreq = 1000 + 7000 = 8000; max(5000,1000,7000,3000) >= 8000
    signal.update(3000);
    assertFalse(signal.isTimeToFlush());
  }
  
  @Test
  public void testTimestampsDescending() {
    int flushFreq = 3000;
    FixedFrequencyFlushSignal signal = new FixedFrequencyFlushSignal(flushFreq);

    // not ready to flush; flushTime = min + flushFreq = 4100 + 3000 = 7100; flush if anything >= flushTime
    signal.update(4100);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 3000 + 3000 = 6000
    signal.update(3000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 2000 + 3000 = 5000
    signal.update(2000);
    assertFalse(signal.isTimeToFlush());

    // ready to flush; flushTime = min + flushFreq = 1000 + 3000 = 4000; max(4100,3000,2000,1000) >= 4000
    signal.update(1000);
    assertTrue(signal.isTimeToFlush());
  }

  @Test
  public void testTimestampsDescendingNoFlush() {
    int flushFreq = 4000;
    FixedFrequencyFlushSignal signal = new FixedFrequencyFlushSignal(flushFreq);

    // not ready to flush; flushTime = min + flushFreq = 4000 + 4000 = 8000; flush if anything >= flushTime
    signal.update(4000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 3000 + 4000 = 7000
    signal.update(3000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 2000 + 4000 = 6000
    signal.update(2000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 1000 + 4000 = 5000
    signal.update(1000);
    assertFalse(signal.isTimeToFlush());
  }

  @Test
  public void testTimestampsAscending() {
    int flushFreq = 3000;
    FixedFrequencyFlushSignal signal = new FixedFrequencyFlushSignal(flushFreq);

    // not ready to flush; flushTime = min + flushFreq = 1000 + 3000 = 4000; flush if anything >= flushTime
    signal.update(1000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 1000 + 3000 = 4000
    signal.update(2000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 1000 + 3000 = 4000
    signal.update(3000);
    assertFalse(signal.isTimeToFlush());

    // ready to flush; flushTime = min + flushFreq = 1000 + 3000 = 4000; max(1000,2000,3000,4000) >= 4000
    signal.update(4000);
  }

  @Test
  public void testTimestampsAscendingNoFlush() {
    int flushFreq = 4000;
    FixedFrequencyFlushSignal signal = new FixedFrequencyFlushSignal(flushFreq);

    // not ready to flush; flushTime = min + flushFreq = 1000 + 4000 = 5000; flush if anything >= flushTime
    signal.update(1000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 1000 + 4000 = 5000
    signal.update(2000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 1000 + 4000 = 5000
    signal.update(3000);
    assertFalse(signal.isTimeToFlush());

    // not ready to flush; flushTime = min + flushFreq = 1000 + 4000 = 5000
    signal.update(4000);
    assertFalse(signal.isTimeToFlush());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeFrequency() {
    // a negative flush frequency makes no sense
    new FixedFrequencyFlushSignal(-1000);
  }

  @Test
  public void testReset() {
    FixedFrequencyFlushSignal signal = new FixedFrequencyFlushSignal(4000);
    signal.update(1000);
    signal.update(6000);

    // ready to flush; flushTime = min + flushFreq = 1000 + 4000 = 5000; max(1000,6000) >= 5000
    assertTrue(signal.isTimeToFlush());

    // reset should turn off the flush signal
    signal.reset();
    assertFalse(signal.isTimeToFlush());
  }
}
