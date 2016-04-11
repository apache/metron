package org.apache.metron.spout.pcap.scheme;

import backtype.storm.spout.MultiScheme;

public interface TimestampSchemeCreator {
  MultiScheme create(TimestampConverter converter);
}
