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

import com.google.common.collect.Iterables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.metron.Constants;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.integration.util.UnitTestHelper;
import org.apache.metron.integration.util.integration.ComponentRunner;
import org.apache.metron.integration.util.integration.Processor;
import org.apache.metron.integration.util.integration.ReadinessState;
import org.apache.metron.integration.util.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.util.integration.components.KafkaWithZKComponent;
import org.apache.metron.integration.util.mock.MockHTable;
import org.apache.metron.parsing.parsers.PcapParser;
import org.apache.metron.pcap.PcapUtils;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PcapParserIntegrationTest extends BaseIntegrationTest {

  private static String BASE_DIR = "pcap";
  private static String DATA_DIR = BASE_DIR + "/data_dir";
  private static String QUERY_DIR = BASE_DIR + "/query";
  private String topologiesDir = "src/main/resources/Metron_Configs/topologies";
  private String targetDir = "target";

  public static class Provider implements TableProvider, Serializable {
    MockHTable.Provider provider = new MockHTable.Provider();

    @Override
    public HTableInterface getTable(Configuration config, String tableName) throws IOException {
      return provider.getTable(config, tableName);
    }
  }

  private File getOutDir(String targetDir) {
    File outDir = new File(new File(targetDir), DATA_DIR);
    if (!outDir.exists()) {
      outDir.mkdirs();
    }

    return outDir;
  }

  private File getQueryDir(String targetDir) {
    File outDir = new File(new File(targetDir), QUERY_DIR);
    if (!outDir.exists()) {
      outDir.mkdirs();
    }
    return outDir;
  }

  private static void clearOutDir(File outDir) {
    for (File f : outDir.listFiles()) {
      f.delete();
    }
  }

  private static Map<String, byte[]> readPcaps(Path pcapFile) throws IOException {
    SequenceFile.Reader reader = new SequenceFile.Reader(new Configuration(),
            Reader.file(pcapFile)
    );
    Map<String, byte[]> ret = new HashMap<>();
    IntWritable key = new IntWritable();
    BytesWritable value = new BytesWritable();
    PcapParser parser = new PcapParser();
    parser.init();
    while (reader.next(key, value)) {
      int keyInt = key.get();
      byte[] valueBytes = value.copyBytes();
      JSONObject message = parser.parse(valueBytes).get(0);
      if (parser.validate(message)) {
        ret.put(PcapUtils.getSessionKey(message), valueBytes);
      }
    }
    return ret;
  }

  @Test
  public void testTopology() throws Exception {
    if (!new File(topologiesDir).exists()) {
      topologiesDir = UnitTestHelper.findDir("topologies");
    }
    targetDir = UnitTestHelper.findDir("target");
    final String kafkaTopic = "pcap";
    final String tableName = "pcap";
    final String columnFamily = "t";
    final String columnIdentifier = "value";
    final File outDir = getOutDir(targetDir);
    final File queryDir = getQueryDir(targetDir);
    clearOutDir(outDir);
    clearOutDir(queryDir);

    File baseDir = new File(new File(targetDir), BASE_DIR);
    Assert.assertNotNull(topologiesDir);
    Assert.assertNotNull(targetDir);
    Path pcapFile = new Path("../Metron-Testing/src/main/resources/sample/data/SampleInput/PCAPExampleOutput");
    final Map<String, byte[]> pcapEntries = readPcaps(pcapFile);
    Assert.assertTrue(Iterables.size(pcapEntries.keySet()) > 0);
    final Properties topologyProperties = new Properties() {{
      setProperty("hbase.provider.impl", "" + Provider.class.getName());
      setProperty("spout.kafka.topic.pcap", kafkaTopic);
      setProperty("bolt.hbase.table.name", tableName);
      setProperty("bolt.hbase.table.fields", columnFamily + ":" + columnIdentifier);
    }};
    final KafkaWithZKComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaWithZKComponent.Topic>() {{
      add(new KafkaWithZKComponent.Topic(kafkaTopic, 1));
      add(new KafkaWithZKComponent.Topic(Constants.ENRICHMENT_TOPIC, 1));
    }});

    FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(topologiesDir + "/pcap/test.yaml"))
            .withTopologyName("pcap")
            .withTopologyProperties(topologyProperties)
            .build();

    final MockHTable pcapTable = (MockHTable) MockHTable.Provider.addToCache(tableName, columnFamily);

    UnitTestHelper.verboseLogging();
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("kafka", kafkaComponent)
            .withComponent("storm", fluxComponent)
            .withMaxTimeMS(60000)
            .withMillisecondsBetweenAttempts(6000)
            .withNumRetries(10)
            .build();
    try {
      runner.start();
      fluxComponent.submitTopology();
      kafkaComponent.writeMessages(kafkaTopic, pcapEntries.values());
      System.out.println("Sent pcap data: " + pcapEntries.size());
      List<byte[]> messages = kafkaComponent.readMessages(kafkaTopic);
      Assert.assertEquals(pcapEntries.size(), messages.size());
      System.out.println("Wrote " + pcapEntries.size() + " to kafka");
      runner.process(new Processor<Void>() {
        @Override
        public ReadinessState process(ComponentRunner runner) {
          int hbaseCount = 0;
          try {
            System.out.println("Waiting...");
            ResultScanner resultScanner = pcapTable.getScanner(columnFamily.getBytes(), columnIdentifier.getBytes());
            while (resultScanner.next() != null) hbaseCount++;
          } catch (IOException e) {
            e.printStackTrace();
          }
          if (hbaseCount == pcapEntries.size()) {
            return ReadinessState.READY;
          } else {
            return ReadinessState.NOT_READY;
          }
        }

        @Override
        public Void getResult() {
          return null;
        }
      });
      ResultScanner resultScanner = pcapTable.getScanner(columnFamily.getBytes(), columnIdentifier.getBytes());
      Result result;
      int rowCount = 0;
      while ((result = resultScanner.next()) != null) {
        String rowKey = new String(result.getRow());
        byte[] hbaseValue = result.getValue(columnFamily.getBytes(), columnIdentifier.getBytes());
        byte[] originalValue = pcapEntries.get(rowKey);
        Assert.assertNotNull("Could not find pcap with key " + rowKey + " in sample data", originalValue);
        Assert.assertArrayEquals("Raw values are different for key " + rowKey, originalValue, hbaseValue);
        rowCount++;
      }
      Assert.assertEquals(pcapEntries.size(), rowCount);
      System.out.println("Ended");
    } finally {
      runner.stop();
      clearOutDir(outDir);
      clearOutDir(queryDir);
    }
  }
}
