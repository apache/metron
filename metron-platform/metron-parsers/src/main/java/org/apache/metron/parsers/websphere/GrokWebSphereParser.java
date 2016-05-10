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

package org.apache.metron.parsers.websphere;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;

public class GrokWebSphereParser extends BasicParser {

	private static final long serialVersionUID = 4860439408055777358L;
	protected transient Grok  grok;
	protected transient InputStream pattern_url;
	protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
	protected static final Logger LOGGER = LoggerFactory.getLogger(GrokWebSphereParser.class);

	public static final String PREFIX = "stream2file";
	public static final String SUFFIX = ".tmp";

	public static File stream2file(InputStream in) throws IOException {
		final File tempFile = File.createTempFile(PREFIX, SUFFIX);
		tempFile.deleteOnExit();
		try (FileOutputStream out = new FileOutputStream(tempFile)) {
			IOUtils.copy(in, out);
		}
		return tempFile;
	}

	public GrokWebSphereParser() throws Exception {
		pattern_url = getClass().getClassLoader().getResourceAsStream(
				"patterns/websphere");
		File file = stream2file(pattern_url);
		grok = Grok.create(file.getPath());
		grok.compile("%{WEBSPHERE}");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public GrokWebSphereParser(String filepath) throws Exception {
		grok = Grok.create(filepath);
		grok.compile("%{WEBSPHERE}");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public GrokWebSphereParser(String filepath, String pattern) throws Exception {
		grok = Grok.create(filepath);
		grok.compile("%{" + pattern + "}");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	//Set up the Grok pattern if it is not set in the constructor
	@Override
	public void init() {
		pattern_url = getClass().getClassLoader().getResourceAsStream(
				"patterns/websphere");
		File file = new File("test");
		try {
			file = stream2file(pattern_url);
			grok = Grok.create(file.getPath());
			grok.compile("%{WEBSPHERE}");
		} catch (GrokException e) {
			LOGGER.error(e.toString());
		} catch (IOException e) {
			LOGGER.error(e.toString());
		}
	}

	//Parses the incoming telemetry message
	@SuppressWarnings("unchecked")
	@Override
	public List<JSONObject> parse(byte[] rawMessage) {

		String toParse = "";
		JSONObject toReturn;
		List<JSONObject> messages = new ArrayList<>();

		try {
			toParse = new String(rawMessage, "UTF-8");
			LOGGER.info("Received message: " + toParse);
			
			if (grok == null) {
				init();
			}

			Match gm = grok.match(toParse);			
			gm.captures();
			
			toReturn = new JSONObject();
			toReturn.putAll(gm.toMap());

			cleanJSON(toReturn);
			messages.add(toReturn);
			return messages;

		} catch (Exception e) {
			LOGGER.error("Failed to parse: " + toParse);
			LOGGER.error(e.toString());
			return null;
		}
	}

	//Ensures that the JSON has the appropriate fields
	@SuppressWarnings("unchecked")
	private void cleanJSON(JSONObject json) throws ParseException {
		
		//Add original string and epoch timestamp
		json.put("original_string", json.get("WEBSPHERE"));
		json.remove("WEBSPHERE");
		addEpochTimestamp(json);
		parseMessageField(json);
		json.remove("UNWANTED");
		
		// Remove keys for which the value was "" or null
		Iterator<Object> keyIter = json.keySet().iterator();
		while (keyIter.hasNext()) {
			Object key = keyIter.next();
			Object value = json.get(key);
			if (null == value || "".equals(value.toString())) {
				keyIter.remove();
			}
		}	
	}

	//Converts the data string to an epoch timestamp
	@SuppressWarnings("unchecked")
	private void addEpochTimestamp(JSONObject json) throws ParseException {
		long epochTimestamp = 0;

		if (json.containsKey("timestamp_string")) {
			int year = Calendar.getInstance().get(Calendar.YEAR);
			String timestampString = year + " " + json.get("timestamp_string");
			Date date = dateFormat.parse(timestampString);
			epochTimestamp = date.getTime();
		}
		else {
			epochTimestamp = System.currentTimeMillis();
		}

		json.put("timestamp", epochTimestamp);
		json.remove("timestamp_string");
	}

	//Handles the specific parses of different message types
	private void parseMessageField(JSONObject json) {

		if (json.containsKey("message")) {
			String message = (String) json.get("message");
			if (message.contains("logged into")) {
				parseLoginMessage(json);
			}
			else if (message.contains("logged out")) {
				parseLogoutMessage(json);
			}
			else if (message.contains("rbm(")) {
				parseRBMMessage(json);
			}
			else {
				parseOtherMessage(json);
			}
		}
	}

	//Extracts the appropriate fields from login messages
	@SuppressWarnings("unchecked")
	private void parseLoginMessage(JSONObject json) {
		String message = (String) json.get("message");
		String parts[] = message.split(":");
		json.put("username", parts[0].substring(5, parts[0].length()-1));
		json.put("ip_src_addr", parts[1].substring(2, parts[1].length()-1));
		json.put("event_subtype", "login");
		json.remove("message");
	}

	//Extracts the appropriate fields from logout messages
	@SuppressWarnings("unchecked")
	private void parseLogoutMessage(JSONObject json) {
		String message = (String) json.get("message");
		String parts[] = message.split("'");
		json.put("ip_src_addr", parts[0].substring(1, parts[0].lastIndexOf("]")));
		json.put("username", parts[1]);
		json.put("security_domain", parts[3]);
		json.put("event_subtype", "logout");
		json.remove("message");
	}
	
	//Extracts the appropriate fields from RBM messages
	@SuppressWarnings("unchecked")
	private void parseRBMMessage(JSONObject json) {
		String message = (String) json.get("message");
		json.put("process", message.substring(0, message.indexOf("(")));
		json.put("message", message.substring(message.indexOf(":") + 2));
	}

	//Extracts the appropriate fields from other messages
	@SuppressWarnings("unchecked")
	private void parseOtherMessage(JSONObject json) {
		String message = (String) json.get("message");
		if (message.contains("(")) {
			json.put("process", message.substring(0, message.indexOf("(")));
			json.put("message", message.substring(message.indexOf(":") + 2));	
		}
	}
}
