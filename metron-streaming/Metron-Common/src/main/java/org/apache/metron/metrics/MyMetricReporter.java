package org.apache.metron.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.GraphiteReporter;

public class MyMetricReporter extends MetricReporter {
	
	final MetricRegistry metrics = new MetricRegistry();
	private ConsoleReporter consoleReporter = null;
	private JmxReporter jmxReporter=null; 
	private GraphiteReporter graphiteReporter = null;

	
	public MyMetricReporter(boolean withConsole, boolean withJMX, boolean witGraphite)
	{
		consoleReporter = ConsoleReporter.forRegistry(metrics).build();
		jmxReporter = JmxReporter.forRegistry(metrics).build();
		graphiteReporter = GraphiteReporter.forRegistry(metrics).build(null);
	}
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public void report() {

	}

}
