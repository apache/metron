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

			String[] parts = message.split("\\|");

			String [] logstart = parts[0].split("<|>|\\(|\\)| ");
			Date date = df.parse(logstart[2] + " " + logstart[3] + " " + Calendar.getInstance().get(Calendar.YEAR) + " " + logstart[4] + " UTC");
			long epoch = date.getTime();
			payload.put("timestamp", epoch);
			payload.put("priority", logstart[1]);

			for (int i = 1; i < parts.length -1; ++i){
				String[] kvp = parts[i].split("=");
				String value = kvp[1];
				if (value.matches("\".*\"")){
					value = value.substring(1, value.length()-1);
				}
				payload.put(kvp[0], value);
			}

			removeEmptyFields(payload);
			messages.add(payload);
			return messages;
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("Failed to parse: " + new String(msg));
			return null;
		}
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