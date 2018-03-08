/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.performance.load.monitor;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractMonitor implements Supplier<Long>, MonitorNaming {
  private static final double EPSILON = 1e-6;
  protected Optional<?> kafkaTopic;
  protected long timestampPrevious = 0;
  public AbstractMonitor(Optional<?> kafkaTopic) {
    this.kafkaTopic = kafkaTopic;
  }

  protected abstract Long monitor(double deltaTs);

  @Override
  public Long get() {
    long timeStarted = System.currentTimeMillis();
    Long ret = null;
    if(timestampPrevious > 0) {
      double deltaTs = (timeStarted - timestampPrevious) / 1000.0;
      if (Math.abs(deltaTs) > EPSILON) {
        ret = monitor(deltaTs);
      }
    }
    timestampPrevious = timeStarted;
    return ret;
  }

  public abstract String format();

}
