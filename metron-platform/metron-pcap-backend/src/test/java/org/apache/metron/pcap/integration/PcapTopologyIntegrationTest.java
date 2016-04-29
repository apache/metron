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

package org.apache.metron.pcap.integration;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import kafka.consumer.ConsumerIterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.kafka.clients.producer.Producer;
import org.apache.metron.common.Constants;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaWithZKComponent;
import org.apache.metron.integration.components.MRComponent;
import org.apache.metron.integration.utils.KafkaUtil;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.PcapMerger;
import org.apache.metron.pcap.mr.PcapJob;
import org.apache.metron.spout.pcap.Endianness;
import org.apache.metron.spout.pcap.scheme.TimestampScheme;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

public class PcapTopologyIntegrationTest {
  final static String KAFKA_TOPIC = "pcap";
  private static String BASE_DIR = "pcap";
  private static String DATA_DIR = BASE_DIR + "/data_dir";
  private static String QUERY_DIR = BASE_DIR + "/query";
  private String topologiesDir = "src/main/flux";
  private String targetDir = "target";
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
    for(File f : outDir.listFiles()) {
      f.delete();
    }
  }
  private static int numFiles(File outDir, Configuration config) {

    return outDir.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return !name.endsWith(".crc");
      }
    }).length;
  }

  private static Iterable<Map.Entry<byte[], byte[]>> readPcaps(Path pcapFile, boolean withHeaders) throws IOException {
    SequenceFile.Reader reader = new SequenceFile.Reader(new Configuration(),
            SequenceFile.Reader.file(pcapFile)
    );
    List<Map.Entry<byte[], byte[]> > ret = new ArrayList<>();
    IntWritable key = new IntWritable();
    BytesWritable value = new BytesWritable();
    while (reader.next(key, value)) {
      byte[] pcapWithHeader = value.copyBytes();
      long calculatedTs = PcapHelper.getTimestamp(pcapWithHeader);
      {

        List<PacketInfo> info = PcapHelper.toPacketInfo(pcapWithHeader);
        for(PacketInfo pi : info) {
          Assert.assertEquals(calculatedTs, pi.getPacketTimeInNanos());
          //IF you are debugging and want to see the packets, uncomment the following.
          //System.out.println( Long.toUnsignedString(calculatedTs) + " => " + pi.getJsonDoc());
        }
      }
      if(withHeaders) {
        ret.add(new AbstractMap.SimpleImmutableEntry<>(Bytes.toBytes(calculatedTs), pcapWithHeader));
      }
      else {
        byte[] pcapRaw = new byte[pcapWithHeader.length - PcapHelper.GLOBAL_HEADER_SIZE - PcapHelper.PACKET_HEADER_SIZE];
        System.arraycopy(pcapWithHeader, PcapHelper.GLOBAL_HEADER_SIZE + PcapHelper.PACKET_HEADER_SIZE, pcapRaw, 0, pcapRaw.length);
        ret.add(new AbstractMap.SimpleImmutableEntry<>(Bytes.toBytes(calculatedTs), pcapRaw));
      }
    }
    return Iterables.limit(ret, 2*(ret.size()/2));
  }

  @Test
  public void testTimestampInPacket() throws Exception {
    testTopology(new Function<Properties, Void>() {
      @Nullable
      @Override
      public Void apply(@Nullable Properties input) {
        input.setProperty("kafka.pcap.ts_scheme", TimestampScheme.FROM_PACKET.toString());
        return null;
      }
    }, (kafkaComponent, pcapEntries) -> kafkaComponent.writeMessages( KAFKA_TOPIC
                                                                    , Collections2.transform(pcapEntries
                                                                                            , input -> input.getValue()
                                                                                            )
                                                                    )
    , true
               );
  }
  @Test
  public void testTimestampInKey() throws Exception {
    testTopology(new Function<Properties, Void>() {
      @Nullable
      @Override
      public Void apply(@Nullable Properties input) {
        input.setProperty("kafka.pcap.ts_scheme", TimestampScheme.FROM_KEY.toString());
        return null;
      }
    }, new SendEntries() {
      @Override
      public void send(KafkaWithZKComponent kafkaComponent, List<Map.Entry<byte[], byte[]>> pcapEntries) throws Exception {
        Producer<byte[], byte[]> producer = kafkaComponent.createProducer(byte[].class, byte[].class);
        KafkaUtil.send(producer, pcapEntries, KAFKA_TOPIC, 2);
        System.out.println("Sent pcap data: " + pcapEntries.size());
        {
          int numMessages = 0;
          ConsumerIterator<?, ?> it = kafkaComponent.getStreamIterator(KAFKA_TOPIC);
          for (int i = 0; i < pcapEntries.size(); ++i, it.next()) {
            numMessages++;
          }
          Assert.assertEquals(pcapEntries.size(), numMessages);
          System.out.println("Wrote " + pcapEntries.size() + " to kafka");
        }
      }
    }, false);
  }

  private static long getTimestamp(int offset, List<Map.Entry<byte[], byte[]>> entries) {
    return Bytes.toLong(entries.get(offset).getKey());
  }

  private static interface SendEntries {
    public void send(KafkaWithZKComponent kafkaComponent, List<Map.Entry<byte[], byte[]>> entries) throws Exception;
  }

  public void testTopology(Function<Properties, Void> updatePropertiesCallback
                          ,SendEntries sendPcapEntriesCallback
                          ,boolean withHeaders
                          )
          throws Exception
  {
    if (!new File(topologiesDir).exists()) {
      topologiesDir = UnitTestHelper.findDir("topologies");
    }
    targetDir = UnitTestHelper.findDir("target");
    final File outDir = getOutDir(targetDir);
    final File queryDir = getQueryDir(targetDir);
    clearOutDir(outDir);
    clearOutDir(queryDir);

    File baseDir = new File(new File(targetDir), BASE_DIR);
    //Assert.assertEquals(0, numFiles(outDir));
    Assert.assertNotNull(topologiesDir);
    Assert.assertNotNull(targetDir);
    Path pcapFile = new Path("../metron-integration-test/src/main/resources/sample/data/SampleInput/PCAPExampleOutput");
    final List<Map.Entry<byte[], byte[]>> pcapEntries = Lists.newArrayList(readPcaps(pcapFile, withHeaders));
    Assert.assertTrue(Iterables.size(pcapEntries) > 0);
    final Properties topologyProperties = new Properties() {{
      setProperty("spout.kafka.topic.pcap", KAFKA_TOPIC);
      setProperty("kafka.pcap.start", "BEGINNING");
      setProperty("kafka.pcap.out", outDir.getAbsolutePath());
      setProperty("kafka.pcap.numPackets", "2");
      setProperty("kafka.pcap.maxTimeMS", "200000000");
      setProperty("kafka.pcap.ts_granularity", "NANOSECONDS");
    }};
    updatePropertiesCallback.apply(topologyProperties);

    final KafkaWithZKComponent kafkaComponent = new KafkaWithZKComponent().withTopics(new ArrayList<KafkaWithZKComponent.Topic>() {{
      add(new KafkaWithZKComponent.Topic(KAFKA_TOPIC, 1));
    }})
            .withPostStartCallback(new Function<KafkaWithZKComponent, Void>() {
                                     @Nullable
                                     @Override
                                     public Void apply(@Nullable KafkaWithZKComponent kafkaWithZKComponent) {

                                       topologyProperties.setProperty("kafka.zk", kafkaWithZKComponent.getZookeeperConnect());
                                       return null;
                                     }
                                   }
            );
    //.withExistingZookeeper("localhost:2000");


    final MRComponent mr = new MRComponent().withBasePath(baseDir.getAbsolutePath());

    FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(topologiesDir + "/pcap/remote.yaml"))
            .withTopologyName("pcap")
            .withTopologyProperties(topologyProperties)
            .build();
    UnitTestHelper.verboseLogging();
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("mr", mr)
            .withComponent("kafka", kafkaComponent)
            .withComponent("storm", fluxComponent)
            .withMaxTimeMS(-1)
            .withMillisecondsBetweenAttempts(2000)
            .withNumRetries(10)
            .build();
    try {
      runner.start();
      System.out.println("Components started...");

      fluxComponent.submitTopology();
      sendPcapEntriesCallback.send(kafkaComponent, pcapEntries);
      runner.process(new Processor<Void>() {
        @Override
        public ReadinessState process(ComponentRunner runner) {
          int numFiles = numFiles(outDir, mr.getConfiguration());
          int expectedNumFiles = pcapEntries.size() / 2;
          if (numFiles == expectedNumFiles) {
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
      PcapJob job = new PcapJob();
      {
        //Ensure that only two pcaps are returned when we look at 4 and 5
        List<byte[]> results =
                job.query(new Path(outDir.getAbsolutePath())
                        , new Path(queryDir.getAbsolutePath())
                        , getTimestamp(4, pcapEntries)
                        , getTimestamp(5, pcapEntries)
                        , new EnumMap<>(Constants.Fields.class)
                        , new Configuration()
                        , FileSystem.get(new Configuration())
                );
        Assert.assertEquals(results.size(), 2);
      }
      {
        //ensure that none get returned since that destination IP address isn't in the dataset
        List<byte[]> results =
                job.query(new Path(outDir.getAbsolutePath())
                        , new Path(queryDir.getAbsolutePath())
                        , getTimestamp(0, pcapEntries)
                        , getTimestamp(1, pcapEntries)
                        , new EnumMap<Constants.Fields, String>(Constants.Fields.class) {{
                          put(Constants.Fields.DST_ADDR, "207.28.210.1");
                        }}
                        , new Configuration()
                        , FileSystem.get(new Configuration())
                );
        Assert.assertEquals(results.size(), 0);
      }
      {
        //same with protocol as before with the destination addr
        List<byte[]> results =
                job.query(new Path(outDir.getAbsolutePath())
                        , new Path(queryDir.getAbsolutePath())
                        , getTimestamp(0, pcapEntries)
                        , getTimestamp(1, pcapEntries)
                        , new EnumMap<Constants.Fields, String>(Constants.Fields.class) {{
                          put(Constants.Fields.PROTOCOL, "foo");
                        }}
                        , new Configuration()
                        , FileSystem.get(new Configuration())
                );
        Assert.assertEquals(results.size(), 0);
      }
      {
        //make sure I get them all.
        List<byte[]> results =
                job.query(new Path(outDir.getAbsolutePath())
                        , new Path(queryDir.getAbsolutePath())
                        , getTimestamp(0, pcapEntries)
                        , getTimestamp(pcapEntries.size()-1, pcapEntries) + 1
                        , new EnumMap<>(Constants.Fields.class)
                        , new Configuration()
                        , FileSystem.get(new Configuration())
                );
        Assert.assertEquals(results.size(), pcapEntries.size());
      }
      {
        List<byte[]> results =
                job.query(new Path(outDir.getAbsolutePath())
                        , new Path(queryDir.getAbsolutePath())
                        , getTimestamp(0, pcapEntries)
                        , getTimestamp(pcapEntries.size()-1, pcapEntries) + 1
                        , new EnumMap<Constants.Fields, String>(Constants.Fields.class) {{
                          put(Constants.Fields.DST_PORT, "22");
                        }}
                        , new Configuration()
                        , FileSystem.get(new Configuration())
                );
        Assert.assertTrue(results.size() > 0);
        Assert.assertEquals(results.size()
                , Iterables.size(filterPcaps(pcapEntries, new Predicate<JSONObject>() {
                  @Override
                  public boolean apply(@Nullable JSONObject input) {
                    Object prt = input.get(Constants.Fields.DST_PORT.getName());
                    return prt != null && prt.toString().equals("22");
                  }
                }, withHeaders)
                )
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PcapMerger.merge(baos, results);
        Assert.assertTrue(baos.toByteArray().length > 0);
      }
      System.out.println("Ended");
    } finally {
      runner.stop();
      clearOutDir(outDir);
      clearOutDir(queryDir);
    }
  }

  public static Function<byte[], Iterable<JSONObject>> TO_JSONS = new Function<byte[], Iterable<JSONObject>>() {
    @Nullable
    @Override
    public Iterable<JSONObject> apply(@Nullable byte[] input) {
      try {
        return PcapHelper.toJSON(PcapHelper.toPacketInfo(input));
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  };

  private Iterable<JSONObject> filterPcaps(Iterable<Map.Entry<byte[], byte[]>> pcaps
                                          ,Predicate<JSONObject> predicate
                                          ,boolean withHeaders
                                          )
  {
    Function<Map.Entry<byte[], byte[]>, byte[]> pcapTransform = null;
    if(!withHeaders) {
      final Endianness endianness = Endianness.getNativeEndianness();
      pcapTransform = kv -> PcapHelper.addGlobalHeader(PcapHelper.addPacketHeader(Bytes.toLong(kv.getKey())
                                                                                 , kv.getValue()
                                                                                 , endianness
                                                                                 )
                                                      , endianness
                                                      );
    }
    else {
      pcapTransform = kv -> kv.getValue();
    }
    return Iterables.filter(
              Iterables.concat(
                      Iterables.transform(
                              Iterables.transform(pcaps, pcapTransform)
                                         , TO_JSONS
                                         )
                              )
                           , predicate
                           );
  }
}
