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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class CEFParser extends BasicParser {

	// Set up the requisite variables
	private static final Logger LOGGER = LoggerFactory.getLogger(CEFParser.class);
	private String dateFormatString;
	private String headerTimestampRegex;
	private TimeZone timeZone;
	private boolean timestampContainsYear = true;

	@Override
	public void init() {
		//Nothing to do to initialize the CEF parser
	}

	@Override
	public void configure(Map<String, Object> config) {
		withDateFormat((String) config.get("dateFormat"));
		withTimeZone((String) config.get("timezone"));
		withHeaderTimestampRegex((String) config.get("headerRegEx"));
	}

	//For specifying the date format that the parser will use
	public CEFParser withDateFormat(String dateFormat) {
		if (dateFormat == null) {
			throw new IllegalArgumentException("DateFormat must be specified in parser config file");
		}
		this.dateFormatString = dateFormat;
		if (LOG.isDebugEnabled()) {
			LOG.debug("CEF parser setting date format: " + dateFormat);
		}
		return this;
	}

	//For setting the timezone of the parser
	public CEFParser withTimeZone(String timeZone) {
		if (timeZone == null) {
			timeZone = "UTC";
		}
		this.timeZone = TimeZone.getTimeZone(timeZone);
		if (LOG.isDebugEnabled()) {
			LOG.debug("CEF parser setting timezone: " + timeZone);
		}
		return this;
	}

	//For telling the parser how to pull out a date from the header
	public CEFParser withHeaderTimestampRegex(String headerRegex) {
		this.headerTimestampRegex = headerRegex;
		if (LOG.isDebugEnabled()) {
			LOG.debug("CEF parser setting header regular expression: " + headerRegex);
		}
		return this;
	}

	// Parse a raw telemetry message
	@SuppressWarnings({ "unchecked"})
	public List<JSONObject> parse(byte[] rawMessage) {

		String message = new String(rawMessage);
		List<JSONObject> messages = new ArrayList<>();
		JSONObject payload = new JSONObject();

		try {

			// Only attempt to split if this is a well-formed CEF line
			if (StringUtils.countMatches(message, "|") < 7){
				LOGGER.error("Not a well-formed CEF line, Failed to parse: " + message);
				throw new IllegalStateException("Unable to Parse Message: " + message + " due to not being a well-formed CEF line");
			}
			
			payload.put("original_string", message.replace("\\=", "="));
			String[] parts = message.split("\\|", 8);

			// Add the standard CEF fields
			payload.put("header", parts[0]);
			payload.put("device_vendor", parts[1]);
			payload.put("device_product", parts[2]);
			payload.put("device_version", parts[3]);
			payload.put("event_class_id", parts[4]);
			payload.put("event_name", parts[5]);
			payload.put("severity", parts[6]);

			// Add the device-specific CEF fields
			String fields = parts[7];
			String key = "";
			String value = "";

			while ((findNextEquals(fields) !=  findLastEquals(fields)) && fields.contains(" ")) {


				// Extract the key-value pairs
				key = fields.substring(0, findNextEquals(fields)).trim();
				fields = fields.substring(findNextEquals(fields) + 1);
			value = fields.substring(0, findNextEquals(fields));
				value = value.substring(0, value.lastIndexOf(" "));
				fields = fields.substring(value.length() + 1);

				//Trim and remove escaped equals characters from values and keys
				key = key.replace("\\=", "=").trim();
				value = value.replace("\\=", "=").trim();
				
				// Place in JSON, accounting for custom field names
				if (payload.containsKey(key+"Label")) {
					payload.put(payload.get(key+"Label"), value);	
					payload.remove(key+"Label");
				}
				else if (key.matches("\\w+\\dLabel") &&  payload.containsKey(key.substring(0, key.indexOf("Label")))) {
					payload.put(value, payload.get(key.substring(0, key.indexOf("Label"))));
					payload.remove(key.substring(0, key.indexOf("Label")));
				}
				else {
					payload.put(key, value);
				}
			}

			// Handle last remaining key-value pair
			key = fields.substring(0, findNextEquals(fields)).replace("\\=", "=").trim();
			value = fields.substring(findNextEquals(fields) + 1).replace("\\=", "=").trim();
			if (payload.containsKey(key+"Label")) {
				payload.put(payload.get(key+"Label"), value);	
				payload.remove(key+"Label");
			}
			else if (key.matches("\\w+\\dLabel") && payload.containsKey(key.substring(0, key.indexOf("Label")))) {
				payload.put(value, payload.get(key.substring(0, key.indexOf("Label"))));
				payload.remove(key.substring(0, key.indexOf("Label")));

			}
			else {
				payload.put(key, value);
			}

			//Ensure that the required fields are present in the JSON
			parseHeader(payload);	
			removeEmptyFields(payload);
			handleTimestamp(payload);
			useReadableFieldsJSON(payload);
			
			messages.add(payload);	
			return messages;

		} catch (Exception e) {
			LOGGER.error("Failed to parse: " + message + " with error message " + e.getMessage(), e);
			throw new IllegalStateException("Unable to Parse Message: " + message + " due to " + e.getMessage(), e);
		}
	}


	// Finds the next non-escaped equals sign
	public int findNextEquals(String input) {

    int nextEqualsIndex = 0;
    int indexOffset = 0;
    int currentIndex = 0;
    boolean found = false;

    if((input.startsWith("http") || input.startsWith("Value=")) && input.contains(" ")){
      indexOffset = input.indexOf(" ") + 1;
      input = input.substring(input.indexOf(" ") + 1);
    }

		if (input.indexOf("=") == -1)
			return -1;

		if (input.indexOf("=") == 0)
			return 0;

		while (!found) {
			nextEqualsIndex = input.indexOf("=", currentIndex);
			if (!"\\".equals(input.substring(nextEqualsIndex-1, nextEqualsIndex)))
				found = true;
			currentIndex = nextEqualsIndex + 1;
		}
    nextEqualsIndex = nextEqualsIndex + indexOffset;
    return nextEqualsIndex;
	}


	// Finds the last non-escaped equals sign
	public int findLastEquals(String input) {

		int lastEqualsIndex = 0;
		int currentIndex = input.length();
		boolean found = false;

		if (input.lastIndexOf("=") == -1)
			return -1;

		if (input.lastIndexOf("=") == 0)
			return 0;

		while (!found) {
			lastEqualsIndex = input.lastIndexOf("=", currentIndex);
			if (!"\\".equals(input.substring(lastEqualsIndex-1, lastEqualsIndex)))
				found = true;
			currentIndex = lastEqualsIndex - 1;
		}

		return lastEqualsIndex;
	}


	//Parses the syslog priority field and the timestamp from the header if they exist
	@SuppressWarnings("unchecked")
	public void parseHeader(JSONObject json) {

		if (json.containsKey("header")) {
			String header = (String) json.get("header");
			if (header.startsWith("<") && header.contains(">")) {
				json.put("priority", header.substring(1, header.indexOf(">")));
			}
			if (null != headerTimestampRegex) {
				Pattern pattern = Pattern.compile(".*(" + headerTimestampRegex + ").*");
				Matcher matcher = pattern.matcher(header);
				if (matcher.find())
				{
					String timestamp = matcher.group(1);
					json.put("rt", timestamp);
				}
			}
		}		
	}

	//Removes any null or empty values from the JSON
	@SuppressWarnings("unchecked")
	private void removeEmptyFields(JSONObject json) {
		Iterator<Object> keyIter = json.keySet().iterator();
		while (keyIter.hasNext()) {
			Object key = keyIter.next();
			Object value = json.get(key);
			if (null == value || "".equals(value.toString())) {
				keyIter.remove();
			}
		}
	}
	
	//Converts a timestamp string to a long
	@SuppressWarnings("unchecked")
	private void handleTimestamp(JSONObject json) {
		
		long epochTimestamp = System.currentTimeMillis();
		
		//Checks for the CEF timestamp field
		if (json.containsKey("rt")) {
			
			String timestamp = (String) json.get("rt");
			if(timestamp.equals("${Event.createTime}")){
				json.put("timestamp", System.currentTimeMillis());
				json.remove("rt");
				return;
			}

			//Adds the year if it is not present
			if (!dateFormatString.contains("yyyy")) {
				timestampContainsYear = false;
				dateFormatString = "yyyy " + dateFormatString;
				
			}

			// Set up the dateformat object with timezone
			SimpleDateFormat dateFormat;
			if (timestampContainsYear) {
				dateFormat = new SimpleDateFormat(dateFormatString);
			}
			else {
				dateFormat = new SimpleDateFormat("yyyy" + dateFormatString);
				int year = Calendar.getInstance().get(Calendar.YEAR);
				timestamp = year + " " + json.get("rt");
			}
			dateFormat.setTimeZone(timeZone);

			try {
				timestamp = timestamp.replace("Z", "UTC");
				epochTimestamp = dateFormat.parse(timestamp).getTime();
			} catch (ParseException e) {
				LOGGER.error("Date Parsing Exception:" + e.toString());
				json.put("timestamp", epochTimestamp);
			}
			
			json.remove("rt");

		}
		else {
			//Default to system time
		}
		json.put("timestamp", epochTimestamp);
	}

	//Replaces CEF shorthand field names with humna-readable field names
	@SuppressWarnings("unchecked")
	public void useReadableFieldsJSON(JSONObject json) {

		if (json.containsKey("act")) {
			json.put("deviceAction", json.get("act"));
			json.remove("act");
		}
		if (json.containsKey("app")) {
			json.put("applicationProtocol", json.get("app"));
			json.remove("app");
		}
		if (json.containsKey("cat")) {
			json.put("deviceEventCategory", json.get("cat"));
			json.remove("cat");
		}
		if (json.containsKey("cnt")) {
			json.put("baseEventCount", json.get("cnt"));
			json.remove("cnt");
		}
		if (json.containsKey("dhost")) {
			json.put("dst_hostname", json.get("dhost"));
			json.remove("dhost");
		}
		if (json.containsKey("dmac")) {
			json.put("dst_mac", json.get("dmac"));
			json.remove("dmac");
		}
		if (json.containsKey("dntdom")) {
			json.put("destinationNtDomain", json.get("dntdom"));
			json.remove("dntdom");
		}
		if (json.containsKey("dpid")) {
			json.put("destinationProcessId", json.get("dpid"));
			json.remove("dpid");
		}
		if (json.containsKey("dpriv")) {
			json.put("destinationUserPrivileges", json.get("dpriv"));
			json.remove("dpriv");
		}
		if (json.containsKey("dproc")) {
			json.put("destinationProcessName", json.get("dproc"));
			json.remove("dproc");
		}
		if (json.containsKey("dpt")) {
			json.put("ip_dst_port", json.get("dpt"));
			json.remove("dpt");
		}
		if (json.containsKey("dst")) {
			json.put("ip_dst_addr", json.get("dst"));
			json.remove("dst");
		}
		if (json.containsKey("dtz")) {
			json.put("deviceTimeZone", json.get("dtz"));
			json.remove("dtz");
		}
		if (json.containsKey("duid")) {
			json.put("dst_user_id", json.get("duid"));
			json.remove("duid");
		}
		if (json.containsKey("duser")) {
			json.put("dst_username", json.get("duser"));
			json.remove("duser");
		}
		if (json.containsKey("dvc")) {
			json.put("deviceAddress", json.get("dvc"));
			json.remove("dvc");
		}
		if (json.containsKey("dvchost")) {
			json.put("deviceHostName", json.get("dvchost"));
			json.remove("dvchost");
		}
		if (json.containsKey("dvcmac")) {
			json.put("deviceMacAddress", json.get("dvcmac"));
			json.remove("dvcmac");
		}
		if (json.containsKey("dvcpid")) {
			json.put("deviceProcessId", json.get("dvcpid"));
			json.remove("dvcpid");
		}
		if (json.containsKey("end")) {
			json.put("endTime", json.get("end"));
			json.remove("end");
		}
		if (json.containsKey("fname")) {
			json.put("fileName", json.get("fname"));
			json.remove("fname");
		}
		if (json.containsKey("fsize")) {
			json.put("fileSize", json.get("fsize"));
			json.remove("fsize");
		}
		if (json.containsKey("in")) {
			json.put("bytesIn", json.get("in"));
			json.remove("in");
		}
		if (json.containsKey("msg")) {
			json.put("message", json.get("msg"));
			json.remove("msg");
		}
		if (json.containsKey("out")) {
			json.put("bytesOut", json.get("out"));
			json.remove("out");
		}
		if (json.containsKey("outcome")) {
			json.put("eventOutcome", json.get("outcome"));
			json.remove("outcome");
		}
		if (json.containsKey("proto")) {
			json.put("protocol", json.get("proto"));
			json.remove("proto");
		}
		if (json.containsKey("request")) {
			json.put("requestURL", json.get("request"));
			json.remove("request");
		}
		if (json.containsKey("shost")) {
			json.put("src_hostname", json.get("shost"));
			json.remove("shost");
		}
		if (json.containsKey("smac")) {
			json.put("src_mac", json.get("smac"));
			json.remove("smac");
		}
		if (json.containsKey("sntdom")) {
			json.put("sourceNtDomain", json.get("sntdom"));
			json.remove("sntdom");
		}
		if (json.containsKey("spid")) {
			json.put("sourceProcessId", json.get("spid"));
			json.remove("spid");
		}
		if (json.containsKey("spriv")) {
			json.put("sourceUserPrivileges", json.get("spriv"));
			json.remove("spriv");
		}
		if (json.containsKey("sproc")) {
			json.put("sourceProcessName", json.get("sproc"));
			json.remove("sproc");
		}
		if (json.containsKey("spt")) {
			json.put("ip_src_port", json.get("spt"));
			json.remove("spt");
		}
		if (json.containsKey("src")) {
			json.put("ip_src_addr", json.get("src"));
			json.remove("src");
		}
		if (json.containsKey("start")) {
			json.put("startTime", json.get("start"));
			json.remove("start");
		}
		if (json.containsKey("suid")) {
			json.put("src_user_id", json.get("suid"));
			json.remove("suid");
		}
		if (json.containsKey("suser")) {
			json.put("src_username", json.get("suser"));
			json.remove("suser");
		}
		if (json.containsKey("agt")) {
			json.put("agentAddress", json.get("agt"));
			json.remove("agt");
		}
		if (json.containsKey("ahost")) {
			json.put("agentHostName", json.get("ahost"));
			json.remove("ahost");
		}
		if (json.containsKey("aid")) {
			json.put("agentId", json.get("aid"));
			json.remove("aid");
		}
		if (json.containsKey("amac")) {
			json.put("agentMacAddress", json.get("amac"));
			json.remove("amac");
		}
		if (json.containsKey("art")) {
			json.put("agentReceiptTime", json.get("art"));
			json.remove("art");
		}
		if (json.containsKey("at")) {
			json.put("agentType", json.get("at"));
			json.remove("at");
		}
		if (json.containsKey("atz")) {
			json.put("agentTimeZone", json.get("atz"));
			json.remove("atz");
		}
		if (json.containsKey("av")) {
			json.put("agentVersion", json.get("av"));
			json.remove("av");
		}
		if (json.containsKey("dlat")) {
			json.put("destinationGeoLatitude", json.get("dlat"));
			json.remove("dlat");
		}
		if (json.containsKey("dlong")) {
			json.put("destinationGeoLongitude", json.get("dlong"));
			json.remove("dlong");
		}
		if (json.containsKey("slat")) {
			json.put("sourceGeoLatitude", json.get("slat"));
			json.remove("slat");
		}
		if (json.containsKey("slong")) {
			json.put("sourceGeoLongitude", json.get("slong"));
			json.remove("slong");
		}
	}

}
