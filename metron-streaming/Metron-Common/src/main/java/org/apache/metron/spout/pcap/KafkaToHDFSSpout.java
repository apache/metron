package org.apache.metron.spout.pcap;

import storm.kafka.Callback;
import storm.kafka.CallbackKafkaSpout;
import storm.kafka.KeyValueSchemeAsMultiScheme;
import storm.kafka.SpoutConfig;

public class KafkaToHDFSSpout extends CallbackKafkaSpout {
    static final long serialVersionUID = 0xDEADBEEFL;
    HDFSWriterConfig config;
    public KafkaToHDFSSpout(SpoutConfig spoutConfig, HDFSWriterConfig config) {
        super(setScheme(spoutConfig), HDFSWriterCallback.class);
        this.config = config;
    }

    @Override
    protected Callback createCallback(Class<? extends Callback> callbackClass) {
        return new HDFSWriterCallback().withConfig(config);
    }

    private static SpoutConfig setScheme(SpoutConfig spoutConfig) {
        spoutConfig.scheme = new KeyValueSchemeAsMultiScheme(new TimestampedPacketScheme());
        return spoutConfig;
    }
}
