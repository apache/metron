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

import com.google.common.base.Function;
import junit.framework.Assert;
import org.apache.metron.TestConstants;
import org.apache.metron.common.Constants;
import org.apache.metron.enrichment.integration.components.ConfigUploadComponent;
import org.apache.metron.integration.*;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.processors.KafkaMessageSet;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.integration.processors.KafkaProcessor;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.parsers.integration.components.ParserTopologyComponent;
import org.apache.metron.test.TestDataType;
import org.apache.metron.test.utils.SampleDataUtils;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.*;

public abstract class ParserIntegrationTest extends BaseIntegrationTest {
  protected List<byte[]> inputMessages;
  @Test
  public void test() throws Exception {
    final String sensorType = getSensorType();
    inputMessages = TestUtils.readSampleData(SampleDataUtils.getSampleDataPath(sensorType, TestDataType.RAW));

    final Properties topologyProperties = new Properties();
    final KafkaComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaComponent.Topic>() {{
      add(new KafkaComponent.Topic(sensorType, 1));
      add(new KafkaComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
      add(new KafkaComponent.Topic(Constants.INVALID_STREAM,1));
      add(new KafkaComponent.Topic(Constants.ERROR_STREAM,1));
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
      ProcessorResult<List<byte[]>> result = runner.process(getProcessor());
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

  @SuppressWarnings("unchecked")
  private KafkaProcessor<List<byte[]>> getProcessor(){

    return new KafkaProcessor<>()
            .withKafkaComponentName("kafka")
            .withReadTopic(Constants.ENRICHMENT_TOPIC)
            .withErrorTopic(Constants.ERROR_STREAM)
            .withInvalidTopic(Constants.INVALID_STREAM)
            .withValidateReadMessages(new Function<KafkaMessageSet, Boolean>() {
              @Nullable
              @Override
              public Boolean apply(@Nullable KafkaMessageSet messageSet) {
                return (messageSet.getMessages().size() + messageSet.getErrors().size() + messageSet.getInvalids().size()) == inputMessages.size();
              }
            })
            .withProvideResult(new Function<KafkaMessageSet,List<byte[]>>(){
              @Nullable
              @Override
              public List<byte[]> apply(@Nullable KafkaMessageSet messageSet) {
                  return messageSet.getMessages();
              }
            });
  }
  abstract String getSensorType();
  abstract List<ParserValidation> getValidations();

}
