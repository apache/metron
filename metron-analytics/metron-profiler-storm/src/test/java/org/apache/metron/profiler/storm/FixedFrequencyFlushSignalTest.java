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

    FixedFrequencyFlushSignal signal = new FixedFrequencyFlushSignal(1000);

    // not time to flush yet
    assertFalse(signal.isTimeToFlush());

    // advance time
    signal.update(5000);

    // not time to flush yet
    assertFalse(signal.isTimeToFlush());

    // advance time
    signal.update(7000);

    // time to flush
    assertTrue(signal.isTimeToFlush());
  }

  @Test
  public void testOutOfOrderTimestamps() {
    FixedFrequencyFlushSignal signal = new FixedFrequencyFlushSignal(1000);

    // advance time, out-of-order
    signal.update(5000);
    signal.update(1000);
    signal.update(7000);
    signal.update(3000);

    // need to flush @ 5000 + 1000 = 6000. if anything > 6000 (even out-of-order), then it should signal a flush
    assertTrue(signal.isTimeToFlush());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeFrequency() {
    new FixedFrequencyFlushSignal(-1000);
  }
}
