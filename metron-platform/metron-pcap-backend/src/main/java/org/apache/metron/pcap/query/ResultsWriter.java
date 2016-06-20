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
package org.apache.metron.pcap.query;

import org.apache.metron.pcap.PcapMerger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ResultsWriter {

  public void write(List<byte[]> pcaps, String outPath) throws IOException {
    File out = new File(outPath);
    try (FileOutputStream fos = new FileOutputStream(out)) {
      fos.write(mergePcaps(pcaps));
    }
  }

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
