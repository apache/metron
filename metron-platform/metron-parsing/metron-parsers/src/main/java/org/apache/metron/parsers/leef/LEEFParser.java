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
package org.apache.metron.parsers.leef;

import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.metron.parsers.BasicParser;
import org.apache.metron.parsers.ParseException;
import org.apache.metron.parsers.cef.CEFParser;
import org.apache.metron.parsers.utils.DateUtils;
import org.apache.metron.parsers.utils.SyslogUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LEEFParser extends BasicParser {
	private static final long serialVersionUID = 1L;

	protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String HEADER_CAPTURE_PATTERN = "[^\\|]*";
	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private static final String EXTENSION_CAPTURE_PATTERN = "(?<!\\\\)=";

	private Pattern p;
	private Pattern pext;

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

		sb.append("LEEF:(?<Version>1.0|2.0|0)?\\|");

		headerBlock("DeviceVendor", sb);
		sb.append("\\|");
		headerBlock("DeviceProduct", sb);
		sb.append("\\|");
		headerBlock("DeviceVersion", sb);
		sb.append("\\|");
		headerBlock("DeviceEvent", sb);
		sb.append("\\|");
		
		// add optional delimiter header (only applicable for LEEF 2.0)
		sb.append("(");
		headerBlock("Delimiter", sb);
		sb.append("\\|");
		sb.append(")?");
		
		// extension capture:
		sb.append(" ?(?<extensions>.*)");
		String pattern = sb.toString();

		p = Pattern.compile(pattern);

		pext = Pattern.compile(EXTENSION_CAPTURE_PATTERN);
	}

	@SuppressWarnings("unchecked")
	public List<JSONObject> parse(byte[] rawMessage) {
		List<JSONObject> messages = new ArrayList<>();

		String cefString = new String(rawMessage, UTF_8);

		Matcher matcher = p.matcher(cefString);

		while (matcher.find()) {
			JSONObject obj = new JSONObject();
			if (!matcher.matches()) {
				break;
			}
			LOG.debug("Found %d groups", matcher.groupCount());
			obj.put("DeviceVendor", matcher.group("DeviceVendor"));
			obj.put("DeviceProduct", matcher.group("DeviceProduct"));
			obj.put("DeviceVersion", matcher.group("DeviceVersion"));
			obj.put("DeviceEvent", matcher.group("DeviceEvent"));
			
			String ext = matcher.group("extensions");

			// In LEEF 2.0 the delimiter can be specified
			String version = matcher.group("Version");
			if (version.equals("2.0")) {
				String delimiter = matcher.group("Delimiter");
				if (delimiter == null || delimiter.length() == 0) {
					delimiter = "\\t";
				}
				delimiter = "(?<!\\\\)[" + delimiter.replace("^", "\\^").replace("\t", "\\t") + "]";
				
				String[] kvs = ext.split(delimiter); 
				for (String kv: kvs) {
					String[] a = kv.split("=");
					obj.put(a[0], a[1]);
				}
			} else if (version.equals("1.0") || version.isEmpty()) {
				// TODO - Support LEEF 1.0 delimiters
				String delimiter = "\t";
				String[] kvs = ext.split(delimiter);
				for (String kv: kvs) {
					String[] a = kv.split("=");
					obj.put(a[0], a[1]);
				}
			} else {
				// Found in the wild examples using CEF rules, which need to handle the processing per the CEFParser
				// Note that technically LEEF does not support the CEF approach to numbered custom variables.
				// We however do here, due to some found in the wild exceptions to the standard.
				CEFParser.parseExtensions(ext, obj);
			}

			// Rename standard CEF fields to comply with Metron standards
			obj = mutate(obj, "dst", "ip_dst_addr");
			obj = mutate(obj, "dstPort", "ip_dst_port");
			obj = convertToInt(obj, "ip_dst_port");

			obj = mutate(obj, "src", "ip_src_addr");
			obj = mutate(obj, "srcPort", "ip_src_port");
			obj = convertToInt(obj, "ip_src_port");

			obj.put("original_string", cefString);

			// apply timestamp from message if present, using devTime, syslog
			// timestamp,
			// default to current system time
			//devTime, devTimeFormat, calLanguage, calCountryOrRegion
			if (obj.containsKey("devTime")) {
				String devTime = (String) obj.get("devTime");
				try {
					// DateFormats allowed in LEEF
					// epoch
					// MMM dd yyyy HH:mm:ss
					// MMM dd yyyy HH:mm:ss.SSS
					// MMM dd yyyy HH:mm:ss.SSS zzz
					// custom in devTimeFormat field
					final String devTimeFormat = (String) obj.get("devTimeFormat");
					
					List<SimpleDateFormat> formats = (obj.containsKey("devTimeFormat"))?
						new ArrayList<SimpleDateFormat>() {{ add(new SimpleDateFormat(devTimeFormat)); }}: 
						DateUtils.DATE_FORMATS_LEEF;
					obj.put("timestamp", DateUtils.parseMultiformat(devTime, formats ));
				} catch (java.text.ParseException e) {
					throw new IllegalStateException("devTime field present in LEEF but cannot be parsed", e);
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

	@Override
	public void configure(Map<String, Object> config) {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("unchecked")
	private JSONObject mutate(JSONObject json, String oldKey, String newKey) {
		if (json.containsKey(oldKey)) {
			json.put(newKey, json.remove(oldKey));
		}
		return json;
	}

}

