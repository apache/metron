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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.metron.pcap.config.PcapOptions;
import org.apache.metron.pcap.writer.PcapResultsWriter;

/**
 * Write to local FS.
 */
public class PcapCliFinalizer extends PcapFinalizer {

  /**
   * Format will have the format &lt;output-path&gt;/pcap-data-&lt;filename-prefix&gt;+&lt;partition-num&gt;.pcap
   * The filename prefix is pluggable, but in most cases it will be provided via the PcapConfig
   * as a formatted timestamp + uuid. A final sample format will look as follows:
   * /base/output/path/pcap-data-201807181911-09855b4ae3204dee8b63760d65198da3+0001.pcap
   */
  private static final String PCAP_CLI_FILENAME_FORMAT = "%s/pcap-data-%s+%04d.pcap";

  @Override
  protected void write(PcapResultsWriter resultsWriter, Configuration hadoopConfig,
      List<byte[]> data, Path outputPath) throws IOException {
    resultsWriter.writeLocal(data, outputPath.toString());
  }

  @Override
  protected Path getOutputPath(Map<String, Object> config, int partition) {
    Path finalOutputPath = PcapOptions.FINAL_OUTPUT_PATH.get(config, PcapOptions.STRING_TO_PATH, Path.class);
    String prefix = PcapOptions.FINAL_FILENAME_PREFIX.get(config, String.class);
    return new Path(String.format(PCAP_CLI_FILENAME_FORMAT, finalOutputPath, prefix, partition));
  }

}
