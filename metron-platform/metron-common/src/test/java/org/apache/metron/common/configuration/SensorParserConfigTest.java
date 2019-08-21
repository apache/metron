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
package org.apache.metron.common.configuration;

import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.metron.TestConstants;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

public class SensorParserConfigTest {

  @Test
  public void testSerDe() throws IOException {
    for(File parserConfig : new File(new File(TestConstants.PARSER_CONFIGS_PATH), "parsers").listFiles()) {
      SensorParserConfig config = null;
      try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(parserConfig), StandardCharsets.UTF_8))) {
        String parserStr = IOUtils.toString(br);
        config = SensorParserConfig.fromBytes(parserStr.getBytes(StandardCharsets.UTF_8));
      }
      SensorParserConfig config2 = SensorParserConfig.fromBytes(config.toJSON().getBytes(
          StandardCharsets.UTF_8));
      Assert.assertEquals(config2, config);
    }
  }
}
