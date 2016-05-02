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
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaWithZKComponent;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class ParserIntegrationTest extends BaseIntegrationTest {

  public abstract String getFluxPath();
  public abstract String getSampleInputPath();
  public abstract String getSampleParsedPath();
  public abstract String getSensorType();
  public abstract String getFluxTopicProperty();

  @Test
  public void test() throws Exception {

    final String kafkaTopic = getSensorType();

    final List<byte[]> inputMessages = TestUtils.readSampleData(getSampleInputPath());

    final Properties topologyProperties = new Properties();
    final KafkaWithZKComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaWithZKComponent.Topic>() {{
      add(new KafkaWithZKComponent.Topic(kafkaTopic, 1));
    }});

    topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());
    FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(getFluxPath()))
            .withTopologyName("test")
            .withTopologyProperties(topologyProperties)
            .build();

    UnitTestHelper.verboseLogging();
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("kafka", kafkaComponent)
            .withComponent("storm", fluxComponent)
            .withMillisecondsBetweenAttempts(5000)
            .withNumRetries(10)
            .build();
    runner.start();
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(kafkaTopic, inputMessages);
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
    List<byte[]> sampleParsedMessages = TestUtils.readSampleData(getSampleParsedPath());
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

}
