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
package org.apache.metron.dataloads.nonbulk.flatfile;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.enrichment.converter.HbaseConverter;

import java.io.IOException;

public class ExtractorState {
  private HTableInterface table;
  private Extractor extractor;
  private HbaseConverter converter;
  private FileSystem fs;

  public ExtractorState(HTableInterface table, Extractor extractor, HbaseConverter converter, Configuration config) {
    this.table = table;
    this.extractor = extractor;
    this.converter = converter;
    try {
      this.fs = FileSystem.get(config);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to retrieve hadoop file system: " + e.getMessage(), e);
    }
  }

  public HTableInterface getTable() {
    return table;
  }

  public Extractor getExtractor() {
    return extractor;
  }

  public HbaseConverter getConverter() {
    return converter;
  }

  public FileSystem getFileSystem() {
    return fs;
  }
}
