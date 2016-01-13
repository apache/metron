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

package org.apache.metron.enrichment.common;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.helpers.topology.ErrorGenerator;
import org.apache.metron.json.serialization.JSONEncoderHelper;
import org.apache.metron.metrics.MetricReporter;

/**
 * Uses an adapter to enrich telemetry messages with additional metadata
 * entries. For a list of available enrichment adapters see
 * org.apache.metron.enrichment.adapters.
 * <p>
 * At the moment of release the following enrichment adapters are available:
 * <p>
 * <ul>
 * 
 * <li>geo = attaches geo coordinates to IPs
 * <li>whois = attaches whois information to domains
 * <li>host = attaches reputation information to known hosts
 * <li>CIF = attaches information from threat intelligence feeds
 * <ul>
 * <p>
 * <p>
 * Enrichments are optional
 **/

@SuppressWarnings({ "rawtypes", "serial" })
public class GenericEnrichmentBolt extends AbstractEnrichmentBolt {

	private static final Logger LOG = LoggerFactory
			.getLogger(GenericEnrichmentBolt.class);
	private JSONObject metricConfiguration;

	/**
	 * @param adapter
	 *            Adapter for doing the enrichment
	 * @return Instance of this class
	 */

	public GenericEnrichmentBolt withAdapter(EnrichmentAdapter adapter) {
		_adapter = adapter;
		return this;
	}

	/**
	 * @param OutputFieldName
	 *            Fieldname of the output tuple for this bolt
	 * @return Instance of this class
	 */

	public GenericEnrichmentBolt withOutputFieldName(String OutputFieldName) {
		_OutputFieldName = OutputFieldName;
		return this;
	}

	/**
	 * @param EnrichmentTag
	 *            Defines what tag the enrichment will be tagged with in the
	 *            telemetry message
	 * @return Instance of this class
	 */

	public GenericEnrichmentBolt withEnrichmentTag(String EnrichmentTag) {
		_enrichment_tag = EnrichmentTag;
		return this;
	}

	/**
	 * @param MAX_CACHE_SIZE_OBJECTS_NUM
	 *            Maximum size of cache before flushing
	 * @return Instance of this class
	 */

	public GenericEnrichmentBolt withMaxCacheSize(long MAX_CACHE_SIZE_OBJECTS_NUM) {
		_MAX_CACHE_SIZE_OBJECTS_NUM = MAX_CACHE_SIZE_OBJECTS_NUM;
		return this;
	}

	/**
	 * @param MAX_TIME_RETAIN_MINUTES
	 *            Maximum time to retain cached entry before expiring
	 * @return Instance of this class
	 */

	public GenericEnrichmentBolt withMaxTimeRetain(long MAX_TIME_RETAIN_MINUTES) {
		_MAX_TIME_RETAIN_MINUTES = MAX_TIME_RETAIN_MINUTES;
		return this;
	}

	/**
	 * @param jsonKeys
	 *            Keys in the telemetry message that are to be enriched by this
	 *            bolt
	 * @return Instance of this class
	 */

	public GenericEnrichmentBolt withKeys(List<String> jsonKeys) {
		_jsonKeys = jsonKeys;
		return this;
	}

	/**
	 * @param config
	 *            A class for generating custom metrics into graphite
	 * @return Instance of this class
	 */

	public GenericEnrichmentBolt withMetricConfiguration(Configuration config) {
		this.metricConfiguration = JSONEncoderHelper.getJSON(config
				.subset("org.apache.metron.metrics"));
		return this;
	}

	@SuppressWarnings("unchecked")
	public void execute(Tuple tuple) {

		LOG.trace("[Metron] Starting enrichment");

		JSONObject in_json = null;
		String key = null;
		
		try {

			key = tuple.getStringByField("key");
			in_json = (JSONObject) tuple.getValueByField("message");

			if (in_json == null || in_json.isEmpty())
				throw new Exception("Could not parse binary stream to JSON");
			
			if(key == null)
				throw new Exception("Key is not valid");

			LOG.trace("[Metron] Received tuple: " + in_json);

			JSONObject message = (JSONObject) in_json.get("message");

			if (message == null || message.isEmpty())
				throw new Exception("Could not extract message from JSON: "
						+ in_json);

			LOG.trace("[Metron] Extracted message: " + message);

			for (String jsonkey : _jsonKeys) {
				LOG.trace("[Metron] Processing:" + jsonkey + " within:"
						+ message);

				String jsonvalue = (String) message.get(jsonkey);
				LOG.trace("[Metron] Processing: " + jsonkey + " -> "
						+ jsonvalue);

				if (null == jsonvalue) {
					LOG.trace("[Metron] Key " + jsonkey
							+ "not present in message " + message);
					continue;
				}
				
				// If the field is empty, no need to enrich
				if ( jsonvalue.length() == 0) {
					continue;
				}

				JSONObject enrichment = cache.getUnchecked(jsonvalue);
				LOG.trace("[Metron] Enriched: " + jsonkey + " -> "
						+ enrichment);

				if (enrichment == null)
					throw new Exception("[Metron] Could not enrich string: "
							+ jsonvalue);

				if (!in_json.containsKey("enrichment")) {
					in_json.put("enrichment", new JSONObject());
					LOG.trace("[Metron] Starting a string of enrichments");
				}

				JSONObject enr1 = (JSONObject) in_json.get("enrichment");

				if (enr1 == null)
					throw new Exception("Internal enrichment is empty");

				if (!enr1.containsKey(_enrichment_tag)) {
					enr1.put(_enrichment_tag, new JSONObject());
					LOG.trace("[Metron] Starting a new enrichment");
				}

				LOG.trace("[Metron] ENR1 is: " + enr1);

				JSONObject enr2 = (JSONObject) enr1.get(_enrichment_tag);
				enr2.put(jsonkey, enrichment);

				LOG.trace("[Metron] ENR2 is: " + enr2);

				enr1.put(_enrichment_tag, enr2);
				in_json.put("enrichment", enr1);
			}

			LOG.debug("[Metron] Generated combined enrichment: " + in_json);

			_collector.emit("message", new Values(key, in_json));
			_collector.ack(tuple);

			if (_reporter != null) {
				emitCounter.inc();
				ackCounter.inc();
			}
		} catch (Exception e) {
			
			LOG.error("[Metron] Unable to enrich message: " + in_json);
			_collector.fail(tuple);

			if (_reporter != null) {
				failCounter.inc();
			}
			
			JSONObject error = ErrorGenerator.generateErrorMessage("Enrichment problem: " + in_json, e);
			_collector.emit("error", new Values(error));
		}
		
		

	}

	public void declareOutputFields(OutputFieldsDeclarer declearer) {
		declearer.declareStream("message", new Fields("key", "message"));
		declearer.declareStream("error", new Fields("message"));
	}

	@Override
	void doPrepare(Map conf, TopologyContext topologyContext,
			OutputCollector collector) throws IOException {
		LOG.info("[Metron] Preparing Enrichment Bolt...");

		_collector = collector;

		try {
			_reporter = new MetricReporter();
			_reporter.initialize(metricConfiguration,
					GenericEnrichmentBolt.class);
			this.registerCounters();
		} catch (Exception e) {
			LOG.info("[Metron] Unable to initialize metrics reporting");
		}

		LOG.info("[Metron] Enrichment bolt initialized...");
	}

}