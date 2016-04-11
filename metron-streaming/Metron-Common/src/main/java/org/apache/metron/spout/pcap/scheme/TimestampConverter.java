package org.apache.metron.spout.pcap.scheme;

import java.io.Serializable;

public interface TimestampConverter extends Serializable {
  public long toNanoseconds(long input);
}
