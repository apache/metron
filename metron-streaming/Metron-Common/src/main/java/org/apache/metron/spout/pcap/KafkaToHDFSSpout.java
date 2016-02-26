package org.apache.metron.spout.pcap;

import storm.kafka.Callback;
import storm.kafka.CallbackKafkaSpout;
import storm.kafka.KeyValueSchemeAsMultiScheme;
import storm.kafka.SpoutConfig;

import java.util.List;

public class KafkaToHDFSSpout extends CallbackKafkaSpout {
    static final long serialVersionUID = 0xDEADBEEFL;
    HDFSWriterConfig config = null;
    public KafkaToHDFSSpout(SpoutConfig spoutConfig, HDFSWriterConfig config) {
        super(setScheme(spoutConfig, config), HDFSWriterCallback.class);
        this.config = config;
    }

    @Override
    protected Callback createCallback(Class<? extends Callback> callbackClass) {
        return new HDFSWriterCallback().withConfig(config);
    }

    private static SpoutConfig setScheme(SpoutConfig spoutConfig, HDFSWriterConfig config) {
        spoutConfig.scheme = new KeyValueSchemeAsMultiScheme(new TimestampedPacketScheme());
        List<String> zkHosts = config.getZookeeperServers();
        Integer zkPort = config.getZookeeperPort();
        if(zkPort != null && zkHosts.size() > 0) {
            spoutConfig.zkServers = zkHosts;
            spoutConfig.zkPort = zkPort;
        }
        spoutConfig.ignoreZkOffsets = true;
        spoutConfig.startOffsetTime = kafka.api.OffsetRequest.EarliestTime();
        return spoutConfig;
    }
}
