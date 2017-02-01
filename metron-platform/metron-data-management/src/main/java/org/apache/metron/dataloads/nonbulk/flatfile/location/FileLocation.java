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
package org.apache.metron.dataloads.nonbulk.flatfile.location;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileLocation implements RawLocation {
  @Override
  public Optional<List<String>> list(String loc) {
    List<String> children = new ArrayList<>();
    for(File f : new File(loc).listFiles()) {
        children.add(f.getPath());
      }
    return Optional.of(children);
  }

  @Override
  public boolean exists(String loc) throws IOException {
    return new File(loc).exists();
  }

  @Override
  public boolean isDirectory(String loc) throws IOException {
    return new File(loc).isDirectory();
  }

  @Override
  public InputStream openInputStream(String loc) throws IOException {
    return new FileInputStream(loc);
  }

  @Override
  public boolean match(String loc) {
    return new File(loc).exists();
  }
}
