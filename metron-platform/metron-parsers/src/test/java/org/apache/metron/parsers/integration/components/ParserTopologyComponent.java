package org.apache.metron.parsers.integration.components;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;
import org.apache.metron.common.spout.kafka.SpoutConfig;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.parsers.topology.ParserTopologyBuilder;

import java.util.HashMap;
import java.util.Map;

public class ParserTopologyComponent implements InMemoryComponent {

  private String zookeeperUrl;
  private String brokerUrl;
  private String sensorType;
  private LocalCluster stormCluster;

  public static class Builder {
    String zookeeperUrl;
    String brokerUrl;
    String sensorType;
    public Builder withZookeeperUrl(String zookeeperUrl) {
      this.zookeeperUrl = zookeeperUrl;
      return this;
    }
    public Builder withBrokerUrl(String brokerUrl) {
      this.brokerUrl = brokerUrl;
      return this;
    }
    public Builder withSensorType(String sensorType) {
      this.sensorType = sensorType;
      return this;
    }

    public ParserTopologyComponent build() {
      return new ParserTopologyComponent(zookeeperUrl, brokerUrl, sensorType);
    }
  }

  public ParserTopologyComponent(String zookeeperUrl, String brokerUrl, String sensorType) {
    this.zookeeperUrl = zookeeperUrl;
    this.brokerUrl = brokerUrl;
    this.sensorType = sensorType;
  }

  @Override
  public void start() throws UnableToStartException {
    try {
      TopologyBuilder topologyBuilder = ParserTopologyBuilder.build(zookeeperUrl, brokerUrl, sensorType, SpoutConfig.Offset.BEGINNING, 1, 1);
      Map<String, Object> stormConf = new HashMap<>();
      stormConf.put(Config.TOPOLOGY_DEBUG, true);
      stormCluster = new LocalCluster();
      stormCluster.submitTopology("parserTopology", stormConf, topologyBuilder.createTopology());
    } catch (Exception e) {
      throw new UnableToStartException("Unable to start parser topology for sensorType: " + sensorType, e);
    }
  }

  @Override
  public void stop() {
    stormCluster.shutdown();
  }
}
