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
package org.apache.metron.parsers.topology;

import backtype.storm.topology.TopologyBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.spout.kafka.SpoutConfig;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.parsers.bolt.ParserBolt;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.writer.KafkaWriter;
import org.json.simple.JSONObject;
import storm.kafka.KafkaSpout;
import storm.kafka.ZkHosts;

public class ParserTopologyBuilder {

  public static TopologyBuilder build(String zookeeperUrl,
                         String brokerUrl,
                         String sensorType,
                         SpoutConfig.Offset offset,
                         int spoutParallelism,
                         int parserParallelism) throws Exception {
    CuratorFramework client = ConfigurationsUtils.getClient(zookeeperUrl);
    client.start();
    SensorParserConfig sensorParserConfig = ConfigurationsUtils.readSensorParserConfigFromZookeeper(sensorType, client);
    client.close();
    String sensorTopic = sensorParserConfig.getSensorTopic() != null ? sensorParserConfig.getSensorTopic() : sensorType;
    TopologyBuilder builder = new TopologyBuilder();
    ZkHosts zkHosts = new ZkHosts(zookeeperUrl);
    SpoutConfig spoutConfig = new SpoutConfig(zkHosts, sensorTopic, "", sensorTopic).from(offset);
    KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);
    builder.setSpout("kafkaSpout", kafkaSpout, spoutParallelism);
    MessageParser<JSONObject> parser = ReflectionUtils.createInstance(sensorParserConfig.getParserClassName());
    parser.configure(sensorParserConfig.getParserConfig());
    KafkaWriter writer = new KafkaWriter(brokerUrl);
    ParserBolt parserBolt = new ParserBolt(zookeeperUrl, sensorType, parser, writer);
    builder.setBolt("parserBolt", parserBolt, parserParallelism).shuffleGrouping("kafkaSpout");
    return builder;
  }
}
