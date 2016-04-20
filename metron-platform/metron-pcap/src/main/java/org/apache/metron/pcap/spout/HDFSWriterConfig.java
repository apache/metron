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
package org.apache.metron.pcap.spout;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HDFSWriterConfig implements Serializable {
  static final long serialVersionUID = 0xDEADBEEFL;
  private long numPackets;
  private long maxTimeMS;
  private String outputPath;
  private String zookeeperQuorum;

  public HDFSWriterConfig withOutputPath(String path) {
    outputPath = path;
    return this;
  }

  public HDFSWriterConfig withNumPackets(long n) {
    numPackets = n;
    return this;
  }

  public HDFSWriterConfig withMaxTimeMS(long t) {
    maxTimeMS = t;
    return this;
  }

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

  public Integer getZookeeperPort() {
    if(zookeeperQuorum != null) {
      String hostPort = Iterables.getFirst(Splitter.on(',').split(zookeeperQuorum), null);
      String portStr = Iterables.getLast(Splitter.on(':').split(hostPort));
      return Integer.parseInt(portStr);
    }
    return  null;
  }

  public String getOutputPath() {
    return outputPath;
  }

  public long getNumPackets() {
    return numPackets;
  }

  public long getMaxTimeMS() {
    return maxTimeMS;
  }

  @Override
  public String toString() {
    return "HDFSWriterConfig{" +
            "numPackets=" + numPackets +
            ", maxTimeMS=" + maxTimeMS +
            ", outputPath='" + outputPath + '\'' +
            '}';
  }
}
