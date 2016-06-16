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
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.writer.ParserWriterConfiguration;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.apache.metron.common.interfaces.MessageWriter;
import org.apache.metron.common.spout.kafka.SpoutConfig;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.common.writer.AbstractWriter;
import org.apache.metron.parsers.bolt.ParserBolt;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.writer.KafkaWriter;
import org.json.simple.JSONObject;
import storm.kafka.KafkaSpout;
import storm.kafka.ZkHosts;

import java.util.Map;

public class ParserTopologyBuilder {

  public static TopologyBuilder build(String zookeeperUrl,
                        String brokerUrl,
                        String sensorType,
                        SpoutConfig.Offset offset,
                        int spoutParallelism,
                        int spoutNumTasks,
                        int parserParallelism,
                        int parserNumTasks
                                      ) throws Exception {
    return build(zookeeperUrl, brokerUrl, sensorType, offset, spoutParallelism, spoutNumTasks, parserParallelism, parserNumTasks, 1048576, 1048576);
  }

  public static TopologyBuilder build(String zookeeperUrl,
                         String brokerUrl,
                         String sensorType,
                         SpoutConfig.Offset offset,
                         int spoutParallelism,
                         int spoutNumTasks,
                         int parserParallelism,
                         int parserNumTasks,
                         int fetchSizeBytes,
                         int bufferSizeBtyes
                                     ) throws Exception {
    CuratorFramework client = ConfigurationsUtils.getClient(zookeeperUrl);
    client.start();
    ParserConfigurations configurations = new ParserConfigurations();
    ConfigurationsUtils.updateParserConfigsFromZookeeper(configurations, client);
    SensorParserConfig sensorParserConfig = configurations.getSensorParserConfig(sensorType);
    if(sensorParserConfig == null) {
      throw new IllegalStateException("Cannot find the parser configuration in zookeeper for " + sensorType + "." +
              "  Please check that it exists in zookeeper by using the zk_load_configs.sh -m DUMP command.");
    }
    client.close();
    String sensorTopic = sensorParserConfig.getSensorTopic() != null ? sensorParserConfig.getSensorTopic() : sensorType;
    TopologyBuilder builder = new TopologyBuilder();
    ZkHosts zkHosts = new ZkHosts(zookeeperUrl);
    SpoutConfig spoutConfig = new SpoutConfig(zkHosts, sensorTopic, "", sensorTopic).from(offset);
    spoutConfig.fetchSizeBytes = fetchSizeBytes;
    spoutConfig.bufferSizeBytes = bufferSizeBtyes;
    KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);
    System.out.println("Initializing parser topology KafkaSpout with the following configs:");
    System.out.println("Fetch Size Bytes: " + spoutConfig.fetchSizeBytes);
    System.out.println("Buffer Size Bytes: " + spoutConfig.bufferSizeBytes);
    builder.setSpout("kafkaSpout", kafkaSpout, spoutParallelism)
           .setNumTasks(spoutNumTasks);
    MessageParser<JSONObject> parser = ReflectionUtils.createInstance(sensorParserConfig.getParserClassName());
    parser.configure(sensorParserConfig.getParserConfig());
    ParserBolt parserBolt = null;
    {
      if(sensorParserConfig.getWriterClassName() == null) {
        KafkaWriter writer = new KafkaWriter(brokerUrl);
        writer.configure(sensorType, new ParserWriterConfiguration(configurations));
        parserBolt = new ParserBolt(zookeeperUrl, sensorType, parser, writer);
      }
      else {
        AbstractWriter writer = ReflectionUtils.createInstance(sensorParserConfig.getWriterClassName());
        writer.configure(sensorType, new ParserWriterConfiguration(configurations));
        if(writer instanceof BulkMessageWriter) {
          parserBolt = new ParserBolt(zookeeperUrl, sensorType, parser, (BulkMessageWriter<JSONObject>)writer);
        }
        else if(writer instanceof MessageWriter) {
          parserBolt = new ParserBolt(zookeeperUrl, sensorType, parser, (MessageWriter<JSONObject>)writer);
        }
        else {
          throw new IllegalStateException("Unable to create parser bolt: writer must be a MessageWriter or a BulkMessageWriter");
        }
      }
    }
    builder.setBolt("parserBolt", parserBolt, parserParallelism)
           .setNumTasks(parserNumTasks)
           .shuffleGrouping("kafkaSpout");
    return builder;
  }

}
