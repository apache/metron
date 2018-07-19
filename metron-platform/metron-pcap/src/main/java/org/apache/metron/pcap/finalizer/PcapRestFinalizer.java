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

import org.apache.hadoop.fs.Path;
import org.apache.metron.job.Statusable;
import org.apache.metron.pcap.config.PcapOptions;

import java.util.Map;

/**
 * Write to HDFS.
 */
public class PcapRestFinalizer extends PcapFinalizer {

  private static final String PCAP_REST_FILEPATH_FORMAT = "%s/%s/%s/%s/page-%s.pcap";

  private String jobType = Statusable.JobType.MAP_REDUCE.name();

  @Override
  protected Path getOutputPath(Map<String, Object> config, int partition) {
    String finalOutputPath = PcapOptions.FINAL_OUTPUT_PATH.get(config, String.class);
    String user = PcapOptions.USERNAME.get(config, String.class);
    String jobId = PcapOptions.JOB_ID.get(config, String.class);
    return new Path(String.format(PCAP_REST_FILEPATH_FORMAT, finalOutputPath, user, jobType, jobId, partition));
  }

}
