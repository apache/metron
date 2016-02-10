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

package org.apache.metron.enrichment.adapters.whois;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;

import com.google.common.base.Joiner;
import org.apache.metron.tldextractor.BasicTldExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhoisHBaseAdapter implements EnrichmentAdapter<String>,
				Serializable {

	protected static final Logger LOG = LoggerFactory
					.getLogger(WhoisHBaseAdapter.class);
	private static final long serialVersionUID = 3371873619030870389L;
	private HTableInterface table;
	private String _table_name;
	private String _quorum;
	private String _port;
	private BasicTldExtractor tldex = new BasicTldExtractor();

	public WhoisHBaseAdapter(String table_name, String quorum, String port) {
		_table_name = table_name;
		_quorum = quorum;
		_port = port;
	}

	public boolean initializeAdapter() {
		Configuration conf = null;
		conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", _quorum);
		conf.set("hbase.zookeeper.property.clientPort", _port);
		conf.set("zookeeper.session.timeout", "20");
		conf.set("hbase.rpc.timeout", "20");
		conf.set("zookeeper.recovery.retry", "1");
		conf.set("zookeeper.recovery.retry.intervalmill", "1");

		try {

			LOG.trace("[Metron] Connecting to HBase");
			LOG.trace("[Metron] ZOOKEEPER = "
					+ conf.get("hbase.zookeeper.quorum"));

			LOG.trace("[Metron] CONNECTING TO HBASE WITH: " + conf);

			HConnection connection = HConnectionManager.createConnection(conf);

			LOG.trace("[Metron] CONNECTED TO HBASE");

			table = connection.getTable(_table_name);

			LOG.trace("--------CONNECTED TO TABLE: " + table);

			JSONObject tester = enrich("cisco.com");

			if (tester.keySet().size() == 0)
				throw new IOException(
						"Either HBASE is misconfigured or whois table is missing");

			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;

	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public JSONObject enrich(String metadataIn) {
		
		String metadata = tldex.extract2LD(metadataIn);

		LOG.trace("[Metron] Pinging HBase For:" + metadata);

        
		JSONObject output = new JSONObject();
		JSONObject payload = new JSONObject();

		Get get = new Get(metadata.getBytes());
		Result rs;

		try {
			rs = table.get(get);

			for (KeyValue kv : rs.raw())
				payload.put(metadata, new String(kv.getValue()));

			output.put("whois", payload);

		} catch (IOException e) {
			payload.put(metadata, "{}");
			output.put("whois", payload);
			e.printStackTrace();
		}

		return output;

	}

	@Override
	public void cleanup() {

	}

	//	private String format(String input) {
//		String output = input;
//		String[] tokens = input.split("\\.");
//		if(tokens.length > 2) {
//			output = Joiner.on(".").join(Arrays.copyOfRange(tokens, 1, tokens.length));;
//		}
//		return output;
//	}

}
