package org.apache.metron.spout.pcap.scheme;

import org.apache.metron.spout.pcap.Endianness;

public interface KeyConvertible {
  KeyConvertible withTimestampConverter(TimestampConverter converter);
  KeyConvertible withEndianness(Endianness endianness);
}
