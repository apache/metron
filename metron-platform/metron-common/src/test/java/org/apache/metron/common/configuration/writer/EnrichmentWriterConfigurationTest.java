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

package org.apache.metron.common.configuration.writer;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class EnrichmentWriterConfigurationTest {

  /**
   * {
   *  "enrichment.writer.batchSize" : 12345,
   *  "enrichment.writer.batchTimeout" : 555
   * }
   */
  @Multiline
  private static String globalJson;

  @Test
  public void gets_batch_size_and_timeout_from_global_config() throws IOException {
    EnrichmentConfigurations configs = new EnrichmentConfigurations();
    configs.updateGlobalConfig(globalJson.getBytes(StandardCharsets.UTF_8));
    EnrichmentWriterConfiguration writerConfig = new EnrichmentWriterConfiguration(configs);
    assertThat("batch timeout should match global config setting",
        writerConfig.getBatchTimeout(null), equalTo(555));
    assertThat("list should have single batch timeout matching global config setting",
        writerConfig.getAllConfiguredTimeouts(), equalTo(asList(555)));
    assertThat("batch size should match global config setting", writerConfig.getBatchSize(null),
        equalTo(12345));
  }

}
