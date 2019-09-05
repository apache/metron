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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.junit.Assert;
import org.junit.Test;

public class ParserWriterConfigurationTest {

  @Test
  public void testDefaultBatchSize() {
    ParserWriterConfiguration config = new ParserWriterConfiguration( new ParserConfigurations() );
    Assert.assertEquals(1, config.getBatchSize("foo"));
  }

  @Test
  public void testDefaultIndex() {
    ParserWriterConfiguration config = new ParserWriterConfiguration( new ParserConfigurations() );
    Assert.assertEquals("foo", config.getIndex("foo"));
  }

  /**
   * {
   *   "parserClassName":"some-parser",
   *   "sensorTopic":"testtopic",
   *   "parserConfig": {
   *     "batchSize" : 5,
   *     "batchTimeout" : "10000",
   *     "index" : "modified-index",
   *     "enabled" : "false"
   *   }
   * }
   */
  @Multiline
  private static String configJson;

  @Test
  public void pulls_writer_configuration_from_parserConfig() throws IOException {
    ParserConfigurations parserConfigurations = new ParserConfigurations();
    final String sensorName = "some-sensor";
    parserConfigurations.updateSensorParserConfig("some-sensor", configJson.getBytes(
        StandardCharsets.UTF_8));
    ParserWriterConfiguration writerConfiguration = new ParserWriterConfiguration(
        parserConfigurations);
    assertThat("batch size should match", writerConfiguration.getBatchSize(sensorName), equalTo(5));
    assertThat("batch timeout should match", writerConfiguration.getBatchTimeout(sensorName), equalTo(10000));
    assertThat("index should match", writerConfiguration.getIndex(sensorName), equalTo("modified-index"));
    assertThat("enabled should match", writerConfiguration.isEnabled(sensorName), equalTo(false));
  }

}
