/*
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
package org.apache.metron.parsers.integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.apache.metron.TestConstants;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.parsers.integration.validation.ParserDriver;
import org.apache.metron.test.TestDataType;
import org.apache.metron.test.utils.SampleDataUtils;
import org.junit.Assert;

public abstract class ParserIntegrationTest extends BaseIntegrationTest {
  // Contains the list of sensor types to be tested.
  // See StormParserIntegrationTest for an example of how to use this in a Parameterized JUnit test
  protected static List<String> sensorTypes = Arrays.asList(
          "asa",
          "bro",
          "jsonMap",
          "jsonMapQuery",
          "jsonMapWrappedQuery",
          "snort",
          "squid",
          "websphere",
          "yaf",
          "syslog3164",
          "syslog5424"
          );

  protected List<byte[]> inputMessages;

  protected String readGlobalConfig() throws IOException {
    File configsRoot = new File("../" + TestConstants.SAMPLE_CONFIG_PATH);
    return new String(Files.readAllBytes(new File(configsRoot, "global.json").toPath()));
  }

  protected String readSensorConfig(String sensorType) throws IOException {
    // First check the basic parsers dir.
    File configsRoot = new File("../" + TestConstants.PARSER_COMMON_CONFIGS_PATH);
    File parsersRoot = new File(configsRoot, "parsers");
    System.out.println("Workspace: " + System.getProperty("user.dir"));
    System.out.println("Parsers root: " + parsersRoot);
    if (!Files.exists(new File(parsersRoot, sensorType + ".json").toPath())) {
      // Use the main parsers configs
      configsRoot = new File("../" + TestConstants.PARSER_CONFIGS_PATH);
      parsersRoot = new File(configsRoot, "parsers");
    }
    return new String(Files.readAllBytes(new File(parsersRoot, sensorType + ".json").toPath()));
  }

  public void runTest(ParserDriver driver) throws Exception {
    String sensorType = driver.getSensorType();
    inputMessages = TestUtils.readSampleData(SampleDataUtils.getSampleDataPath("..", sensorType, TestDataType.RAW));

    ProcessorResult<List<byte[]>> result = driver.run(inputMessages);
    List<byte[]> outputMessages = result.getResult();
    StringBuffer buffer = new StringBuffer();
    if (result.failed()){
      result.getBadResults(buffer);
      buffer.append(String.format("%d Valid Messages Processed", outputMessages.size())).append("\n");
      dumpParsedMessages(outputMessages,buffer);
      Assert.fail(buffer.toString());
    } else {
      List<ParserValidation> validations = getValidations();
      if (validations == null || validations.isEmpty()) {
        buffer.append("No validations configured for sensorType ").append(sensorType).append(".  Dumping parsed messages").append("\n");
        dumpParsedMessages(outputMessages,buffer);
        Assert.fail(buffer.toString());
      } else {
        for (ParserValidation validation : validations) {
          System.out.println("Running " + validation.getName() + " on sensorType " + sensorType);
          validation.validate(sensorType, outputMessages);
        }
      }
    }
  }

  public void dumpParsedMessages(List<byte[]> outputMessages, StringBuffer buffer) {
    for (byte[] outputMessage : outputMessages) {
      buffer.append(new String(outputMessage)).append("\n");
    }
  }

  abstract List<ParserValidation> getValidations();
}
