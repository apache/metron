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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import kafka.consumer.ConsumerIterator;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.kafka.clients.producer.Producer;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.HDFSUtils;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.MRComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.integration.utils.KafkaUtil;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.Statusable;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.PcapMerger;
import org.apache.metron.pcap.config.FixedPcapConfig;
import org.apache.metron.pcap.config.PcapOptions;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.apache.metron.pcap.finalizer.PcapFinalizerStrategies;
import org.apache.metron.pcap.mr.PcapJob;
import org.apache.metron.pcap.query.PcapCli;
import org.apache.metron.spout.pcap.Endianness;
import org.apache.metron.spout.pcap.deserializer.Deserializers;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class PcapTopologyIntegrationTest extends BaseIntegrationTest {
  final static String KAFKA_TOPIC = "pcap";
  private static String BASE_DIR = "pcap";
  private static String DATA_DIR = BASE_DIR + "/data_dir";
  private static String INTERIM_RESULT = BASE_DIR + "/query";
  private static String OUTPUT_DIR = BASE_DIR + "/output";
  private static final int MAX_RETRIES = 30;
  private static final int SLEEP_MS = 500;
  private String topologiesDir = "src/main/flux";
  private String targetDir = "target";

  private static void clearOutDirs(File... dirs) throws IOException {
    for(File dir: dirs) {
      for(File f : dir.listFiles()) {
        if (f.isDirectory()) {
          FileUtils.deleteDirectory(f);
        } else {
          f.delete();
        }
      }
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

  // This will eventually be completely deprecated.  As it takes a significant amount of testing, the test is being disabled.
  @Ignore
  @Test
  public void testTimestampInPacket() throws Exception {
    testTopology(new Function<Properties, Void>() {
      @Nullable
      @Override
      public Void apply(@Nullable Properties input) {
        input.setProperty("kafka.pcap.ts_scheme", Deserializers.FROM_PACKET.toString());
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
        input.setProperty("kafka.pcap.ts_scheme", Deserializers.FROM_KEY.toString());
        return null;
      }
    }, new SendEntries() {
      @Override
      public void send(KafkaComponent kafkaComponent, List<Map.Entry<byte[], byte[]>> pcapEntries) throws Exception {
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
    public void send(KafkaComponent kafkaComponent, List<Map.Entry<byte[], byte[]>> entries) throws Exception;
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
    final File inputDir = getDir(targetDir, DATA_DIR);
    final File interimResultDir = getDir(targetDir, INTERIM_RESULT);
    final File outputDir = getDir(targetDir, OUTPUT_DIR);
    clearOutDirs(inputDir, interimResultDir, outputDir);

    File baseDir = new File(new File(targetDir), BASE_DIR);
    //Assert.assertEquals(0, numFiles(outDir));
    Assert.assertNotNull(topologiesDir);
    Assert.assertNotNull(targetDir);
    Path pcapFile = new Path("../metron-integration-test/src/main/sample/data/SampleInput/PCAPExampleOutput");
    final List<Map.Entry<byte[], byte[]>> pcapEntries = Lists.newArrayList(readPcaps(pcapFile, withHeaders));
    Assert.assertTrue(Iterables.size(pcapEntries) > 0);
    final Properties topologyProperties = new Properties() {{
      setProperty("topology.workers", "1");
      setProperty("topology.worker.childopts", "");
      setProperty("spout.kafka.topic.pcap", KAFKA_TOPIC);
      setProperty("kafka.pcap.start", "EARLIEST");
      setProperty("kafka.pcap.out", inputDir.getAbsolutePath());
      setProperty("kafka.pcap.numPackets", "2");
      setProperty("kafka.pcap.maxTimeMS", "200000000");
      setProperty("kafka.pcap.ts_granularity", "NANOSECONDS");
      setProperty("kafka.spout.parallelism", "1");
      setProperty("topology.auto-credentials", "[]");
      setProperty("kafka.security.protocol", "PLAINTEXT");
      setProperty("hdfs.sync.every", "1");
      setProperty("hdfs.replication.factor", "-1");
    }};
    updatePropertiesCallback.apply(topologyProperties);

    final ZKServerComponent zkServerComponent = getZKServerComponent(topologyProperties);

    final KafkaComponent kafkaComponent = getKafkaComponent(topologyProperties, Collections.singletonList(
            new KafkaComponent.Topic(KAFKA_TOPIC, 1)));


    final MRComponent mr = new MRComponent().withBasePath(baseDir.getAbsolutePath());

    FluxTopologyComponent fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(topologiesDir + "/pcap/remote.yaml"))
            .withTopologyName("pcap")
            .withTopologyProperties(topologyProperties)
            .build();
    //UnitTestHelper.verboseLogging();
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("mr", mr)
            .withComponent("zk",zkServerComponent)
            .withComponent("kafka", kafkaComponent)
            .withComponent("storm", fluxComponent)
            .withMaxTimeMS(-1)
            .withMillisecondsBetweenAttempts(2000)
            .withNumRetries(10)
            .withCustomShutdownOrder(new String[]{"storm","kafka","zk","mr"})
            .build();
    try {
      runner.start();

      fluxComponent.submitTopology();
      sendPcapEntriesCallback.send(kafkaComponent, pcapEntries);
      runner.process(new Processor<Void>() {
        @Override
        public ReadinessState process(ComponentRunner runner) {
          int numFiles = numFiles(inputDir, mr.getConfiguration());
          int expectedNumFiles = pcapEntries.size() / 2;
          if (numFiles == expectedNumFiles) {
            return ReadinessState.READY;
          } else {
            return ReadinessState.NOT_READY;
          }
        }

        @Override
        public ProcessorResult<Void> getResult() {
          return null;
        }
      });

      FixedPcapConfig configuration = new FixedPcapConfig(PcapCli.PREFIX_STRATEGY);
      Configuration hadoopConf = new Configuration();
      PcapOptions.JOB_NAME.put(configuration, "jobName");
      PcapOptions.HADOOP_CONF.put(configuration, hadoopConf);
      PcapOptions.FILESYSTEM.put(configuration, FileSystem.get(hadoopConf));
      PcapOptions.BASE_PATH.put(configuration, new Path(inputDir.getAbsolutePath()));
      PcapOptions.BASE_INTERIM_RESULT_PATH.put(configuration, new Path(interimResultDir.getAbsolutePath()));
      PcapOptions.START_TIME_NS.put(configuration, getTimestamp(4, pcapEntries));
      PcapOptions.END_TIME_NS.put(configuration, getTimestamp(5, pcapEntries));
      PcapOptions.NUM_REDUCERS.put(configuration, 10);
      PcapOptions.NUM_RECORDS_PER_FILE.put(configuration, 2);
      PcapOptions.FINAL_OUTPUT_PATH.put(configuration, new Path(outputDir.getAbsolutePath()));
      {
        //Ensure that only two pcaps are returned when we look at 4 and 5
        PcapOptions.FILTER_IMPL.put(configuration, new FixedPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, new HashMap());
        PcapJob<Map<String, String>> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(results.get().getSize(), 1);
      }
      {
        // Ensure that only two pcaps are returned when we look at 4 and 5
        // test with empty query filter
        PcapOptions.FILTER_IMPL.put(configuration, new QueryPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, "");
        PcapJob<String> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(results.get().getSize(), 1);
      }
      {
        //ensure that none get returned since that destination IP address isn't in the dataset
        PcapOptions.FILTER_IMPL.put(configuration, new FixedPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, new HashMap<String, String>() {{
          put(Constants.Fields.DST_ADDR.getName(), "207.28.210.1");
        }});
        PcapJob<Map<String, String>> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(results.get().getSize(), 0);
      }
      {
        // ensure that none get returned since that destination IP address isn't in the dataset
        // test with query filter
        PcapOptions.FILTER_IMPL.put(configuration, new QueryPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, "ip_dst_addr == '207.28.210.1'");
        PcapJob<String> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(results.get().getSize(), 0);
      }
      {
        //same with protocol as before with the destination addr
        PcapOptions.FILTER_IMPL.put(configuration, new FixedPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, new HashMap<String, String>() {{
          put(Constants.Fields.PROTOCOL.getName(), "foo");
        }});
        PcapJob<Map<String, String>> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(results.get().getSize(), 0);
      }
      {
        //same with protocol as before with the destination addr
        //test with query filter
        PcapOptions.FILTER_IMPL.put(configuration, new QueryPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, "protocol == 'foo'");
        PcapJob<String> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(results.get().getSize(), 0);
      }
      {
        //make sure I get them all.
        PcapOptions.FILTER_IMPL.put(configuration, new FixedPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, new HashMap<>());
        PcapOptions.START_TIME_NS.put(configuration, getTimestamp(0, pcapEntries));
        PcapOptions.END_TIME_NS.put(configuration, getTimestamp(pcapEntries.size()-1, pcapEntries) + 1);
        PcapJob<Map<String, String>> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(10, results.get().getSize());
      }
      {
        //make sure I get them all.
        //with query filter
        PcapOptions.FILTER_IMPL.put(configuration, new QueryPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, "");
        PcapOptions.START_TIME_NS.put(configuration, getTimestamp(0, pcapEntries));
        PcapOptions.END_TIME_NS.put(configuration, getTimestamp(pcapEntries.size()-1, pcapEntries) + 1);
        PcapJob<String> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(10, results.get().getSize());
      }
      {
        PcapOptions.FILTER_IMPL.put(configuration, new FixedPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, new HashMap<String, String>() {{
          put(Constants.Fields.DST_PORT.getName(), "22");
        }});
        PcapOptions.NUM_RECORDS_PER_FILE.put(configuration, 1);
        PcapJob<Map<String, String>> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertTrue(results.get().getSize() > 0);
        Assert.assertEquals(Iterables.size(bytes)
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
        PcapMerger.merge(baos, HDFSUtils.readBytes(results.get().getPage(0)));
        Assert.assertTrue(baos.toByteArray().length > 0);
      }
      {
        //same with protocol as before with the destination addr
        //test with query filter
        PcapOptions.FILTER_IMPL.put(configuration, new QueryPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, "ip_dst_port == 22");
        PcapJob<String> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(Iterables.size(bytes)
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
        PcapMerger.merge(baos, HDFSUtils.readBytes(results.get().getPage(0)));
        Assert.assertTrue(baos.toByteArray().length > 0);
      }
      {
        // test with query filter ip_dst_port > 20 and ip_dst_port < 55792
        PcapOptions.FILTER_IMPL.put(configuration, new QueryPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, "ip_dst_port > 20 and ip_dst_port < 55792");
        PcapJob<String> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(Iterables.size(bytes)
                , Iterables.size(filterPcaps(pcapEntries, new Predicate<JSONObject>() {
                          @Override
                          public boolean apply(@Nullable JSONObject input) {
                            Object prt = input.get(Constants.Fields.DST_PORT.getName());
                            return prt != null && ((Long) prt > 20 && (Long) prt < 55792);
                          }
                        }, withHeaders)
                )
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PcapMerger.merge(baos, HDFSUtils.readBytes(results.get().getPage(0)));
        Assert.assertTrue(baos.toByteArray().length > 0);
      }
      {
        //test with query filter ip_dst_port > 55790
        PcapOptions.FILTER_IMPL.put(configuration, new QueryPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, "ip_dst_port > 55790");
        PcapJob<String> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(Iterables.size(bytes)
                , Iterables.size(filterPcaps(pcapEntries, new Predicate<JSONObject>() {
                          @Override
                          public boolean apply(@Nullable JSONObject input) {
                            Object prt = input.get(Constants.Fields.DST_PORT.getName());
                            return prt != null && (Long) prt > 55790;
                          }
                        }, withHeaders)
                )
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PcapMerger.merge(baos, HDFSUtils.readBytes(results.get().getPage(0)));
        Assert.assertTrue(baos.toByteArray().length > 0);
      }
      {
        //test with query filter and byte array matching
        PcapOptions.FILTER_IMPL.put(configuration, new QueryPcapFilter.Configurator());
        PcapOptions.FIELDS.put(configuration, "BYTEARRAY_MATCHER('2f56abd814bc56420489ca38e7faf8cec3d4', packet)");
        PcapOptions.START_TIME_NS.put(configuration, getTimestamp(0, pcapEntries));
        PcapOptions.END_TIME_NS.put(configuration, getTimestamp(pcapEntries.size()-1, pcapEntries) + 1);
        PcapJob<String> job = new PcapJob<>();
        Statusable<Path> results = job.submit(PcapFinalizerStrategies.CLI, configuration);
        Assert.assertEquals(Statusable.JobType.MAP_REDUCE, results.getJobType());
        Assert.assertEquals(JobStatus.State.RUNNING, results.getStatus().getState());
        waitForJob(results);

        Assert.assertEquals(JobStatus.State.SUCCEEDED, results.getStatus().getState());
        Iterable<byte[]> bytes = Iterables.transform(results.get(), path -> {
          try {
            return HDFSUtils.readBytes(path);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
        assertInOrder(bytes);
        Assert.assertEquals(1, results.get().getSize());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PcapMerger.merge(baos, HDFSUtils.readBytes(results.get().getPage(0)));
        Assert.assertTrue(baos.toByteArray().length > 0);
      }

      System.out.println("Ended");
    } finally {
      runner.stop();
      clearOutDirs(inputDir, interimResultDir, outputDir);
    }
  }

  private void waitForJob(Statusable statusable) throws Exception {
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      if (statusable.isDone()) {
        return;
      }
    }
    throw new Exception("Job did not complete within " + (MAX_RETRIES * SLEEP_MS) + " seconds");
  }

  private File getDir(String targetDir, String childDir) {
    File directory = new File(new File(targetDir), childDir);
    if (!directory.exists()) {
      directory.mkdirs();
    }
    return directory;
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
      //if you are debugging and want the hex dump of the packets, uncomment the following:

      //for(byte b : pcapWithHeader) {
      //  System.out.print(String.format("%02x", b));
      //}
      //System.out.println("");

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

  public static void assertInOrder(Iterable<byte[]> packets) {
    long previous = 0;
    for(byte[] packet : packets) {
      for(JSONObject json : TO_JSONS.apply(packet)) {
        Long current = Long.parseLong(json.get("ts_micro").toString());
        Assert.assertNotNull(current);
        Assert.assertTrue(Long.compareUnsigned(current, previous) >= 0);
        previous = current;
      }
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
