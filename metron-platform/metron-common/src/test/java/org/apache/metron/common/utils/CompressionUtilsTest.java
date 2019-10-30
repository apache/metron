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

import org.apache.metron.integration.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CompressionUtilsTest {

  private static final String SAMPLE_TEXT = "hello world";
  private File tempDir;
  private File textFile;

  @BeforeEach
  public void setup() throws IOException {
    tempDir = TestUtils.createTempDir(this.getClass().getName());
    textFile = new File(tempDir, "test-text-file.txt");
    TestUtils.write(textFile, SAMPLE_TEXT);
  }

  @Test
  public void compresses_Gzip() throws IOException {
    File gzipFile = new File(tempDir, "test-gz-compression-file.gz");
    CompressionStrategies.GZIP.compress(textFile, gzipFile);
    assertThat(CompressionStrategies.GZIP.test(gzipFile), equalTo(true));
  }

  @Test
  public void decompresses_Gzip() throws IOException {
    File gzipFile = new File(tempDir, "test-gz-decompress.gz");
    CompressionStrategies.GZIP.compress(textFile, gzipFile);
    assertThat("gzipped file should exist", gzipFile.exists(), equalTo(true));
    File unzippedText = new File(tempDir, "test-gz-decompressed.txt");
    CompressionStrategies.GZIP.decompress(gzipFile, unzippedText);
    assertThat("decompressed file should exist", unzippedText.exists(), equalTo(true));
    String actual = TestUtils.read(unzippedText);
    assertThat("decompressed text should match", actual, equalTo(SAMPLE_TEXT));
  }

}
