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

package org.apache.metron.enrichment.adapters.threat;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class ThreatHbaseAdapter implements EnrichmentAdapter<String>,
				Serializable {

	protected static final org.slf4j.Logger LOG = LoggerFactory
					.getLogger(ThreatHbaseAdapter.class);
	private static final long serialVersionUID = 1L;
	private String _tableName;
	private HTableInterface table;
	private String _quorum;
	private String _port;

	public ThreatHbaseAdapter(String quorum, String port, String tableName) {
		_quorum = quorum;
		_port = port;
		_tableName = tableName;
	}

	/** The LOGGER. */
	private static final Logger LOGGER = Logger
			.getLogger(ThreatHbaseAdapter.class);

	@Override
	public void logAccess(String value) {

	}

	public JSONObject enrich(String metadata) {

		JSONObject output = new JSONObject();
		LOGGER.debug("=======Looking Up For:" + metadata);
		output.putAll(getThreatObject(metadata));

		return output;
	}

	@SuppressWarnings({ "rawtypes", "deprecation" })
	protected Map getThreatObject(String key) {

		LOGGER.debug("=======Pinging HBase For:" + key);
		
		Get get = new Get(Bytes.toBytes(key));
		Result rs;
		Map output = new HashMap();

		try {
			rs = table.get(get);

			if (!rs.isEmpty()) {
				byte[] source_family = Bytes.toBytes("source");
				JSONParser parser = new JSONParser();
				
				Map<byte[], byte[]> sourceFamilyMap = rs.getFamilyMap(source_family);
				
				for (Map.Entry<byte[], byte[]> entry  : sourceFamilyMap.entrySet()) {
					String k = Bytes.toString(entry.getKey());
					LOGGER.debug("=======Found intel from source: " + k);
					output.put(k,parser.parse(Bytes.toString(entry.getValue())));
	            }
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}

	@Override
	public boolean initializeAdapter() {

		// Initialize HBase Table
		Configuration conf = null;
		conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", _quorum);
		conf.set("hbase.zookeeper.property.clientPort", _port);

		try {
			LOGGER.debug("=======Connecting to HBASE===========");
			LOGGER.debug("=======ZOOKEEPER = "
					+ conf.get("hbase.zookeeper.quorum"));
			HConnection connection = HConnectionManager.createConnection(conf);
			table = connection.getTable(_tableName);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOGGER.debug("=======Unable to Connect to HBASE===========");
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public void cleanup() {

	}
}
