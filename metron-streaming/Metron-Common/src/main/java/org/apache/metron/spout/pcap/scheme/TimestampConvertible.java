package org.apache.metron.spout.pcap.scheme;

public interface TimestampConvertible {
  TimestampConvertible withTimestampConverter(TimestampConverter converter);
}
