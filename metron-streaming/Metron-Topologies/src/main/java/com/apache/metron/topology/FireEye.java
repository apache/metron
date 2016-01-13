package com.apache.metron.topology;

import org.apache.commons.configuration.ConfigurationException;
import backtype.storm.generated.InvalidTopologyException;
import com.apache.metron.topology.runner.FireEyeRunner;
import com.apache.metron.topology.runner.TopologyRunner;


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
