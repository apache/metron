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

package org.apache.metron.common.utils;

import java.io.File;
import java.io.IOException;

public interface CompressionStrategy {

  /**
   * Compress infile.
   *
   * @param inFile file to compress
   * @param outFile destination path for compressed output
   * @throws IOException Any IO error
   */
  void compress(File inFile, File outFile) throws IOException;

  /**
   * Decompress infile.
   *
   * @param inFile file to decompress
   * @param outFile destination path for decompressed output
   * @throws IOException Any IO error
   */
  void decompress(File inFile, File outFile) throws IOException;

  /**
   * Test if file is proper gzip format. True if valid, false otherwise.
   *
   * @param gzipFile file to check for gzip compression
   * @return true if file is a gzip format, false otherwise.
   */
  boolean test(File gzipFile);

}
