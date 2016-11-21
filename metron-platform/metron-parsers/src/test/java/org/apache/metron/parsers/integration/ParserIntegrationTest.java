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
package org.apache.metron.parsers.integration;

import junit.framework.Assert;
import org.apache.metron.TestConstants;
import org.apache.metron.common.Constants;
import org.apache.metron.enrichment.integration.components.ConfigUploadComponent;
import org.apache.metron.integration.*;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.parsers.integration.components.ParserTopologyComponent;
import org.apache.metron.test.TestDataType;
import org.apache.metron.test.utils.SampleDataUtils;
import org.junit.Test;

import java.util.*;

public abstract class ParserIntegrationTest extends BaseIntegrationTest {

  @Test
  public void test() throws Exception {
    final String sensorType = getSensorType();
    final List<byte[]> inputMessages = TestUtils.readSampleData(SampleDataUtils.getSampleDataPath(sensorType, TestDataType.RAW));

    final Properties topologyProperties = new Properties();
    final KafkaComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaComponent.Topic>() {{
      add(new KafkaComponent.Topic(sensorType, 1));
      add(new KafkaComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
    }});
    topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());

    ZKServerComponent zkServerComponent = getZKServerComponent(topologyProperties);

    ConfigUploadComponent configUploadComponent = new ConfigUploadComponent()
            .withTopologyProperties(topologyProperties)
            .withGlobalConfigsPath(TestConstants.SAMPLE_CONFIG_PATH)
            .withParserConfigsPath(TestConstants.PARSER_CONFIGS_PATH);

    ParserTopologyComponent parserTopologyComponent = new ParserTopologyComponent.Builder()
            .withSensorType(sensorType)
            .withTopologyProperties(topologyProperties)
            .withBrokerUrl(kafkaComponent.getBrokerList()).build();

    //UnitTestHelper.verboseLogging();
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("zk", zkServerComponent)
            .withComponent("kafka", kafkaComponent)
            .withComponent("config", configUploadComponent)
            .withComponent("org/apache/storm", parserTopologyComponent)
            .withMillisecondsBetweenAttempts(5000)
            .withNumRetries(10)
            .withCustomShutdownOrder(new String[] {"org/apache/storm","config","kafka","zk"})
            .build();
    runner.start();
    try {
      kafkaComponent.writeMessages(sensorType, inputMessages);
      ProcessorResult<List<byte[]>> result =
              runner.process(new Processor<List<byte[]>>() {
                List<byte[]> messages = null;
                List<byte[]> errors = null;
                List<byte[]> invalids = null;

                public ReadinessState process(ComponentRunner runner) {
                  KafkaComponent kafkaComponent = runner.getComponent("kafka", KafkaComponent.class);
                  List<byte[]> outputMessages = kafkaComponent.readMessages(Constants.ENRICHMENT_TOPIC);
                  if (outputMessages.size() == inputMessages.size()) {
                    messages = outputMessages;
                    return ReadinessState.READY;
                  } else {
                    errors = kafkaComponent.readMessages(Constants.ERROR_STREAM);
                    invalids = kafkaComponent.readMessages(Constants.INVALID_STREAM);
                    if(errors.size() > 0 || invalids.size() > 0) {
                      messages = outputMessages;
                      return ReadinessState.READY;
                    }
                    return ReadinessState.NOT_READY;
                  }
                }

                public ProcessorResult<List<byte[]>> getResult() {
                  ProcessorResult.Builder<List<byte[]>> builder = new ProcessorResult.Builder();
                  return builder.withResult(messages).withProcessErrors(errors).withProcessInvalids(invalids).build();
                }
              });
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
          buffer.append("No validations configured for sensorType " + sensorType + ".  Dumping parsed messages").append("\n");
          dumpParsedMessages(outputMessages,buffer);
          Assert.fail(buffer.toString());
        } else {
          for (ParserValidation validation : validations) {
            System.out.println("Running " + validation.getName() + " on sensorType " + sensorType);
            validation.validate(sensorType, outputMessages);
          }
        }
      }
    } finally {
      runner.stop();
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
