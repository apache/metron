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

package org.apache.metron.pcap;

import java.util.List;
import org.apache.hadoop.fs.Path;
import org.apache.metron.job.Pageable;

public class PcapFiles implements Pageable<Path> {

  private final List<Path> files;

  public PcapFiles(List<Path> files) {
    this.files = files;
  }

  @Override
  public Iterable<Path> asIterable() {
    return files;
  }

  @Override
  public Path getPage(int num) {
    return files.get(num);
  }

  @Override
  public int getSize() {
    return files.size();
  }
}
