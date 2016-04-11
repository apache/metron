package org.apache.metron.spout.pcap.scheme;

public interface TimestampConverter {
  public long toNanoseconds(long input);
}
