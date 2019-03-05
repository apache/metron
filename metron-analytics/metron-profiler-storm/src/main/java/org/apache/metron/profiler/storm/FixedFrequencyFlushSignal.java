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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Signals a flush on a fixed frequency; every X milliseconds.
 */
public class FixedFrequencyFlushSignal implements FlushSignal {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Tracks the min timestamp.
   */
  private long minTime;

  /**
   * Tracks the max timestamp.
   */
  private long maxTime;

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
    minTime = Long.MAX_VALUE;
    maxTime = Long.MIN_VALUE;
    LOG.debug("Flush counters reset");
  }

  /**
   * Update the internal state which tracks time.
   *
   * @param timestamp The timestamp received within a tuple.
   */
  @Override
  public void update(long timestamp) {
    if(LOG.isWarnEnabled()) {
      checkIfOutOfOrder(timestamp);
    }

    if(timestamp < minTime) {
      minTime = timestamp;
    }

    if(timestamp > maxTime) {
      maxTime = timestamp;
    }
  }

  /**
   * Checks if the timestamp is significantly out-of-order.
   *
   * @param timestamp The last timestamp.
   */
  private void checkIfOutOfOrder(long timestamp) {
    // do not warn if this is the first timestamp we've seen, which will always be 'out-of-order'
    if (maxTime > Long.MIN_VALUE) {

      long outOfOrderBy = maxTime - timestamp;
      if (Math.abs(outOfOrderBy) > flushFrequency) {
        LOG.warn("Timestamp out-of-order by {} ms. This may indicate a problem in the data. " +
                        "timestamp={}, maxKnown={}, flushFreq={} ms",
                outOfOrderBy, timestamp, maxTime, flushFrequency);
      }
    }
  }

  /**
   * Returns true, if it is time to flush.
   *
   * @return True if time to flush.  Otherwise, false.
   */
  @Override
  public boolean isTimeToFlush() {
    boolean flush = false;

    long flushTime = minTime + flushFrequency;
    if(maxTime >= flushTime) {
      flush = true;
    }

    LOG.debug("'{}' ms until flush; flush?={}, minTime={}, maxTime={}, flushTime={}",
            Math.max(0, flushTime - maxTime), flush, minTime, maxTime, flushTime);
    return flush;
  }

  @Override
  public long currentTimeMillis() {
    return maxTime;
  }
}
