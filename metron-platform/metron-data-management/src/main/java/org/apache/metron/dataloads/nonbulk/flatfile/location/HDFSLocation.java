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

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HDFSLocation implements RawLocation<FileSystem> {

  FileSystem fs = null;

  @Override
  public Optional<List<String>> list(String loc) throws IOException {
    List<String> children = new ArrayList<>();
    for(FileStatus f : fs.listStatus(new Path(loc)) ) {
        children.add(f.getPath().toString());
      }
    return Optional.of(children);
  }

  @Override
  public boolean exists(String loc) throws IOException {
    return fs.exists(new Path(loc));
  }

  @Override
  public boolean isDirectory(String loc) throws IOException {
    return fs.isDirectory(new Path(loc));
  }

  @Override
  public InputStream openInputStream(String loc) throws IOException {
    return fs.open(new Path(loc));
  }

  @Override
  public boolean match(String loc) {
    try {
      return loc.startsWith("hdfs://") && exists(loc);
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public void init(FileSystem state) {
    this.fs = state;
  }


}
