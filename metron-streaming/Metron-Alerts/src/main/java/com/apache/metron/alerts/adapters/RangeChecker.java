package com.opensoc.alerts.adapters;

import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;

public class RangeChecker {

	static boolean checkRange(Set<String> CIDR_networks, String ip) {
		for (String network : CIDR_networks) {
				
			System.out.println("Looking at range: " + network + " and ip " + ip);
			SubnetUtils utils = new SubnetUtils(network);
			if(utils.getInfo().isInRange(ip)) {
				System.out.println(ip + " in range " + network);
				return true;
			}
		}
		
		//no matches
		return false;
	}
}
