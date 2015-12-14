package com.opensoc.topology.runner;

import com.opensoc.filters.GenericMessageFilter;
import com.opensoc.parser.interfaces.MessageParser;
import com.opensoc.parsing.AbstractParserBolt;
import com.opensoc.parsing.TelemetryParserBolt;
import com.opensoc.test.spouts.GenericInternalTestSpout;

public class FireEyeRunner extends TopologyRunner{
	
	 static String test_file_path = "SampleInput/FireeyeExampleOutput";

	@Override
	public boolean initializeParsingBolt(String topology_name,
			String name) {
		try {
			
			String messageUpstreamComponent = messageComponents.get(messageComponents.size()-1);
			
			System.out.println("[OpenSOC] ------" +  name + " is initializing from " + messageUpstreamComponent);

			
			String class_name = config.getString("bolt.parser.adapter");
			
			if(class_name == null)
			{
				System.out.println("[OpenSOC] Parser adapter not set.  Please set bolt.indexing.adapter in topology.conf");
				throw new Exception("Parser adapter not set");
			}
			
			Class loaded_class = Class.forName(class_name);
			MessageParser parser = (MessageParser) loaded_class.newInstance();
	        
	        
			AbstractParserBolt parser_bolt = new TelemetryParserBolt()
					.withMessageParser(parser)
					.withOutputFieldName(topology_name)
					.withMessageFilter(new GenericMessageFilter())
					.withMetricConfig(config);

			builder.setBolt(name, parser_bolt,
					config.getInt("bolt.parser.parallelism.hint"))
					.shuffleGrouping(messageUpstreamComponent)
					.setNumTasks(config.getInt("bolt.parser.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return true;
	}

	@Override	
	public  boolean initializeTestingSpout(String name) {
		try {

			System.out.println("[OpenSOC] Initializing Test Spout");

			GenericInternalTestSpout testSpout = new GenericInternalTestSpout()
					.withFilename(test_file_path).withRepeating(
							config.getBoolean("spout.test.parallelism.repeat"));

			builder.setSpout(name, testSpout,
					config.getInt("spout.test.parallelism.hint")).setNumTasks(
					config.getInt("spout.test.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return true;
	}
	
	

}