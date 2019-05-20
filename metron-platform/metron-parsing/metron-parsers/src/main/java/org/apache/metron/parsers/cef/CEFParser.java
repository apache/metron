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

package org.apache.metron.parsers.cef;

import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.metron.parsers.BasicParser;
import org.apache.metron.parsers.ParseException;
import org.apache.metron.parsers.utils.DateUtils;
import org.apache.metron.parsers.utils.SyslogUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CEFParser extends BasicParser {
	private static final long serialVersionUID = 1L;

	protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String HEADER_CAPTURE_PATTERN = "[^\\|]*";
	private static final String EXTENSION_CAPTURE_PATTERN = "(?<!\\\\)=";
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private Pattern p;
	private static final Pattern patternExtensions = Pattern.compile(EXTENSION_CAPTURE_PATTERN);

	public void init() {

		// CEF Headers: Device Vendor|Device Product|Device Version|Device Event
		// Class ID|Name|Severity

		String syslogTime = "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\b +(?:(?:0[1-9])|(?:[12][0-9])|(?:3[01])|[1-9]) (?!<[0-9])(?:2[0123]|[01]?[0-9]):(?:[0-5][0-9])(?::(?:(?:[0-5]?[0-9]|60)(?:[:.,][0-9]+)?))(?![0-9])?";
		String syslogTime5424 = "(?:\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2}))";
		String syslogPriority = "<(?:[0-9]+)>";
		String syslogHost = "[a-z0-9\\.\\\\-_]+";

		StringBuilder sb = new StringBuilder("");
		sb.append("(?<syslogPriority>");
		sb.append(syslogPriority);
		sb.append(")?");
		sb.append("(?<syslogTime>");
		sb.append(syslogTime);
		sb.append("|");
		sb.append(syslogTime5424);
		sb.append(")?");

		sb.append("(?<syslogHost>");
		sb.append(syslogHost);
		sb.append(")?");

		sb.append(".*");

		sb.append("CEF: ?0\\|");

		headerBlock("DeviceVendor", sb);
		sb.append("\\|");
		headerBlock("DeviceProduct", sb);
		sb.append("\\|");
		headerBlock("DeviceVersion", sb);
		sb.append("\\|");
		headerBlock("DeviceEvent", sb);
		sb.append("\\|");
		headerBlock("Name", sb);
		sb.append("\\|");
		headerBlock("Severity", sb);
		sb.append("\\|");

		// extension capture:
		sb.append("(?<extensions>.*)");
		String pattern = sb.toString();

		p = Pattern.compile(pattern);

	}

	public static void parseExtensions(String ext, JSONObject obj) {
		Matcher m = patternExtensions.matcher(ext);

		int index = 0;
		String key = null;
		String value = null;
		Map<String, String> labelMap = new HashMap<String, String>();

		while (m.find()) {
			if (key == null) {
				key = ext.substring(index, m.start());
				index = m.end();
				if (!m.find()) {
					break;
				}
			}
			value = ext.substring(index, m.start());
			index = m.end();
			int v = value.lastIndexOf(" ");
			if (v > 0) {
				String temp = value.substring(0, v).trim();
				if (key.endsWith("Label")) {
					labelMap.put(key.substring(0, key.length() - 5), temp);
				} else {
					obj.put(key, temp);
				}
				key = value.substring(v).trim();
			}
		}
		value = ext.substring(index);

		// Build a map of Label extensions to apply later
		if (key.endsWith("Label")) {
			labelMap.put(key.substring(0, key.length() - 5), value);
		} else {
			obj.put(key, value);
		}

		// Apply the labels to custom fields
		for (Entry<String, String> label : labelMap.entrySet()) {
			mutate(obj, label.getKey(), label.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	public List<JSONObject> parse(byte[] rawMessage) {
		List<JSONObject> messages = new ArrayList<>();

		String cefString = new String(rawMessage, UTF_8);

		Matcher matcher = p.matcher(cefString);

		while (matcher.find()) {
			JSONObject obj = new JSONObject();
			if (matcher.matches()) {
				LOG.debug("Found %d groups", matcher.groupCount());
				obj.put("DeviceVendor", matcher.group("DeviceVendor"));
				obj.put("DeviceProduct", matcher.group("DeviceProduct"));
				obj.put("DeviceVersion", matcher.group("DeviceVersion"));
				obj.put("DeviceEvent", matcher.group("DeviceEvent"));
				obj.put("Name", matcher.group("Name"));
				obj.put("Severity", standardizeSeverity(matcher.group("Severity")));
			}

			parseExtensions(matcher.group("extensions"), obj);

			// Rename standard CEF fields to comply with Metron standards
			obj = mutate(obj, "dst", "ip_dst_addr");
			obj = mutate(obj, "dpt", "ip_dst_port");
			obj = convertToInt(obj, "ip_dst_port");

			obj = mutate(obj, "src", "ip_src_addr");
			obj = mutate(obj, "spt", "ip_src_port");
			obj = convertToInt(obj, "ip_src_port");

			obj = mutate(obj, "act", "deviceAction");
			// applicationProtocol
			obj = mutate(obj, "app", "protocol");

			obj.put("original_string", cefString);

			// apply timestamp from message if present, using rt, syslog
			// timestamp,
			// default to current system time

			if (obj.containsKey("rt")) {
				String rt = (String) obj.get("rt");
				try {
					obj.put("timestamp", DateUtils.parseMultiformat(rt, DateUtils.DATE_FORMATS_CEF));
				} catch (java.text.ParseException e) {
					throw new IllegalStateException("rt field present in CEF but cannot be parsed", e);
				}
			} else {
				String logTimestamp = matcher.group("syslogTime");
				if (!(logTimestamp == null || logTimestamp.isEmpty())) {
					try {
						obj.put("timestamp", SyslogUtils.parseTimestampToEpochMillis(logTimestamp, Clock.systemUTC()));
					} catch (ParseException e) {
						throw new IllegalStateException("Cannot parse syslog timestamp", e);
					}
				} else {
					obj.put("timestamp", System.currentTimeMillis());
				}
			}

			// add the host
			String host = matcher.group("syslogHost");
			if (!(host == null || host.isEmpty())) {
				obj.put("host", host);
			}

			messages.add(obj);
		}
		return messages;
	}

	@SuppressWarnings("unchecked")
	private JSONObject convertToInt(JSONObject obj, String key) {
		if (obj.containsKey(key)) {
			obj.put(key, Integer.valueOf((String) obj.get(key)));
		}
		return obj;
	}

	private void headerBlock(String name, StringBuilder sb) {
		sb.append("(?<").append(name).append(">").append(HEADER_CAPTURE_PATTERN).append(")");
	}

	/**
	 * Maps string based severity in CEF format to integer.
	 * 
	 * The strings are mapped according to the CEF 23 specification, taking the
	 * integer value as the value of the range buckets rounded up
	 * 
	 * The valid string values are: Unknown, Low, Medium, High, and Very-High.
	 * The valid integer values are: 0-3=Low, 4-6=Medium, 7- 8=High, and
	 * 9-10=Very-High.
	 * 
	 * @param severity
	 *            String or Integer
	 * @return Integer value mapped from the string
	 */
	private Integer standardizeSeverity(String severity) {
		if (severity.length() < 3) {
			// should be a number
			return Integer.valueOf(severity);
		} else {
			switch (severity) {
			case "Low":
				return 2;
			case "Medium":
				return 5;
			case "High":
				return 8;
			case "Very-High":
				return 10;
			default:
				return 0;
			}
		}
	}

	@Override
	public void configure(Map<String, Object> config) {
	}

	@SuppressWarnings("unchecked")
	private static JSONObject mutate(JSONObject json, String oldKey, String newKey) {
		if (json.containsKey(oldKey)) {
			json.put(newKey, json.remove(oldKey));
		}
		return json;
	}

}
