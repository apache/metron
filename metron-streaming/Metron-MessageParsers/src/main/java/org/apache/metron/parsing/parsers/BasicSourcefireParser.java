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

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class BasicSourcefireParser extends BasicParser {

	private static final Logger _LOG = LoggerFactory
					.getLogger(BasicSourcefireParser.class);

	public static final String hostkey = "host";
	String domain_name_regex = "([^\\.]+)\\.([a-z]{2}|[a-z]{3}|([a-z]{2}\\.[a-z]{2}))$";
	String sidRegex = "(.*)(\\[[0-9]+:[0-9]+:[0-9]\\])(.*)$";
	//String sidRegex = "(\\[[0-9]+:[0-9]+:[0-9]\\])(.*)$";
	Pattern sidPattern = Pattern.compile(sidRegex);	
	Pattern pattern = Pattern.compile(domain_name_regex);

	@Override
	public void init() {

	}

	@SuppressWarnings({ "unchecked", "unused" })
	public List<JSONObject> parse(byte[] msg) {

		JSONObject payload = new JSONObject();
		String toParse = "";
		List<JSONObject> messages = new ArrayList<>();
		try {

			toParse = new String(msg, "UTF-8");
			_LOG.debug("Received message: " + toParse);

			String tmp = toParse.substring(toParse.lastIndexOf("{"));
			payload.put("key", tmp);

			String protocol = tmp.substring(tmp.indexOf("{") + 1,
					tmp.indexOf("}")).toLowerCase();
			String source = tmp.substring(tmp.indexOf("}") + 1,
					tmp.indexOf("->")).trim();
			String dest = tmp.substring(tmp.indexOf("->") + 2, tmp.length())
					.trim();

			payload.put("protocol", protocol);

			String source_ip = "";
			String dest_ip = "";

			if (source.contains(":")) {
				String parts[] = source.split(":");
				payload.put("ip_src_addr", parts[0]);
				payload.put("ip_src_port", parts[1]);
				source_ip = parts[0];
			} else {
				payload.put("ip_src_addr", source);
				source_ip = source;

			}

			if (dest.contains(":")) {
				String parts[] = dest.split(":");
				payload.put("ip_dst_addr", parts[0]);
				payload.put("ip_dst_port", parts[1]);
				dest_ip = parts[0];
			} else {
				payload.put("ip_dst_addr", dest);
				dest_ip = dest;
			}
			long timestamp = System.currentTimeMillis();
			payload.put("timestamp", timestamp);
			
			Matcher sidMatcher = sidPattern.matcher(toParse);
			String originalString = null;
			String signatureId = "";
			if (sidMatcher.find()) {
				signatureId = sidMatcher.group(2);
				originalString = sidMatcher.group(1) +" "+ sidMatcher.group(2) + " " + sidMatcher.group(3);
			} else {
				_LOG.warn("Unable to find SID in message: " + toParse);
				originalString = toParse;
			}
			payload.put("original_string", originalString);
			payload.put("signature_id", signatureId);
			messages.add(payload);
			return messages;
		} catch (Exception e) {
			e.printStackTrace();
			_LOG.error("Failed to parse: " + toParse);
			return null;
		}
	}

	


}
