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
package org.apache.metron.parsers.fireeye;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.parsers.utils.ParserUtils;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasicFireEyeParser extends BasicParser {

	private static final long serialVersionUID = 6328907550159134550L;
	protected static final Logger LOG = LoggerFactory
					.getLogger(BasicFireEyeParser.class);


	String tsRegex ="([a-zA-Z]{3})\\s+(\\d+)\\s+(\\d+\\:\\d+\\:\\d+)\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)";
	
	
	Pattern tsPattern = Pattern.compile(tsRegex);
	// private transient static MetronGrok grok;
	// private transient static InputStream pattern_url;

	public BasicFireEyeParser() throws Exception {
		// pattern_url = getClass().getClassLoader().getResourceAsStream(
		// "patterns/fireeye");
		//
		// File file = ParserUtils.stream2file(pattern_url);
		// grok = MetronGrok.create(file.getPath());
		//
		// grok.compile("%{FIREEYE_BASE}");
	}

	@Override
	public void configure(Map<String, Object> parserConfig) {

	}

	@Override
	public void init() {

	}

	@Override
	public List<JSONObject> parse(byte[] raw_message) {
		String toParse = new String(raw_message);
		List<JSONObject> messages = new ArrayList<>();
		try {

			String positveIntPattern = "<[1-9][0-9]*>";
			Pattern p = Pattern.compile(positveIntPattern);
			Matcher m = p.matcher(toParse);

			String delimiter = "";

			while (m.find()) {
				delimiter = m.group();

			}

			if (!StringUtils.isBlank(delimiter)) {
				String[] tokens = toParse.split(delimiter);

				if (tokens.length > 1)
					toParse = delimiter + tokens[1];

			}

			JSONObject toReturn = parseMessage(toParse);

			toReturn.put("timestamp", getTimeStamp(toParse,delimiter));
			messages.add(toReturn);
			return messages;

		} catch (Exception e) {
			LOG.error("Failed to parse: " + new String(raw_message), e);
      throw new IllegalStateException("Unable to Parse Message: " + new String(raw_message) + " due to " + e.getMessage(), e);
		}

	}

	private long getTimeStamp(String toParse,String delimiter) throws ParseException {
		
		long ts = 0;
		String month = null;
		String day = null;
		String time = null;
		Matcher tsMatcher = tsPattern.matcher(toParse);
		if (tsMatcher.find()) {
			month = tsMatcher.group(1);
			day = tsMatcher.group(2);
			time = tsMatcher.group(3);
	
				} else {
			LOG.warn("Unable to find timestamp in message: " + toParse);
			ts = ParserUtils.convertToEpoch(month, day, time, true);
		}

			return ts;
	
	}

	private JSONObject parseMessage(String toParse) {

		// System.out.println("Received message: " + toParse);

		// MetronMatch gm = grok.match(toParse);
		// gm.captures();

		JSONObject toReturn = new JSONObject();
		//toParse = toParse.replaceAll("  ", " ");
		String[] mTokens = toParse.split("\\s+");
	 //mTokens = toParse.split(" ");

		// toReturn.putAll(gm.toMap());

		String id = mTokens[4];

		// We are not parsing the fedata for multi part message as we cannot
		// determine how we can split the message and how many multi part
		// messages can there be.
		// The message itself will be stored in the response.

		String[] tokens = id.split("\\.");
		if (tokens.length == 2) {

			String[] array = Arrays.copyOfRange(mTokens, 1, mTokens.length - 1);
			String syslog = Joiner.on(" ").join(array);

			Multimap<String, String> multiMap = formatMain(syslog);

			for (String key : multiMap.keySet()) {

				String value = Joiner.on(",").join(multiMap.get(key));
				toReturn.put(key, value.trim());
			}

		}

		toReturn.put("original_string", toParse);

		String ip_src_addr = (String) toReturn.get("dvc");
		String ip_src_port = (String) toReturn.get("src_port");
		String ip_dst_addr = (String) toReturn.get("dst_ip");
		String ip_dst_port = (String) toReturn.get("dst_port");

		if (ip_src_addr != null)
			toReturn.put("ip_src_addr", ip_src_addr);
		if (ip_src_port != null)
			toReturn.put("ip_src_port", ip_src_port);
		if (ip_dst_addr != null)
			toReturn.put("ip_dst_addr", ip_dst_addr);
		if (ip_dst_port != null)
			toReturn.put("ip_dst_port", ip_dst_port);

		System.out.println(toReturn);

		return toReturn;
	}

	private Multimap<String, String> formatMain(String in) {
		Multimap<String, String> multiMap = ArrayListMultimap.create();
		String input = in.replaceAll("cn3", "dst_port")
				.replaceAll("cs5", "cncHost").replaceAll("proto", "protocol")
				.replaceAll("rt=", "timestamp=").replaceAll("cs1", "malware")
				.replaceAll("dst=", "dst_ip=")
				.replaceAll("shost", "src_hostname")
				.replaceAll("dmac", "dst_mac").replaceAll("smac", "src_mac")
				.replaceAll("spt", "src_port")
				.replaceAll("\\bsrc\\b", "src_ip");
		String[] tokens = input.split("\\|");

		if (tokens.length > 0) {
			String message = tokens[tokens.length - 1];

			String pattern = "([\\w\\d]+)=([^=]*)(?=\\s*\\w+=|\\s*$) ";
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(message);

			while (m.find()) {
				String[] str = m.group().split("=");
				multiMap.put(str[0], str[1]);

			}

		}
		return multiMap;
	}

	

}
