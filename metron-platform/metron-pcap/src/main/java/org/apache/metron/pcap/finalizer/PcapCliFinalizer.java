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

/**
 * Write to local FS.
 */
public class PcapCliFinalizer extends PcapFinalizer {

  @Override
  protected String getOutputFileName(Map<String, Object> config, int partition) {
    Path finalOutputPath = (Path) PcapFinalizerOptions.FINAL_OUTPUT_PATH.get(config);
    String prefix = (String) config.get("finalFilenamePrefix");
    return String.format("%s/pcap-data-%s+%04d.pcap", finalOutputPath, prefix, partition);
  }

}
