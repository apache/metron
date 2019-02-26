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

package org.apache.metron.common.utils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.kafka.common.protocol.SecurityProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum KafkaUtils {
  INSTANCE;
  public static final String SECURITY_PROTOCOL = "security.protocol";

  /**
   * Retrieves the list of Kafka brokers from ZooKeeper.
   *
   * @param zkQuorum The ZK quorum to use to connect with ZK
   * @return List of string for Kafka brokers in host:port format
   * @throws Exception If there's an issue retrieving brokers
   */
  public List<String> getBrokersFromZookeeper(String zkQuorum) throws Exception {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    CuratorFramework framework = CuratorFrameworkFactory.newClient(zkQuorum, retryPolicy);
    framework.start();
    try {
      return getBrokersFromZookeeper(framework);
    } finally {
      framework.close();
    }
  }

  /**
   * Retrieves the list of Kafka brokers from ZooKeeper.
   *
   * @param client ZK client to use during lookup
   * @return List of string for Kafka brokers in host:port format
   * @throws Exception If there's an issue retrieving brokers
   */
  public List<String> getBrokersFromZookeeper(CuratorFramework client) throws Exception {
    List<String> ret = new ArrayList<>();
    for(String id : client.getChildren().forPath("/brokers/ids")) {
      byte[] data = client.getData().forPath("/brokers/ids/" + id);
      String brokerInfoStr = new String(data);
      Map<String, Object> brokerInfo = JSONUtils.INSTANCE.load(brokerInfoStr, JSONUtils.MAP_SUPPLIER);
      String host = (String) brokerInfo.get("host");
      if(host != null) {
        ret.add(host + ":" + brokerInfo.get("port"));
      }
      else {
        Object endpoints = brokerInfo.get("endpoints");
        if(endpoints != null && endpoints instanceof List) {
          List<String> eps = (List<String>)endpoints;
          for(String url : eps) {
            ret.addAll(fromEndpoint(url));
          }
        }
      }
    }
    return ret;
  }

  /**
   * Normalizes the Kafka security protocol name found in configs, in order to handle the
   * inconsistencies seen in Kafka versions (PLAINTEXTSASL vs. SASL_PLAINTEXT)
   *
   * @param configs The config to normalize the protocol in
   * @return The map with protocol adjusted
   */
  public Map<String, Object> normalizeProtocol(Map<String, Object> configs) {
    if(configs.containsKey(SECURITY_PROTOCOL)) {
      String protocol = normalizeProtocol((String)configs.get(SECURITY_PROTOCOL));
      configs.put(SECURITY_PROTOCOL, protocol);
    }
    return configs;
  }

  /**
   * Normalizes the Kafka security protocol name, in order to handle the inconsistencies seen in
   * Kafka versions (PLAINTEXTSASL vs. SASL_PLAINTEXT)
   *
   * @param protocol The protocol to normalize
   * @return The protocol recognized by Kafka
   */
  public String normalizeProtocol(String protocol) {
    if(protocol.equalsIgnoreCase("PLAINTEXTSASL") || protocol.equalsIgnoreCase("SASL_PLAINTEXT")) {
      if(SecurityProtocol.getNames().contains("PLAINTEXTSASL")) {
        return "PLAINTEXTSASL";
      }
      else if(SecurityProtocol.getNames().contains("SASL_PLAINTEXT")) {
        return "SASL_PLAINTEXT";
      }
      else {
        throw new IllegalStateException("Unable to find the appropriate SASL protocol, " +
                "viable options are: " + Joiner.on(",").join(SecurityProtocol.getNames()));
      }
    }
    else {
      return protocol.trim();
    }
  }
  /*
  The URL accepted is NOT a general URL, and is assumed to follow the format used by the Kafka structures in Zookeeper.
  See: https://cwiki.apache.org/confluence/display/KAFKA/Kafka+data+structures+in+Zookeeper
   */
  List<String> fromEndpoint(String url){
    List<String> ret = new ArrayList<>();
    if(url != null) {
      Iterable<String> splits = Splitter.on("//").split(url);
      if(Iterables.size(splits) == 2) {
        String hostPort = Iterables.getLast(splits);
        ret.add(hostPort);
      }
    }
    return ret;
  }
}
