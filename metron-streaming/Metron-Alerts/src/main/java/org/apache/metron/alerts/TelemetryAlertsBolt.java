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

package org.apache.metron.alerts;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.google.common.cache.CacheBuilder;
import org.apache.metron.alerts.interfaces.AlertsAdapter;
import org.apache.metron.helpers.topology.ErrorGenerator;
import org.apache.metron.json.serialization.JSONEncoderHelper;
import org.apache.metron.metrics.MetricReporter;

@SuppressWarnings("rawtypes")
public class TelemetryAlertsBolt extends AbstractAlertBolt {

	/**
	 * Use an adapter to tag existing telemetry messages with alerts. The list
	 * of available tagger adapters is located under
	 * org.apache.metron.tagging.adapters. At the time of the release the following
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

	// private AlertsCache suppressed_alerts;

	/**
	 * 
	 * @param tagger
	 *            - tagger adapter for generating alert tags
	 * @return instance of bolt
	 */
	public TelemetryAlertsBolt withAlertsAdapter(AlertsAdapter tagger) {
		_adapter = tagger;
		return this;
	}

	/**
	 * 
	 * @param OutputFieldName
	 *            - output name of the tuple coming out of this bolt
	 * @return - instance of this bolt
	 */
	public TelemetryAlertsBolt withOutputFieldName(String OutputFieldName) {
		this.OutputFieldName = OutputFieldName;
		return this;
	}

	/**
	 * 
	 * @param metricProperties
	 *            - metric output to graphite
	 * @return - instance of this bolt
	 */
	public TelemetryAlertsBolt withMetricProperties(Properties metricProperties) {
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

	public TelemetryAlertsBolt withIdentifier(JSONObject identifier) {
		this._identifier = identifier;
		return this;
	}

	/**
	 * @param config
	 *            A class for generating custom metrics into graphite
	 * @return Instance of this class
	 */

	public TelemetryAlertsBolt withMetricConfiguration(Configuration config) {
		this.metricConfiguration = JSONEncoderHelper.getJSON(config
				.subset("org.apache.metron.metrics"));
		return this;
	}

	/**
	 * @param MAX_CACHE_SIZE_OBJECTS_NUM
	 *            Maximum size of cache before flushing
	 * @return Instance of this class
	 */

	public TelemetryAlertsBolt withMaxCacheSize(int MAX_CACHE_SIZE_OBJECTS_NUM) {
		_MAX_CACHE_SIZE_OBJECTS_NUM = MAX_CACHE_SIZE_OBJECTS_NUM;
		return this;
	}

	/**
	 * @param MAX_TIME_RETAIN_MINUTES
	 *            Maximum time to retain cached entry before expiring
	 * @return Instance of this class
	 */

	public TelemetryAlertsBolt withMaxTimeRetain(int MAX_TIME_RETAIN_MINUTES) {
		_MAX_TIME_RETAIN_MINUTES = MAX_TIME_RETAIN_MINUTES;
		return this;
	}

	@Override
	void doPrepare(Map conf, TopologyContext topologyContext,
			OutputCollector collector) throws IOException {

		cache = CacheBuilder.newBuilder().maximumSize(_MAX_CACHE_SIZE_OBJECTS_NUM)
				.expireAfterWrite(_MAX_TIME_RETAIN_MINUTES, TimeUnit.MINUTES).build();

		LOG.info("[Metron] Preparing TelemetryAlert Bolt...");

		try {
			_reporter = new MetricReporter();
			_reporter.initialize(metricProperties, TelemetryAlertsBolt.class);
			LOG.info("[Metron] Initialized metrics");
		} catch (Exception e) {
			LOG.info("[Metron] Could not initialize metrics");
		}
	}

	@SuppressWarnings("unchecked")
	public void execute(Tuple tuple) {

		LOG.trace("[Metron] Starting to process message for alerts");
		JSONObject original_message = null;
		String key = null;

		try {

			key = tuple.getStringByField("key");
			original_message = (JSONObject) tuple.getValueByField("message");

			if (original_message == null || original_message.isEmpty())
				throw new Exception("Could not parse message from byte stream");
			
			if(key == null)
				throw new Exception("Key is not valid");
			
			LOG.trace("[Metron] Received tuple: " + original_message);

			JSONObject alerts_tag = new JSONObject();
			Map<String, JSONObject> alerts_list = _adapter
					.alert(original_message);
			JSONArray uuid_list = new JSONArray();

			if (alerts_list == null || alerts_list.isEmpty()) {
				System.out.println("[Metron] No alerts detected in: "
						+ original_message);
				_collector.ack(tuple);
				_collector.emit("message", new Values(key, original_message));
			} else {
				for (String alert : alerts_list.keySet()) {
					uuid_list.add(alert);

					LOG.trace("[Metron] Checking alerts cache: " + alert);

					if (cache.getIfPresent(alert) == null) {
						System.out.println("[Metron]: Alert not found in cache: " + alert);

						JSONObject global_alert = new JSONObject();
						global_alert.putAll(_identifier);
						global_alert.putAll(alerts_list.get(alert));
						global_alert.put("timestamp", System.currentTimeMillis());
						_collector.emit("alert", new Values(global_alert));

						cache.put(alert, "");

					} else
						LOG.trace("Alert located in cache: " + alert);

					LOG.debug("[Metron] Alerts are: " + alerts_list);

					if (original_message.containsKey("alerts")) {
						JSONArray already_triggered = (JSONArray) original_message
								.get("alerts");

						uuid_list.addAll(already_triggered);
						LOG.trace("[Metron] Messages already had alerts...tagging more");
					}

					original_message.put("alerts", uuid_list);

					LOG.debug("[Metron] Detected alerts: " + alerts_tag);

					_collector.ack(tuple);
					_collector.emit("message", new Values(key, original_message));

				}

				/*
				 * if (metricConfiguration != null) { emitCounter.inc();
				 * ackCounter.inc(); }
				 */
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("Failed to tag message :" + original_message);
			e.printStackTrace();
			_collector.fail(tuple);

			/*
			 * if (metricConfiguration != null) { failCounter.inc(); }
			 */


			JSONObject error = ErrorGenerator.generateErrorMessage(
					"Alerts problem: " + original_message, e);
			_collector.emit("error", new Values(error));
		}
	}

}
