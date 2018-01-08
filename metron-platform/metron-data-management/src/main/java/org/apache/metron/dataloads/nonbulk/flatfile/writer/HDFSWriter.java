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
package org.apache.metron.dataloads.nonbulk.flatfile.writer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.Optional;

public class HDFSWriter implements Writer {
  @Override
  public void validate(Optional<String> fileNameOptional, Configuration hadoopConfig) throws InvalidWriterOutput {
    if(!fileNameOptional.isPresent()) {
      throw new InvalidWriterOutput("Filename is not present.");
    }
    String fileName = fileNameOptional.get();
    if(StringUtils.isEmpty(fileName) || fileName.trim().equals(".") || fileName.trim().equals("..") || fileName.trim().endsWith("/")) {
      throw new InvalidWriterOutput("Filename is empty or otherwise invalid.");
    }
  }

  @Override
  public void write(byte[] obj, Optional<String> output, Configuration hadoopConfig) throws IOException {
    FileSystem fs = FileSystem.get(hadoopConfig);
    try(FSDataOutputStream stream = fs.create(new Path(output.get()))) {
      IOUtils.write(obj, stream);
      stream.flush();
    }
  }
}
