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
package org.apache.metron.spout.pcap;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.spout.pcap.deserializer.Deserializers;
import org.apache.metron.spout.pcap.deserializer.KeyValueDeserializer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configure the HDFS Writer for PCap
 */
public class HDFSWriterConfig implements Serializable {
  static final long serialVersionUID = 0xDEADBEEFL;
  private long numPackets;
  private long maxTimeNS;
  private int syncEvery = 1;
  private int replicationFactor = -1;
  private String outputPath;
  private String zookeeperQuorum;
  private KeyValueDeserializer deserializer;
  private Map<String, Object> hdfsConfig = new HashMap<>();

  /**
   * Set the deserializer, the bit of logic that defines how the timestamp and packet are read.
   * @param deserializer One of the Deserializers in org.apache.metron.spout.pcap.deserializer.Deserializers
   * @param timestampConverter One of org.apache.metron.common.utils.timestamp.TimestampConverters.  This defines the units of our timestamp.
   * @return
   */
  public HDFSWriterConfig withDeserializer(String deserializer, String timestampConverter) {
    this.deserializer = Deserializers.create(deserializer, timestampConverter);
    return this;
  }

  /**
   * The output path in HDFS to write to.
   * @param path
   * @return
   */
  public HDFSWriterConfig withOutputPath(String path) {
    outputPath = path;
    return this;
  }

  /**
   * The number of packets to write before a file is rolled.
   * @param n
   * @return
   */
  public HDFSWriterConfig withNumPackets(long n) {
    numPackets = n;
    return this;
  }

  /**
   * The number of packets to write before a file is rolled.
   * @param n
   * @return
   */
  public HDFSWriterConfig withSyncEvery(int n) {
    syncEvery = n;
    return this;
  }

  /**
   * The map config for HDFS
   * @param config
   * @return
   */
  public HDFSWriterConfig withHDFSConfig(Map<String, Object> config) {
    hdfsConfig = config;
    return this;
  }

  /**
   * The HDFS replication factor to use. A value of -1 will not set replication factor.
   * @param n
   * @return
   */
  public HDFSWriterConfig withReplicationFactor(int n) {
    replicationFactor = n;
    return this;
  }

  /**
   * The total amount of time (in ms) to write before a file is rolled.
   * @param t
   * @return
   */
  public HDFSWriterConfig withMaxTimeMS(long t) {
    maxTimeNS = TimestampConverters.MILLISECONDS.toNanoseconds(t);
    return this;
  }

  /**
   * The zookeeper quorum to use.
   * @param zookeeperQuorum
   * @return
   */
  public HDFSWriterConfig withZookeeperQuorum(String zookeeperQuorum) {
    this.zookeeperQuorum = zookeeperQuorum;
    return this;
  }

  public List<String> getZookeeperServers() {
    List<String> out = new ArrayList<>();
    if(zookeeperQuorum != null) {
      for (String hostPort : Splitter.on(',').split(zookeeperQuorum)) {
        Iterable<String> tokens = Splitter.on(':').split(hostPort);
        String host = Iterables.getFirst(tokens, null);
        if(host != null) {
          out.add(host);
        }
      }
    }
    return out;
  }

  public Map<String, Object> getHDFSConfig() {
    return hdfsConfig;
  }

  public Integer getZookeeperPort() {
    if(zookeeperQuorum != null) {
      String hostPort = Iterables.getFirst(Splitter.on(',').split(zookeeperQuorum), null);
      String portStr = Iterables.getLast(Splitter.on(':').split(hostPort));
      return Integer.parseInt(portStr);
    }
    return  null;
  }

  public int getSyncEvery() {
    return syncEvery;
  }

  public int getReplicationFactor() {
    return replicationFactor;
  }

  public KeyValueDeserializer getDeserializer() {
    return deserializer;
  }

  public String getOutputPath() {
    return outputPath;
  }

  public long getNumPackets() {
    return numPackets;
  }

  public long getMaxTimeNS() {
    return maxTimeNS;
  }

  @Override
  public String toString() {
    return "HDFSWriterConfig{" +
            "numPackets=" + numPackets +
            ", maxTimeNS=" + maxTimeNS +
            ", outputPath='" + outputPath + '\'' +
            '}';
  }
}
