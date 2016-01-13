package com.opensoc.pcap;

import java.util.Comparator;

import org.apache.log4j.Logger;

import org.krakenapps.pcap.packet.PcapPacket;

public class PcapPacketComparator implements Comparator<PcapPacket> {

	/** The Constant LOG. */
	private static final Logger LOG = Logger.getLogger(PcapMerger.class);
	
	public int compare(PcapPacket p1, PcapPacket p2) {

		Long p1time = new Long(p1.getPacketHeader().getTsSec()) * 1000000L + new Long(p1.getPacketHeader().getTsUsec());
		Long p2time = new Long(p2.getPacketHeader().getTsSec()) * 1000000L + new Long(p2.getPacketHeader().getTsUsec());
		Long delta = p1time - p2time;
		LOG.debug("p1time: " + p1time.toString() + " p2time: " + p2time.toString() + " delta: " + delta.toString());
		return delta.intValue();
	}
}
