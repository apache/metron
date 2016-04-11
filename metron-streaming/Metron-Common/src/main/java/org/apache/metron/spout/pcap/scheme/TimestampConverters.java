package org.apache.metron.spout.pcap.scheme;


public enum TimestampConverters {
  MILLISECONDS(tsMilli -> tsMilli*1000000L)
  ,MICROSECONDS(tsMicro -> tsMicro*1000L)
  ,NANOSECONDS(tsNano -> tsNano);
  TimestampConverter converter;
  TimestampConverters(TimestampConverter converter) {
    this.converter = converter;
  }

  public static TimestampConverter getConverter(String converter) {
    return TimestampConverters.valueOf(converter).converter;
  }
}
