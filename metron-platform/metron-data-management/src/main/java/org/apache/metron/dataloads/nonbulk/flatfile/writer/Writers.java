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

import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.util.Optional;

public enum Writers implements Writer {
  LOCAL(new LocalWriter()),
  HDFS(new HDFSWriter()),
  CONSOLE(new ConsoleWriter())
  ;
  private Writer writer;

  Writers(Writer writer) {
    this.writer = writer;
  }
  public static Optional<Writers> getStrategy(String strategyName) {
    if(strategyName == null) {
      return Optional.empty();
    }
    for(Writers strategy : values()) {
      if(strategy.name().equalsIgnoreCase(strategyName.trim())) {
        return Optional.of(strategy);
      }
    }
    return Optional.empty();
  }

  @Override
  public void validate(Optional<String> output, Configuration hadoopConf) throws InvalidWriterOutput {
    writer.validate(output, hadoopConf);
  }

  @Override
  public void write(byte[] obj, Optional<String> output, Configuration hadoopConf) throws IOException {
    writer.write(obj, output, hadoopConf);
  }
}
