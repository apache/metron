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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.integration.utils.TestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HDFSUtilsTest {

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void writes_file_to_local_fs() throws Exception {
    String outText = "small brown bike and casket lottery";
    String outFile = tempDir.getRoot().getAbsolutePath() + "/outfile.txt";
    Configuration config = new Configuration();
    config.set("fs.default.name", "file:///");
    HDFSUtils.write(config, outText.getBytes(StandardCharsets.UTF_8), outFile);
    String actual = TestUtils.read(new File(outFile));
    assertThat("Text should match", actual, equalTo(outText));
  }

  @Test
  public void writes_file_to_local_fs_with_scheme_defined_only_in_uri() throws Exception {
    String outText = "small brown bike and casket lottery";
    String outFile = tempDir.getRoot().getAbsolutePath() + "/outfile.txt";
    Configuration config = new Configuration();
    HDFSUtils.write(config, outText.getBytes(StandardCharsets.UTF_8), "file:///" + outFile);

    String actual = TestUtils.read(new File(outFile));
    assertThat("Text should match", actual, equalTo(outText));
  }

}
