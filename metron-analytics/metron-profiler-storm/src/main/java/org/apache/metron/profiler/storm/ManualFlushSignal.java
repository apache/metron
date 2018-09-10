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

/**
 * Signals that a flush should occur.
 *
 * <p>The flush signal can be turned on or off like a switch as needed.  Most useful for testing.
 */
public class ManualFlushSignal implements FlushSignal {

  private boolean flushNow = false;

  public void setFlushNow(boolean flushNow) {
    this.flushNow = flushNow;
  }

  @Override
  public boolean isTimeToFlush() {
    return flushNow;
  }

  @Override
  public void update(long timestamp) {
    // nothing to do
  }

  @Override
  public void reset() {
    // nothing to do.
  }

  @Override
  public long currentTimeMillis() {
    // not needed
    return 0;
  }
}
