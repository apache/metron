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
package org.apache.metron.pcap.writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.common.utils.HDFSUtils;
import org.apache.metron.pcap.PcapMerger;

public class PcapResultsWriter {

  /**
   * Write out pcaps. Configuration offers ability to configure for HDFS or local FS, if desired.
   *
   * @param config Standard hadoop filesystem config.
   * @param pcaps pcap data to write. Pre-merged format as a list of pcaps as byte arrays.
   * @param outPath where to write the pcap data to.
   * @throws IOException I/O issue encountered.
   */
  public void write(Configuration config, List<byte[]> pcaps, String outPath) throws IOException {
    HDFSUtils.write(config, mergePcaps(pcaps), outPath);
  }

  /**
   * Creates a pcap file with proper global header from individual pcaps.
   *
   * @param pcaps pcap records to merge into a pcap file with header.
   * @return merged result.
   * @throws IOException I/O issue encountered.
   */
  public byte[] mergePcaps(List<byte[]> pcaps) throws IOException {
    if (pcaps == null) {
      return new byte[]{};
    }
    if (pcaps.size() == 1) {
      return pcaps.get(0);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PcapMerger.merge(baos, pcaps);
    return baos.toByteArray();
  }
}
