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

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;

import com.codahale.metrics.Counter;
import com.google.common.cache.Cache;
import org.apache.metron.alerts.interfaces.AlertsAdapter;
import org.apache.metron.metrics.MetricReporter;

@SuppressWarnings("rawtypes")
public abstract class AbstractAlertBolt extends BaseRichBolt {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6710596708304282838L;

	transient Cache<String, String> cache;

	protected static final Logger LOG = LoggerFactory
			.getLogger(AbstractAlertBolt.class);

	protected OutputCollector _collector;
	protected AlertsAdapter _adapter;

	protected String OutputFieldName;
	protected JSONObject _identifier;
	protected MetricReporter _reporter;

	protected int _MAX_CACHE_SIZE_OBJECTS_NUM = -1;
	protected int _MAX_TIME_RETAIN_MINUTES = -1;

	protected Counter ackCounter, emitCounter, failCounter;

	protected void registerCounters() {

		String ackString = _adapter.getClass().getSimpleName() + ".ack";

		String emitString = _adapter.getClass().getSimpleName() + ".emit";

		String failString = _adapter.getClass().getSimpleName() + ".fail";

		ackCounter = _reporter.registerCounter(ackString);
		emitCounter = _reporter.registerCounter(emitString);
		failCounter = _reporter.registerCounter(failString);

	}

	public final void prepare(Map conf, TopologyContext topologyContext,
			OutputCollector collector) {
		_collector = collector;

		if (this._adapter == null)
			throw new IllegalStateException("Alerts adapter must be specified");
		if (this._identifier == null)
			throw new IllegalStateException("Identifier must be specified");

		if (this._MAX_CACHE_SIZE_OBJECTS_NUM == -1)
			throw new IllegalStateException("MAX_CACHE_SIZE_OBJECTS_NUM must be specified");
		if (this._MAX_TIME_RETAIN_MINUTES == -1)
			throw new IllegalStateException("MAX_TIME_RETAIN_MINUTES must be specified");

		try {
			doPrepare(conf, topologyContext, collector);
		} catch (IOException e) {
			LOG.error("Counld not initialize...");
			e.printStackTrace();
		}

		boolean success = _adapter.initialize();
		
		try {
			if (!success)

				throw new Exception("Could not initialize adapter");
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declearer) {
		declearer.declareStream("message", new Fields("key", "message"));
		declearer.declareStream("alert", new Fields( "message"));
		declearer.declareStream("error", new Fields("message"));
	}

	abstract void doPrepare(Map conf, TopologyContext topologyContext,
			OutputCollector collector) throws IOException;

}
