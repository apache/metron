/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.pcap;

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
