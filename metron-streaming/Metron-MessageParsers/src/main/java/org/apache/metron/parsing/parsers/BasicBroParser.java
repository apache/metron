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

package org.apache.metron.parsing.parsers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.metron.tldextractor.BasicTldExtractor;

@SuppressWarnings("serial")
public class BasicBroParser extends AbstractParser {

	protected static final Logger _LOG = LoggerFactory
			.getLogger(BasicBroParser.class);
	private JSONCleaner cleaner = new JSONCleaner();
	private BasicTldExtractor tldex = new BasicTldExtractor();

	@SuppressWarnings("unchecked")
	public JSONObject parse(byte[] msg) {

		_LOG.trace("[Metron] Starting to parse incoming message");

		String raw_message = null;

		try {

			raw_message = new String(msg, "UTF-8");
			_LOG.trace("[Metron] Received message: " + raw_message);

			JSONObject cleaned_message = cleaner.Clean(raw_message);
			_LOG.debug("[Metron] Cleaned message: " + raw_message);

			if (cleaned_message == null || cleaned_message.isEmpty())
				throw new Exception("Unable to clean message: " + raw_message);

			String key = cleaned_message.keySet().iterator().next().toString();

			if (key == null)
				throw new Exception("Unable to retrieve key for message: "
						+ raw_message);

			JSONObject payload = (JSONObject) cleaned_message.get(key);

			String originalString = " |";
			for (Object k : payload.keySet()) {
				originalString = originalString + " " + k.toString() + ":"
						+ payload.get(k).toString();
			}
			originalString = key.toUpperCase() + originalString;
			payload.put("original_string", originalString);

			if (payload == null)
				throw new Exception("Unable to retrieve payload for message: "
						+ raw_message);

			if (payload.containsKey("ts")) {
				String ts = payload.remove("ts").toString();
				payload.put("timestamp", ts);
				_LOG.trace("[Metron] Added ts to: " + payload);
			}

			if (payload.containsKey("id.orig_h")) {
				String source_ip = payload.remove("id.orig_h").toString();
				payload.put("ip_src_addr", source_ip);
				_LOG.trace("[Metron] Added ip_src_addr to: " + payload);
			} else if (payload.containsKey("tx_hosts")) {
				JSONArray txHosts = (JSONArray) payload.remove("tx_hosts");
				if (txHosts != null && !txHosts.isEmpty()) {
					payload.put("ip_src_addr", txHosts.get(0));
					_LOG.trace("[Metron] Added ip_src_addr to: " + payload);
				}
			}
			
			if (payload.containsKey("id.resp_h")) {
				String source_ip = payload.remove("id.resp_h").toString();
				payload.put("ip_dst_addr", source_ip);
				_LOG.trace("[Metron] Added ip_dst_addr to: " + payload);
			} else if (payload.containsKey("rx_hosts")) {
				JSONArray rxHosts = (JSONArray) payload.remove("rx_hosts");
				if (rxHosts != null && !rxHosts.isEmpty()) {
					payload.put("ip_dst_addr", rxHosts.get(0));
					_LOG.trace("[Metron] Added ip_dst_addr to: " + payload);
				}
			}
			
			if (payload.containsKey("id.orig_p")) {
				String source_port = payload.remove("id.orig_p").toString();
				payload.put("ip_src_port", source_port);
				_LOG.trace("[Metron] Added ip_src_port to: " + payload);
			}
			if (payload.containsKey("id.resp_p")) {
				String dest_port = payload.remove("id.resp_p").toString();
				payload.put("ip_dst_port", dest_port);
				_LOG.trace("[Metron] Added ip_dst_port to: " + payload);
			}
			
//			if (payload.containsKey("host")) {
//
//				String host = payload.get("host").toString().trim();
//				String tld = tldex.extractTLD(host);
//
//				payload.put("tld", tld);
//				_LOG.trace("[Metron] Added tld to: " + payload);
//
//			}
//			if (payload.containsKey("query")) {
//				String host = payload.get("query").toString();
//				String[] parts = host.split("\\.");
//				int length = parts.length;
//				if (length >= 2) {
//					payload.put("tld", parts[length - 2] + "."
//							+ parts[length - 1]);
//					_LOG.trace("[Metron] Added tld to: " + payload);
//				}
//			}

			_LOG.trace("[Metron] Inner message: " + payload);

			payload.put("protocol", key);
			_LOG.debug("[Metron] Returning parsed message: " + payload);

			return payload;

		} catch (Exception e) {

			_LOG.error("Unable to Parse Message: " + raw_message);
			e.printStackTrace();
			return null;
		}

	}

	
}
