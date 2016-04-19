package org.apache.metron.spout.pcap.scheme;

import backtype.storm.spout.MultiScheme;
import org.apache.metron.spout.pcap.Endianness;

public interface TimestampSchemeCreator {
  MultiScheme create(TimestampConverter converter, Endianness endianness);
}
