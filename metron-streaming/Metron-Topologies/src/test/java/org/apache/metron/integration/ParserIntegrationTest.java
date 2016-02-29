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
import kafka.consumer.ConsumerIterator;
import kafka.javaapi.producer.Producer;
import kafka.message.MessageAndMetadata;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.metron.integration.util.UnitTestHelper;
import org.apache.metron.integration.util.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.util.integration.components.KafkaWithZKComponent;
import org.apache.metron.integration.util.integration.util.KafkaUtil;
import org.apache.metron.spout.pcap.HDFSWriterCallback;
import org.apache.metron.test.converters.HexStringConverter;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ParserIntegrationTest {

  private String topologiesDir = "src/main/resources/Metron_Configs/topologies";
  private String targetDir = "target";
  private String samplePath = "src/main/resources/SampleInput/YafExampleOutput";

  private static File getOutDir(String targetDir) {
    File outDir = new File(new File(targetDir), "pcap_ng");
    if (!outDir.exists()) {
      outDir.mkdirs();
      outDir.deleteOnExit();
    }
    return outDir;
  }
  private static void clearOutDir(File outDir) {
    for(File f : outDir.listFiles()) {
      f.delete();
    }
  }
  private static int numFiles(File outDir) {
    return outDir.listFiles().length;
  }

  private static List<byte[]> readMessages(File sampleDataFile) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(sampleDataFile));
    List<byte[]> ret = new ArrayList<>();
    for(String line = null;(line = br.readLine()) != null;) {
      long ts = System.currentTimeMillis();
      ret.add(line.getBytes());
    }
    return ret;
  }

  @Test
  public void test() throws Exception {
    UnitTestHelper.verboseLogging();
    if (!new File(topologiesDir).exists()) {
      topologiesDir = UnitTestHelper.findDir("topologies");
    }
    targetDir = UnitTestHelper.findDir("target");
    final String kafkaTopic = "yaf";
    final File outDir = getOutDir(targetDir);
    clearOutDir(outDir);
    Assert.assertEquals(0, numFiles(outDir));
    Assert.assertNotNull(topologiesDir);
    Assert.assertNotNull(targetDir);

    final Properties topologyProperties = new Properties() {{
      setProperty("spout.kafka.topic.yaf", kafkaTopic);
    }};
    final KafkaWithZKComponent kafkaComponent = new KafkaWithZKComponent().withTopics(new ArrayList<KafkaWithZKComponent.Topic>() {{
      add(new KafkaWithZKComponent.Topic(kafkaTopic, 1));
    }})
            .withPostStartCallback(new Function<KafkaWithZKComponent, Void>() {
              @Nullable
              @Override
              public Void apply(@Nullable KafkaWithZKComponent kafkaWithZKComponent) {
                topologyProperties.setProperty("kafka.zk", kafkaWithZKComponent.getZookeeperConnect());
                return null;
              }
            });

    kafkaComponent.writeMessages(kafkaTopic, readMessages(new File(samplePath)));

//    FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
//            .withTopologyLocation(new File(topologiesDir + "/pcap_ng/remote.yaml"))
//            .withTopologyName("pcap_ng")
//            .withTopologyProperties(topologyProperties)
//            .build();

    List<byte[]> consumedMessages = new ArrayList<>();
    ConsumerIterator consumerIterator = kafkaComponent.getStreamIterator(kafkaTopic);
    while(consumerIterator.hasNext()) {
      MessageAndMetadata messageAndMetadata = consumerIterator.next();
      consumedMessages.add(messageAndMetadata.message().toString().getBytes());
    }

  }
}
