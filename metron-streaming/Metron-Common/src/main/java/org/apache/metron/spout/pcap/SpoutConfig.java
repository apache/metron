package org.apache.metron.spout.pcap;

import org.apache.metron.spout.pcap.scheme.TimestampScheme;
import storm.kafka.BrokerHosts;

public class SpoutConfig extends org.apache.metron.spout.kafka.SpoutConfig{

  public SpoutConfig(BrokerHosts hosts, String topic, String zkRoot, String id) {
    super(hosts, topic, zkRoot, id);
  }

  public SpoutConfig withTimestampScheme(String scheme) {
    super.scheme = TimestampScheme.getScheme(scheme);
    return this;
  }
}
