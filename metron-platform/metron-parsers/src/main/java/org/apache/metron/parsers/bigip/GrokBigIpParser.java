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

package org.apache.metron.parsers.bigip;

import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;
import org.apache.metron.parsers.GrokParser;
import org.json.simple.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrokBigIpParser extends GrokParser {

	public GrokBigIpParser(String grokHdfsPath, String patternLabel) {
		super(grokHdfsPath, patternLabel);
	}

	private final static String BIGIP_NEWSESSION_PATTERN = "NEWSESSION";
	private final static String BIGIP_STATISTICS_PATTERN = "STATISTICS";
	private final static String BIGIP_LOGIN_PATTERN = "LOGIN";
	private final static String BIGIP_ACCESSPOLICYRESULT_PATTERN = "ACCESSPOLICYRESULT";
	private final static String BIGIP_SESSION_PATTERN = "SESSION";

	@Override
	protected long formatTimestamp(Object value) {
		long epochTimestamp = System.currentTimeMillis();
		if (value != null) {
			try {
				epochTimestamp = toEpoch(Calendar.getInstance().get(Calendar.YEAR)  + " " + value);
			} catch (ParseException e) {
				//default to current time
			}
		}
		return epochTimestamp;
	}

	@Override
	protected void postParse(JSONObject message) {
		removeEmptyFields(message);
		message.remove("timestamp_string");
		// identify the log type and parse the message field accordingly
		if (message.containsKey("message")) {
			String messageValue = (String) message.get("message");
			if (messageValue.contains("New session")) {
				parseMessage(message, "session", BIGIP_NEWSESSION_PATTERN, true);
			}
			else if (messageValue.contains("session.logon")) {
				parseMessage(message, "login", BIGIP_LOGIN_PATTERN, true);
			}
			else if (messageValue.contains("Session statistics")) {
				parseMessage(message, "statistics", BIGIP_STATISTICS_PATTERN, true);
			}
			else if (messageValue.contains("Access policy result")) {
				parseMessage(message, "access policy result", BIGIP_ACCESSPOLICYRESULT_PATTERN, true);
			}
			else {
				Pattern pattern = Pattern.compile("\\d{8}:5: \\w{8}:");
				Matcher matcher = pattern.matcher(messageValue);
				// Other session messsages match this regex (contain a log code and session id)
				if (matcher.find()){
					parseMessage(message, "session", BIGIP_SESSION_PATTERN, false);
				}
				// System messages do not contain a log code and session id
				else{
					// System messages do not need to be parsed a second time
					message.put("big_ip_message_type", "system");
				}
			}
		}
	}

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

	//Extracts the appropriate fields from messages
	@SuppressWarnings("unchecked")
	private void parseMessage(JSONObject json, String messageType, String pattern, boolean removeMessage) {
		json.put("big_ip_message_type", messageType);
		try {
			// compile the grok object to parse the message field according to log type
			grok.compile("%{" + pattern + "}");
			Match gm = grok.match((String) json.get("message"));
			gm.captures();
			json.putAll(gm.toMap());
		} catch (GrokException e) {
			LOG.debug("Error parsing Big IP log", e.toString());
		}
		json.remove(pattern);
		if (removeMessage){
			json.remove("message");
		}
		try {
			// recompile the grok object for the next log
			grok.compile("%{" + "BIGIP" + "}");
		} catch (GrokException e) {
			e.printStackTrace();
		}
	}

}
