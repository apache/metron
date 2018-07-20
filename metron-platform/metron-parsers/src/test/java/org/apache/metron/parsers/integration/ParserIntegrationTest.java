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

import org.apache.metron.TestConstants;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.test.TestDataType;
import org.apache.metron.test.utils.SampleDataUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public abstract class ParserIntegrationTest extends BaseIntegrationTest {
  protected List<byte[]> inputMessages;

  protected String readGlobalConfig() throws IOException {
    File configsRoot = new File(TestConstants.SAMPLE_CONFIG_PATH);
    return new String(Files.readAllBytes(new File(configsRoot, "global.json").toPath()));
  }

  protected String readSensorConfig(String sensorType) throws IOException {
    File configsRoot = new File(TestConstants.PARSER_CONFIGS_PATH);
    File parsersRoot = new File(configsRoot, "parsers");
    return new String(Files.readAllBytes(new File(parsersRoot, sensorType + ".json").toPath()));
  }

  @Test
  public void test() throws Exception {
    String sensorType = getSensorType();
    ParserDriver driver = new ParserDriver(sensorType, readSensorConfig(sensorType), readGlobalConfig());
    inputMessages = TestUtils.readSampleData(SampleDataUtils.getSampleDataPath(sensorType, TestDataType.RAW));

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

  abstract String getSensorType();
  abstract List<ParserValidation> getValidations();

}
