package org.apache.metron.spout.pcap.scheme;

import backtype.storm.spout.MultiScheme;
import storm.kafka.KeyValueSchemeAsMultiScheme;

public enum TimestampScheme {
  FROM_KEY(new KeyValueSchemeAsMultiScheme(new FromKeyScheme()))
  ,FROM_PACKET(new FromPacketScheme());
  ;
  public static final String KV_FIELD = "kv";
  MultiScheme scheme;
  TimestampScheme(MultiScheme scheme)
  {
    this.scheme = scheme;
  }
  public MultiScheme getScheme() {
    return scheme;
  }

  public static MultiScheme getScheme(String scheme) {
    try {
      TimestampScheme ts = TimestampScheme.valueOf(scheme.toUpperCase());
      return ts.getScheme();
    }
    catch(IllegalArgumentException iae) {
      return TimestampScheme.FROM_KEY.getScheme();
    }
  }

}
