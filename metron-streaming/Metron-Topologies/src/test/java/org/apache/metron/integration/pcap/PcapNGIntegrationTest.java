package org.apache.metron.integration.pcap;

import backtype.storm.Config;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import kafka.consumer.ConsumerIterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.clients.producer.Producer;
import org.apache.metron.Constants;
import org.apache.metron.helpers.services.mr.PcapJob;
import org.apache.metron.integration.util.UnitTestHelper;
import org.apache.metron.integration.util.integration.ComponentRunner;
import org.apache.metron.integration.util.integration.Processor;
import org.apache.metron.integration.util.integration.ReadinessState;
import org.apache.metron.integration.util.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.util.integration.components.KafkaWithZKComponent;
import org.apache.metron.integration.util.integration.components.MRComponent;
import org.apache.metron.integration.util.integration.util.KafkaUtil;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapParser;
import org.apache.metron.spout.pcap.HDFSWriterCallback;
import org.apache.metron.spout.pcap.PartitionHDFSWriter;
import org.apache.metron.test.converters.HexStringConverter;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

public class PcapNGIntegrationTest {
  private static String BASE_DIR = "pcap_ng";
  private static String DATA_DIR = BASE_DIR + "/data_dir";
  private static String QUERY_DIR = BASE_DIR + "/query";
  private String topologiesDir = "src/main/resources/Metron_Configs/topologies";
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
   /* try {
      FileSystem fs = FileSystem.get(config);
      int i = 0;
      for(RemoteIterator<LocatedFileStatus> it =  fs.listFiles(new Path(outDir.getAbsolutePath()), false);it.hasNext();++i) {
        it.next();
      }
      return i;
    } catch (IOException e) {
      throw new RuntimeException("failed to get the num files", e);
    }*/
    return outDir.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return !name.endsWith(".crc");
      }
    }).length;
  }

  private static Iterable<Map.Entry<byte[], byte[]>> readPcaps(File pcapFile) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(pcapFile));
    List<Map.Entry<byte[], byte[]> > ret = new ArrayList<>();
    HexStringConverter converter = new HexStringConverter();
    long ts = 0L;
    PcapParser parser = new PcapParser();
    parser.init();
    for(String line = null;(line = br.readLine()) != null;) {
      byte[] pcapWithHeader = converter.convert(line);
      byte[] pcapRaw = new byte[pcapWithHeader.length - PartitionHDFSWriter.PCAP_GLOBAL_HEADER.length];
      System.arraycopy(pcapWithHeader, PartitionHDFSWriter.PCAP_GLOBAL_HEADER.length, pcapRaw, 0, pcapRaw.length);
      List<PacketInfo> l = parser.getPacketInfo(pcapWithHeader);
      ret.add(new AbstractMap.SimpleImmutableEntry<>(Bytes.toBytes(ts++), pcapRaw));
    }
    return Iterables.limit(ret, 2*(ret.size()/2));
  }

  @Test
  public void testTopology() throws Exception {
    if (!new File(topologiesDir).exists()) {
      topologiesDir = UnitTestHelper.findDir("topologies");
    }
    targetDir = UnitTestHelper.findDir("target");
    final String kafkaTopic = "pcap";
    final File outDir = getOutDir(targetDir);
    final File queryDir = getQueryDir(targetDir);
    clearOutDir(outDir);
    clearOutDir(queryDir);

    File baseDir = new File(new File(targetDir), BASE_DIR);
    //Assert.assertEquals(0, numFiles(outDir));
    Assert.assertNotNull(topologiesDir);
    Assert.assertNotNull(targetDir);
    File pcapFile = new File(topologiesDir + "/../../SampleInput/PCAPExampleOutput");
    final List<Map.Entry<byte[], byte[]>> pcapEntries = Lists.newArrayList(readPcaps(pcapFile));
    Assert.assertTrue(Iterables.size(pcapEntries) > 0);
    final Properties topologyProperties = new Properties() {{
      setProperty("spout.kafka.topic.pcap", kafkaTopic);
      setProperty("kafka.pcap.out", outDir.getAbsolutePath());
      setProperty("kafka.pcap.numPackets", "2");
      setProperty("kafka.pcap.maxTimeMS", "200000000");
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
                                   }
            );
    //.withExistingZookeeper("localhost:2000");


    final MRComponent mr = new MRComponent().withBasePath(baseDir.getAbsolutePath());

    FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(topologiesDir + "/pcap_ng/remote.yaml"))
            .withTopologyName("pcap_ng")
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
      Producer<byte[], byte[]> producer = kafkaComponent.createProducer(byte[].class, byte[].class);
      KafkaUtil.send(producer, pcapEntries, kafkaTopic, 2);
      System.out.println("Sent pcap data: " + pcapEntries.size());
      {
        int numMessages = 0;
        ConsumerIterator<?, ?> it = kafkaComponent.getStreamIterator(kafkaTopic);
        for (int i = 0; i < pcapEntries.size(); ++i, it.next()) {
          numMessages++;
        }
        Assert.assertEquals(pcapEntries.size(), numMessages);
        System.out.println("Wrote " + pcapEntries.size() + " to kafka");
      }
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
      //now we can do a query.
      PcapJob job = new PcapJob();
      {
        System.err.println("Starting job\n\n===============================================");
        List<byte[]> results =
        job.query(new Path(outDir.getAbsolutePath())
                , new Path(queryDir.getAbsolutePath())
                , 0l
                , 1l
                , new EnumMap<Constants.Fields, String>(Constants.Fields.class)
                , new Configuration()
                , FileSystem.get(new Configuration())
                );
        Assert.assertEquals(results.size(), 2);
      }
      {
        System.err.println("Starting job\n\n===============================================");
        List<byte[]> results =
        job.query(new Path(outDir.getAbsolutePath())
                , new Path(queryDir.getAbsolutePath())
                , 0l
                , 1l
                , new EnumMap<Constants.Fields, String>(Constants.Fields.class) {{
                  put(Constants.Fields.DST_ADDR, "207.28.210.1");
                }}
                , new Configuration()
                , FileSystem.get(new Configuration())
                );
        Assert.assertEquals(results.size(), 0);
      }
      {
        System.err.println("Starting job\n\n===============================================");
        List<byte[]> results =
        job.query(new Path(outDir.getAbsolutePath())
                , new Path(queryDir.getAbsolutePath())
                , 0l
                , 1l
                , new EnumMap<Constants.Fields, String>(Constants.Fields.class) {{
                  put(Constants.Fields.PROTOCOL, "foo");
                }}
                , new Configuration()
                , FileSystem.get(new Configuration())
                );
        Assert.assertEquals(results.size(), 0);
      }
      System.out.println("Ended");
    }
    finally {
      runner.stop();
      clearOutDir(outDir);
      clearOutDir(queryDir);
    }
  }
}
