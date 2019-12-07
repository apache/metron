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
package org.apache.metron.writers.integration;

import java.io.IOException;
import org.apache.metron.parsers.integration.EnvelopedParserIntegrationTest;
import org.apache.metron.parsers.integration.validation.ParserDriver;
import org.apache.metron.parsers.integration.validation.StormParserDriver;
import org.junit.jupiter.api.Test;

public class StormEnvelopedParserIntegrationTest extends EnvelopedParserIntegrationTest {

  @Test
  public void testEnvelopedData() throws IOException {
    ParserDriver driver = new StormParserDriver("test", parserConfig_default, "{}");
    super.testEnvelopedData(driver);
  }

  @Test
  public void testEnvelopedData_withMetadataPrefix() throws IOException {
    ParserDriver driver = new StormParserDriver("test", parserConfig_withPrefix, "{}");
    super.testEnvelopedData_withMetadataPrefix(driver);
  }

  @Test
  public void testEnvelopedData_noMergeMetadata() throws IOException {
    ParserDriver driver = new StormParserDriver("test", parserConfig_nomerge, "{}");
    super.testEnvelopedData_noMergeMetadata(driver);
  }

  @Test
  public void testCiscoPixEnvelopingCisco302020() throws Exception {
      ParserDriver syslogDriver = new StormParserDriver("ciscoPix", ciscoPixSyslogConfig, "{}");
      ParserDriver driver = new StormParserDriver("cisco302020", cisco302020Config, "{}");
      super.testCiscoPixEnvelopingCisco302020(syslogDriver, driver);
  }
}
