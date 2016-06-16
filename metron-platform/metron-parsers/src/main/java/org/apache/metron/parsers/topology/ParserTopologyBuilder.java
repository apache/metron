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
import org.apache.metron.common.Constants;
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
import org.apache.metron.parsers.bolt.WriterBolt;
import org.apache.metron.parsers.bolt.WriterHandler;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.writer.KafkaWriter;
import org.json.simple.JSONObject;
import storm.kafka.KafkaSpout;
import storm.kafka.ZkHosts;

import java.util.EnumMap;
import java.util.Map;

public class ParserTopologyBuilder {

  public static TopologyBuilder build(String zookeeperUrl,
                         String brokerUrl,
                         String sensorType,
                         SpoutConfig.Offset offset,
                         int spoutParallelism,
                         int spoutNumTasks,
                         int parserParallelism,
                         int parserNumTasks,
                         int invalidWriterParallelism,
                         int invalidWriterNumTasks,
                         int errorWriterParallelism,
                         int errorWriterNumTasks
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
    KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);
    builder.setSpout("kafkaSpout", kafkaSpout, spoutParallelism)
           .setNumTasks(spoutNumTasks);
    MessageParser<JSONObject> parser = ReflectionUtils.createInstance(sensorParserConfig.getParserClassName());
    parser.configure(sensorParserConfig.getParserConfig());

    ParserBolt parserBolt = new ParserBolt(zookeeperUrl
                                          , sensorType
                                          , parser
                                          ,getHandler( sensorType
                                                     , configurations
                                                     , sensorParserConfig.getWriterClassName() == null
                                                     ? new KafkaWriter(brokerUrl).withTopic(Constants.ENRICHMENT_TOPIC)
                                                     :ReflectionUtils.createInstance(sensorParserConfig.getWriterClassName())
                                                     )
                                          );

    WriterBolt errorBolt = new WriterBolt(getHandler( sensorType
                                                    , configurations
                                                    , sensorParserConfig.getErrorWriterClassName() == null
                                                    ? new KafkaWriter(brokerUrl).withTopic(Constants.DEFAULT_PARSER_ERROR_TOPIC)
                                                                                .withConfigPrefix("error")
                                                    :ReflectionUtils.createInstance(sensorParserConfig.getWriterClassName())
                                                    )
                                         , configurations
                                         , sensorType
                                         );
    WriterBolt invalidBolt = new WriterBolt(getHandler( sensorType
                                                    , configurations
                                                    , sensorParserConfig.getErrorWriterClassName() == null
                                                    ? new KafkaWriter(brokerUrl).withTopic(Constants.DEFAULT_PARSER_INVALID_TOPIC)
                                                                                .withConfigPrefix("invalid")
                                                    :ReflectionUtils.createInstance(sensorParserConfig.getWriterClassName())
                                                    )
                                         , configurations
                                         , sensorType
                                         );

    builder.setBolt("parserBolt", parserBolt, parserParallelism)
           .setNumTasks(parserNumTasks)
           .shuffleGrouping("kafkaSpout");
    if(errorWriterNumTasks > 0) {
      builder.setBolt("errorMessageWriter", errorBolt, errorWriterParallelism)
              .setNumTasks(errorWriterNumTasks)
              .shuffleGrouping("parserBolt", Constants.ERROR_STREAM)
      ;
    }
    if(invalidWriterNumTasks > 0) {
      builder.setBolt("invalidMessageWriter", invalidBolt, invalidWriterParallelism)
              .setNumTasks(invalidWriterNumTasks)
              .shuffleGrouping("parserBolt", Constants.INVALID_STREAM)
      ;
    }
    return builder;
  }

  private static WriterHandler getHandler(String sensorType, ParserConfigurations configurations, AbstractWriter writer) {
    writer.configure(sensorType, new ParserWriterConfiguration(configurations));
    if(writer instanceof BulkMessageWriter) {
      return new WriterHandler((BulkMessageWriter<JSONObject>)writer);
    }
    else if(writer instanceof MessageWriter) {
      return new WriterHandler((MessageWriter<JSONObject>)writer);
    }
    else {
      throw new IllegalStateException("Unable to create parser bolt: writer must be a MessageWriter or a BulkMessageWriter");
    }
  }

}
