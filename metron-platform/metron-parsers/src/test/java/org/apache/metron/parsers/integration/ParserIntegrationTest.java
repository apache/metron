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

import org.apache.metron.common.Constants;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.parsers.integration.components.ParserTopologyComponent;
import org.apache.metron.test.TestDataType;
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.KafkaWithZKComponent;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ParserIntegrationTest extends BaseIntegrationTest {

  private String configRoot = "../metron-parsers/src/main/config/zookeeper/parsers";
  private String sampleDataRoot = "../metron-integration-test/src/main/resources/sample/data";
  private String testSensorType;

  @Test
  public void test() throws Exception {

    for (String name: new File(configRoot).list()) {
      final String sensorType = name;
      if (testSensorType != null && !testSensorType.equals(sensorType)) continue;

      final List<byte[]> inputMessages = TestUtils.readSampleData(getSampleDataPath(sensorType, TestDataType.RAW));

      final Properties topologyProperties = new Properties();
      final KafkaWithZKComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaWithZKComponent.Topic>() {{
        add(new KafkaWithZKComponent.Topic(sensorType, 1));
      }});

      topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());

      ParserTopologyComponent parserTopologyComponent = new ParserTopologyComponent.Builder()
              .withSensorType(sensorType)
              .withZookeeperUrl(kafkaComponent.getZookeeperConnect())
              .withBrokerUrl(kafkaComponent.getBrokerList()).build();

      UnitTestHelper.verboseLogging();
      ComponentRunner runner = new ComponentRunner.Builder()
              .withComponent("kafka", kafkaComponent)
              .withComponent("storm", parserTopologyComponent)
              .withMillisecondsBetweenAttempts(5000)
              .withNumRetries(10)
              .build();
      runner.start();
      kafkaComponent.writeMessages(sensorType, inputMessages);
      List<byte[]> outputMessages =
              runner.process(new Processor<List<byte[]>>() {
                List<byte[]> messages = null;

                public ReadinessState process(ComponentRunner runner) {
                  KafkaWithZKComponent kafkaWithZKComponent = runner.getComponent("kafka", KafkaWithZKComponent.class);
                  List<byte[]> outputMessages = kafkaWithZKComponent.readMessages(Constants.ENRICHMENT_TOPIC);
                  if (outputMessages.size() == inputMessages.size()) {
                    messages = outputMessages;
                    return ReadinessState.READY;
                  } else {
                    return ReadinessState.NOT_READY;
                  }
                }

                public List<byte[]> getResult() {
                  return messages;
                }
              });
      List<byte[]> sampleParsedMessages = TestUtils.readSampleData(getSampleDataPath(sensorType, TestDataType.PARSED));
      Assert.assertEquals(sampleParsedMessages.size(), outputMessages.size());
      for (int i = 0; i < outputMessages.size(); i++) {
        String sampleParsedMessage = new String(sampleParsedMessages.get(i));
        String outputMessage = new String(outputMessages.get(i));
        try {
          assertJSONEqual(sampleParsedMessage, outputMessage);
        } catch (Throwable t) {
          System.out.println("expected: " + sampleParsedMessage);
          System.out.println("actual: " + outputMessage);
          throw t;
        }
      }
      runner.stop();
    }
  }

  public static void assertJSONEqual(String doc1, String doc2) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map m1 = mapper.readValue(doc1, Map.class);
    Map m2 = mapper.readValue(doc2, Map.class);
    for(Object k : m1.keySet()) {
      Object v1 = m1.get(k);
      Object v2 = m2.get(k);

      if(v2 == null) {
        Assert.fail("Unable to find key: " + k + " in output");
      }
      if(k.equals("timestamp")) {
        //TODO: Take the ?!?@ timestamps out of the reference file.
        Assert.assertEquals(v1.toString().length(), v2.toString().length());
      }
      else if(!v2.equals(v1)) {
        Assert.assertEquals("value mismatch for " + k ,v1, v2);
      }
    }
    Assert.assertEquals(m1.size(), m2.size());
  }

  private String getSampleDataPath(String sensorType, TestDataType testDataType) throws FileNotFoundException {
    File sensorSampleDataPath = new File(sampleDataRoot, sensorType);
    if (sensorSampleDataPath.exists() && sensorSampleDataPath.isDirectory()) {
      File sampleDataPath = new File(sensorSampleDataPath, testDataType.getDirectoryName());
      if (sampleDataPath.exists() && sampleDataPath.isDirectory()) {
        File[] children = sampleDataPath.listFiles();
        if (children != null && children.length > 0) {
          return children[0].getAbsolutePath();
        }
      }
    }
    throw new FileNotFoundException("Could not find data in " + sampleDataRoot + "/" + sensorType + "/" + testDataType.getDirectoryName());
  }

  public static void main(String[] args) throws Exception {
    ParserIntegrationTest parserIntegrationTest = new ParserIntegrationTest();
    if (args.length > 0) {
      parserIntegrationTest.testSensorType = args[0];
    }
    parserIntegrationTest.test();
  }

}
