package com.opensoc.topology;

import org.apache.commons.configuration.ConfigurationException;
import backtype.storm.generated.InvalidTopologyException;
import com.opensoc.topology.runner.FireEyeRunner;
import com.opensoc.topology.runner.TopologyRunner;


/**
 * Topology for processing FireEye syslog messages
 *
 */
public class FireEye {

	public static void main(String[] args) throws ConfigurationException, Exception, InvalidTopologyException {
		
		TopologyRunner runner = new FireEyeRunner();
		runner.initTopology(args, "fireeye");
	}
	
}
