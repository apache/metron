package com.opensoc.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class MetricReporter {

	final MetricRegistry metrics = new MetricRegistry();
	private ConsoleReporter consoleReporter = null;
	private JmxReporter jmxReporter = null;
	private GraphiteReporter graphiteReporter = null;

	private Class _klas;
	private String _topologyname = "topology";

	/** The Constant LOGGER. */
	private static final Logger _Logger = Logger
			.getLogger(MetricReporter.class);

	public void initialize(Map config, Class klas) {

		_Logger.debug("===========Initializing Reporter");
		this._klas = klas;
		if (config.get("topologyname")!=null)
			_topologyname = (String) config.get("topologyname");
			
		this.start(config);

	}

	public Counter registerCounter(String countername) {
		return metrics.counter(MetricRegistry.name(_topologyname,_klas.getCanonicalName(), countername));
	}

	public void start(Map config) {
		try {
			if (config.get("reporter.jmx").equals("true")) {
				jmxReporter = JmxReporter.forRegistry(metrics).build();
				jmxReporter.start();
			}

			if (config.get("reporter.console").equals("true")) {
				consoleReporter = ConsoleReporter.forRegistry(metrics).build();
				consoleReporter.start(1, TimeUnit.SECONDS);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if (config.get("reporter.graphite").equals("true")) {
				String address = (String) config.get("graphite.address");
				int port = Integer.parseInt((String) config
						.get("graphite.port"));

				_Logger.debug("===========Graphite ADDRESS: " + address + ":"
						+ port);

				Graphite graphite = new Graphite(new InetSocketAddress(address,
						port));
				// Check if graphite connectivity works
				graphite.connect();
				graphite.close();

				graphiteReporter = GraphiteReporter.forRegistry(metrics).build(
						graphite);

				_Logger.debug("---------******STARTING GRAPHITE*********---------");
				graphiteReporter.start(1, TimeUnit.SECONDS);
			}
		}

		catch (IOException io) {
			_Logger.warn("Unable to Connect to Graphite");
		}
	}
}
