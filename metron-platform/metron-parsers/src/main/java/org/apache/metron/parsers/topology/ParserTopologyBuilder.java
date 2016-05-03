package org.apache.metron.parsers.topology;

import backtype.storm.topology.TopologyBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.cli.ConfigurationsUtils;
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
    SensorParserConfig sensorParserConfig = ConfigurationsUtils.readSensorParserConfigFromZookeeper(sensorType, client);
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
