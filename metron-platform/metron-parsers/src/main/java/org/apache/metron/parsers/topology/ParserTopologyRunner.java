package org.apache.metron.parsers.topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.Utils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.cli.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorEnrichmentConfig;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.parsers.bolt.ParserBolt;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.writer.KafkaWriter;
import org.json.simple.JSONObject;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;

import java.util.Map;

public class ParserTopologyRunner {

  public static void main(String[] args) {

  }

  public static void run(String zookeeperUrl, String brokerUrl, String sensorType, boolean fromBeginning, int spoutParallelism, int parserParallelism, boolean runLocally, Map<String, Object> stormConf) throws
          Exception {
    CuratorFramework client = ConfigurationsUtils.getClient(zookeeperUrl);
    SensorEnrichmentConfig sensorEnrichmentConfig = ConfigurationsUtils.readSensorEnrichmentConfigFromZookeeper(sensorType, client);
    String sensorTopic = sensorEnrichmentConfig.getSensorTopic() != null ? sensorEnrichmentConfig.getSensorTopic() : sensorType;
    TopologyBuilder builder = new TopologyBuilder();
    ZkHosts zkHosts = new ZkHosts(zookeeperUrl);
    SpoutConfig spoutConfig = new SpoutConfig(zkHosts, sensorTopic, "", sensorTopic);
    if (fromBeginning) {
      spoutConfig.ignoreZkOffsets = false;
      spoutConfig.startOffsetTime = -2;
    } else {
      spoutConfig.ignoreZkOffsets = true;
      spoutConfig.startOffsetTime = -1;
    }
    KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);
    builder.setSpout("kafkaSpout", kafkaSpout, spoutParallelism);
    MessageParser<JSONObject> parser = ReflectionUtils.createInstance(sensorEnrichmentConfig.getParserClassName());
    KafkaWriter writer = new KafkaWriter(brokerUrl);
    ParserBolt parserBolt = new ParserBolt(zookeeperUrl, sensorType, parser, writer);
    builder.setBolt("parserBolt", parserBolt, parserParallelism).shuffleGrouping("kafkaSpout");
    if (runLocally) {
      stormConf.put(Config.TOPOLOGY_DEBUG, true);
      LocalCluster cluster = new LocalCluster();
      cluster.submitTopology("parserTopology", stormConf, builder.createTopology());
      Utils.sleep(300000);
      cluster.shutdown();
    } else {
      StormSubmitter.submitTopology("parserTopology", stormConf, builder.createTopology());
    }
  }
}
