/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opensoc.tagging;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.opensoc.alerts.interfaces.TaggerAdapter;
import com.opensoc.json.serialization.JSONEncoderHelper;
import com.opensoc.metrics.MetricReporter;

@SuppressWarnings("rawtypes")
public class TelemetryTaggerBolt extends AbstractTaggerBolt {

	/**
	 * Use an adapter to tag existing telemetry messages with alerts. The list
	 * of available tagger adapters is located under
	 * com.opensoc.tagging.adapters. At the time of the release the following
	 * adapters are available:
	 * 
	 * <p>
	 * <ul>
	 * <li>RegexTagger = read a list or regular expressions and tag a message if
	 * they exist in a message
	 * <li>StaticAllTagger = tag each message with a static alert
	 * <ul>
	 * <p>
	 */
	private static final long serialVersionUID = -2647123143398352020L;
	private Properties metricProperties;
	private JSONObject metricConfiguration;

	/**
	 * 
	 * @param tagger
	 *            - tagger adapter for generating alert tags
	 * @return instance of bolt
	 */
	public TelemetryTaggerBolt withMessageTagger(TaggerAdapter tagger) {
		_adapter = tagger;
		return this;
	}

	/**
	 * 
	 * @param OutputFieldName
	 *            - output name of the tuple coming out of this bolt
	 * @return - instance of this bolt
	 */
	public TelemetryTaggerBolt withOutputFieldName(String OutputFieldName) {
		this.OutputFieldName = OutputFieldName;
		return this;
	}

	/**
	 * 
	 * @param metricProperties
	 *            - metric output to graphite
	 * @return - instance of this bolt
	 */
	public TelemetryTaggerBolt withMetricProperties(Properties metricProperties) {
		this.metricProperties = metricProperties;
		return this;
	}

	/**
	 * 
	 * @param identifier
	 *            - the identifier tag for tagging telemetry messages with
	 *            alerts out of this bolt
	 * @return - instance of this bolt
	 */

	public TelemetryTaggerBolt withIdentifier(JSONObject identifier) {
		this._identifier = identifier;
		return this;
	}
	
	/**
	 * @param config
	 *            A class for generating custom metrics into graphite
	 * @return Instance of this class
	 */

	public TelemetryTaggerBolt withMetricConfiguration(Configuration config) {
		this.metricConfiguration = JSONEncoderHelper.getJSON(config
				.subset("com.opensoc.metrics"));
		return this;
	}

	@Override
	void doPrepare(Map conf, TopologyContext topologyContext,
			OutputCollector collector) throws IOException {

		LOG.info("[OpenSOC] Preparing TelemetryParser Bolt...");

		try {
			_reporter = new MetricReporter();
			_reporter.initialize(metricProperties, TelemetryTaggerBolt.class);
			LOG.info("[OpenSOC] Initialized metrics");
		} catch (Exception e) {
			LOG.info("[OpenSOC] Could not initialize metrics");
		}
	}

	@SuppressWarnings("unchecked")
	public void execute(Tuple tuple) {

		LOG.trace("[OpenSOC] Starting to process message for alerts");
		JSONObject original_message = null;

		try {

			original_message = (JSONObject) tuple.getValue(0);

			if (original_message == null || original_message.isEmpty())
				throw new Exception("Could not parse message from byte stream");

			LOG.trace("[OpenSOC] Received tuple: " + original_message);

			JSONObject alerts_tag = new JSONObject();
			JSONArray alerts_list = _adapter.tag(original_message);

			LOG.trace("[OpenSOC] Tagged message: " + alerts_list);

			if (alerts_list.size() != 0) {
				if (original_message.containsKey("alerts")) {
					JSONObject tag = (JSONObject) original_message
							.get("alerts");
					JSONArray already_triggered = (JSONArray) tag
							.get("triggered");
					alerts_list.addAll(already_triggered);
					LOG.trace("[OpenSOC] Created a new string of alerts");
				}

				alerts_tag.put("identifier", _identifier);
				alerts_tag.put("triggered", alerts_list);
				original_message.put("alerts", alerts_tag);
				
				LOG.debug("[OpenSOC] Detected alerts: " + alerts_tag);
			}
			else
			{
				LOG.debug("[OpenSOC] The following messages did not contain alerts: " + original_message);
			}

			_collector.ack(tuple);
			_collector.emit(new Values(original_message));
			
			/*if (metricConfiguration != null) {
				emitCounter.inc();
				ackCounter.inc();
			}*/

		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("Failed to tag message :" + original_message);
			e.printStackTrace();
			_collector.fail(tuple);
			
			/*
			if (metricConfiguration != null) {
				failCounter.inc();
			}*/
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declearer) {
		declearer.declare(new Fields(this.OutputFieldName));

	}
}