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

package org.apache.metron.pcap.finalizer;

import java.util.Map;
import org.apache.hadoop.fs.Path;
import org.apache.metron.job.Finalizer;
import org.apache.metron.job.JobException;
import org.apache.metron.job.Pageable;

/**
 * PcapJob runs a MapReduce job that outputs Sequence Files to HDFS. This Strategy/Factory class
 * provides options for doing final processing on this raw MapReduce output for the CLI and REST
 * API's.
 */
public enum PcapFinalizerStrategies implements Finalizer<Path> {
  CLI(new PcapCliFinalizer()),
  REST(new PcapRestFinalizer());

  private Finalizer<Path> finalizer;

  PcapFinalizerStrategies(Finalizer<Path> finalizer) {
    this.finalizer = finalizer;
  }

  @Override
  public Pageable<Path> finalizeJob(Map<String, Object> config) throws JobException {
    return finalizer.finalizeJob(config);
  }

}
