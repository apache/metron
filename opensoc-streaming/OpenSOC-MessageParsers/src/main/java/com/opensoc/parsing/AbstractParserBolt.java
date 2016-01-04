/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opensoc.parsing;

import java.io.IOException;
import java.util.Map;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;

import com.codahale.metrics.Counter;
import com.opensoc.metrics.MetricReporter;
import com.opensoc.parser.interfaces.MessageFilter;
import com.opensoc.parser.interfaces.MessageParser;

@SuppressWarnings("rawtypes")
public abstract class AbstractParserBolt extends BaseRichBolt {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6710596708304282838L;

	protected static final Logger LOG = LoggerFactory
			.getLogger(AbstractParserBolt.class);

	protected OutputCollector _collector;
	protected MessageParser _parser;

	protected String OutputFieldName;
	protected MetricReporter _reporter;
	protected MessageFilter _filter;

	protected Counter ackCounter, emitCounter, failCounter;

	/**
	 * Register counters to be reported to graphite
	 * */

	protected void registerCounters() {

		String ackString = _parser.getClass().getSimpleName() + ".ack";

		String emitString = _parser.getClass().getSimpleName() + ".emit";

		String failString = _parser.getClass().getSimpleName() + ".fail";

		ackCounter = _reporter.registerCounter(ackString);
		emitCounter = _reporter.registerCounter(emitString);
		failCounter = _reporter.registerCounter(failString);

	}

	/**
	 * Check to make sure all required variables have been initialized
	 * */

	public final void prepare(Map conf, TopologyContext topologyContext,
			OutputCollector collector) {
		_collector = collector;
		if (this._parser == null)
			throw new IllegalStateException("MessageParser must be specified");
		if (this.OutputFieldName == null)
			throw new IllegalStateException("OutputFieldName must be specified");

		if (this._filter == null)
			throw new IllegalStateException("MessageFilter must be specified");

		try {
			doPrepare(conf, topologyContext, collector);
		} catch (IOException e) {
			LOG.error("Counld not initialize...");
			e.printStackTrace();
		}
	}

	/**
	 * @param parser
	 *            The parser class for parsing the incoming raw message byte
	 *            stream
	 * @return Instance of this class
	 */

	public boolean checkForSchemaCorrectness(JSONObject message) {
		int correct = 0;

		
		if (!(message.containsKey("original_string"))) {
			LOG.trace("[OpenSOC] Message does not have original_string: " + message);
			return false;
		} else if (!(message.containsKey("timestamp"))) { 
			LOG.trace("[OpenSOC] Message does not have timestamp: " + message);
			return false;
		} else {
			LOG.trace("[OpenSOC] Message conforms to schema: "
					+ message);
			return true;
		}
	}

	abstract void doPrepare(Map conf, TopologyContext topologyContext,
			OutputCollector collector) throws IOException;

	protected String generateTopologyKey(String src_ip, String dst_ip)
			throws Exception {
		try {
			if (dst_ip == null && src_ip == null)
				return "0";

			if (src_ip == null || src_ip.length() == 0)
				return dst_ip;

			if (dst_ip == null || dst_ip.length() == 0)
				return src_ip;

			double ip1 = Double.parseDouble(src_ip.replace(".", ""));
			double ip2 = Double.parseDouble(dst_ip.replace(".", ""));

			return String.valueOf(ip1 + ip2);
		} catch (Exception e) {
			return "0";
		}
	}

}