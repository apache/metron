/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.integration;

import com.google.common.base.Function;
import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.consumer.ConsumerIterator;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.producer.Producer;
import kafka.message.MessageAndMetadata;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.metron.Constants;
import org.apache.metron.integration.util.TestUtils;
import org.apache.metron.integration.util.UnitTestHelper;
import org.apache.metron.integration.util.integration.ComponentRunner;
import org.apache.metron.integration.util.integration.Processor;
import org.apache.metron.integration.util.integration.ReadinessState;
import org.apache.metron.integration.util.integration.components.ElasticSearchComponent;
import org.apache.metron.integration.util.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.util.integration.components.KafkaWithZKComponent;
import org.apache.metron.integration.util.integration.util.KafkaUtil;
import org.apache.metron.spout.pcap.HDFSWriterCallback;
import org.apache.metron.test.converters.HexStringConverter;
import org.apache.metron.utils.SourceConfigUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

public abstract class ParserIntegrationTest {

  public abstract String getFluxPath();
  public abstract String getSampleInputPath();
  public abstract String getSampleParsedPath();
  public abstract String getSourceType();
  public abstract String getSourceConfig();
  public abstract String getFluxTopicProperty();

  @Test
  public void test() throws Exception {

    final String kafkaTopic = "test";

    final List<byte[]> inputMessages = TestUtils.readSampleData(getSampleInputPath());

    final Properties topologyProperties = new Properties() {{
      setProperty(getFluxTopicProperty(), kafkaTopic);
    }};
    final KafkaWithZKComponent kafkaComponent = new KafkaWithZKComponent().withTopics(new ArrayList<KafkaWithZKComponent.Topic>() {{
      add(new KafkaWithZKComponent.Topic(kafkaTopic, 1));
    }})
            .withPostStartCallback(new Function<KafkaWithZKComponent, Void>() {
              @Nullable
              @Override
              public Void apply(@Nullable KafkaWithZKComponent kafkaWithZKComponent) {
                topologyProperties.setProperty("kafka.zk", kafkaWithZKComponent.getZookeeperConnect());
                try {
                  SourceConfigUtils.writeToZookeeper(getSourceType(), getSourceConfig().getBytes(), kafkaWithZKComponent.getZookeeperConnect());
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }
            });

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
            .withTimeBetweenAttempts(5000)
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
      Assert.assertEquals(sampleParsedMessage, outputMessage);
    }
    runner.stop();

  }
}
