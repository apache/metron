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

package org.apache.metron.profiler.bolt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Signals a flush on a fixed frequency; every X milliseconds.
 */
public class FixedFrequencyFlushSignal implements FlushSignal {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The latest known timestamp.
   */
  private long currentTime;

  /**
   * The time when the next flush should occur.
   */
  private long flushTime;

  /**
   * The amount of time between flushes in milliseconds.
   */
  private long flushFrequency;

  public FixedFrequencyFlushSignal(long flushFrequencyMillis) {

    if(flushFrequencyMillis < 0) {
      throw new IllegalArgumentException("flush frequency must be >= 0");
    }

    this.flushFrequency = flushFrequencyMillis;
    reset();
  }

  /**
   * Resets the state used to keep track of time.
   */
  @Override
  public void reset() {
    flushTime = 0;
    currentTime = 0;

    LOG.debug("Flush counters reset");
  }

  /**
   * Update the internal state which tracks time.
   *
   * @param timestamp The timestamp received within a tuple.
   */
  @Override
  public void update(long timestamp) {

    if(timestamp > currentTime) {

      // need to update current time
      LOG.debug("Updating current time; last={}, new={}", currentTime, timestamp);
      currentTime = timestamp;

    } else if ((currentTime - timestamp) > flushFrequency) {

      // significantly out-of-order timestamps
      LOG.warn("Timestamps out-of-order by '{}' ms. This may indicate a problem in the data. last={}, current={}",
              (currentTime - timestamp),
              timestamp,
              currentTime);
    }

    if(flushTime == 0) {

      // set the next time to flush
      flushTime = currentTime + flushFrequency;
      LOG.debug("Setting flush time; flushTime={}, currentTime={}, flushFreq={}",
              flushTime,
              currentTime,
              flushFrequency);
    }
  }

  /**
   * Returns true, if it is time to flush.
   *
   * @return True if time to flush.  Otherwise, false.
   */
  @Override
  public boolean isTimeToFlush() {

    boolean flush = currentTime > flushTime;
    LOG.debug("Flush={}, '{}' ms until flush; currentTime={}, flushTime={}",
            flush,
            flush ? 0 : (flushTime-currentTime),
            currentTime,
            flushTime);

    return flush;
  }

  @Override
  public long currentTimeMillis() {
    return currentTime;
  }
}
