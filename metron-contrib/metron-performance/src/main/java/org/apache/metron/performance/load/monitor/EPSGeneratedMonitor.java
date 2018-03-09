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
import java.util.concurrent.atomic.AtomicLong;

public class EPSGeneratedMonitor extends AbstractMonitor {
  private AtomicLong numSent;
  private long numSentPrevious = 0;
  public EPSGeneratedMonitor(Optional<?> kafkaTopic, AtomicLong numSent) {
    super(kafkaTopic);
    this.numSent = numSent;
  }

  @Override
  protected Long monitor(double deltaTs) {
    if(kafkaTopic.isPresent()) {
      long totalProcessed = numSent.get();
      long written = (totalProcessed - numSentPrevious);
      long epsWritten = (long) (written / deltaTs);
      numSentPrevious = totalProcessed;
      return epsWritten;
    }
    return null;
  }

  @Override
  public String format() {
    return "%d eps generated to " + kafkaTopic.get();
  }

  @Override
  public String name() {
    return "generated";
  }

}
