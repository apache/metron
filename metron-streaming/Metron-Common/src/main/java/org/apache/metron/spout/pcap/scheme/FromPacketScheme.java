package org.apache.metron.spout.pcap.scheme;

import backtype.storm.spout.MultiScheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.google.common.collect.ImmutableList;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.log4j.Logger;
import org.apache.metron.pcap.PcapByteInputStream;
import org.apache.metron.spout.pcap.PcapHelper;
import org.krakenapps.pcap.file.GlobalHeader;
import org.krakenapps.pcap.packet.PacketHeader;
import org.krakenapps.pcap.packet.PcapPacket;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class FromPacketScheme implements MultiScheme {
  private static final Logger LOG = Logger.getLogger(FromPacketScheme.class);
  @Override
  public Iterable<List<Object>> deserialize(byte[] rawValue) {
    byte[] value = PcapHelper.headerizeIfNecessary(rawValue);
    Long ts = getTimestamp(value);
    if(ts != null) {
      return ImmutableList.of(new Values(ImmutableList.of(new LongWritable(ts), new BytesWritable(rawValue))));
    }
    else {
      return ImmutableList.of(new Values(Collections.EMPTY_LIST));
    }
  }

  @Override
  public Fields getOutputFields() {
    return new Fields(TimestampScheme.KV_FIELD);
  }
  public static Long getTimestamp(byte[] pcap) {
    PcapByteInputStream pcapByteInputStream = null;
    try {
      pcapByteInputStream = new PcapByteInputStream(pcap);
      PcapPacket packet = pcapByteInputStream.getPacket();
      GlobalHeader globalHeader = pcapByteInputStream.getGlobalHeader();
      PacketHeader packetHeader = packet.getPacketHeader();
      if ( globalHeader.getMagicNumber() == 0xa1b2c3d4 || globalHeader.getMagicNumber() == 0xd4c3b2a1 )
      {
        //Time is in micro assemble as nano
        LOG.info("Times are in micro according to the magic number");
        return packetHeader.getTsSec() * 1000000000L + packetHeader.getTsUsec() * 1000L ;
      }
      else if ( globalHeader.getMagicNumber() == 0xa1b23c4d || globalHeader.getMagicNumber() == 0x4d3cb2a1 ) {
        //Time is in nano assemble as nano
        LOG.info("Times are in nano according to the magic number");
        return packetHeader.getTsSec() * 1000000000L + packetHeader.getTsUsec() ;
      }
      //Default assume time is in micro assemble as nano
      LOG.warn("Unknown magic number. Defaulting to micro");
      return packetHeader.getTsSec() * 1000000000L + packetHeader.getTsUsec() * 1000L ;
    }
    catch(IOException ioe) {
      //we cannot read the packet, so we return null here.
      LOG.error("Unable to read packet", ioe);
    }
    finally {
      if(pcapByteInputStream != null) {
        try {
          pcapByteInputStream.close();
        } catch (IOException e) {
          LOG.error("Unable to close stream", e);
        }
      }
    }
    return null;
  }
}
