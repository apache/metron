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

package org.apache.metron.parsers.bluecoatcim;

import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class BasicBluecoatCIMParser extends BasicParser {

	private static final Logger LOG = LoggerFactory.getLogger(BasicBluecoatCIMParser.class);
	private SimpleDateFormat df = new SimpleDateFormat("MMM dd yyyy HH:mm:ss zzz");
	
	@Override
	public void init() {

	}

	@SuppressWarnings({ "unchecked", "unused" })
	public List<JSONObject> parse(byte[] msg) {

		List<JSONObject> messages = new ArrayList<>();
		JSONObject payload = new JSONObject();
		
		try {
			String message = new String(msg, "UTF-8");
			payload.put("original_string", message);

			// some values in the pipe delimited bluecoat cim logs contain pipes
			// parse and remove them first to avoid problems parsing the rest of the string
			String[] array;
			array = handleValueContainingDelimiter(message, "uri_query", "|uri_extension=");
			message = array[0];
			payload.put("uri_query", array[1]);
			array = handleValueContainingDelimiter(message, "http_referrer", "|status=");
			message = array[0];
			payload.put("http_referrer", array[1]);

			// split message into pipe delimited substrings
			String[] substrings = message.split("\\|");

			// the first substring contains timestamp and priority
			String [] logstart = substrings[0].split("<|>|\\(|\\)| ");
			Date date = df.parse(logstart[2] + " " + logstart[3] + " " + Calendar.getInstance().get(Calendar.YEAR) + " " + logstart[4] + " UTC");
			long epoch = date.getTime();
			payload.put("timestamp", epoch);
			payload.put("priority", logstart[1]);

			// the remaining substrings (except for the last one) are key value pairs with format key=value
			for (int i = 1; i < substrings.length -1; ++i){
				String[] kvp = substrings[i].split("=",2);
				String value = kvp[1];
				// remove quotes that appear around some values
				if (value.matches("\".*\"")){
					value = value.substring(1, value.length()-1);
				}
				payload.put(kvp[0], value);
			}

			removeEmptyFields(payload);
			messages.add(payload);
			return messages;
		} catch (Exception e) {
			LOG.error("Failed to parse: " + new String(msg) + "\n");
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private String[] handleValueContainingDelimiter(String message, String key, String nextKey){
		int keyStart = message.indexOf(key);
		int valueStart = keyStart + key.length() + 1;
		int valueEnd = message.indexOf(nextKey);
		String value = message.substring(valueStart,valueEnd);
		String newMessage = message.substring(0, keyStart - 1) + message.substring(valueEnd);
		return new String[] {newMessage, value};
	}

	@SuppressWarnings("unchecked")
	private void removeEmptyFields(JSONObject json) {
		Iterator<Object> keyIter = json.keySet().iterator();
		while (keyIter.hasNext()) {
			Object key = keyIter.next();
			Object value = json.get(key);
			if (null == value || "".equals(value.toString()) || "-".equals(value.toString())) {
				keyIter.remove();
			}
		}
	}
}