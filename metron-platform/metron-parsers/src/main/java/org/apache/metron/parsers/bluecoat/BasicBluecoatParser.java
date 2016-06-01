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

package org.apache.metron.parsers.bluecoat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class BasicBluecoatParser extends BasicParser {

	private static final Logger _LOG = LoggerFactory.getLogger(BasicBluecoatParser.class);
	private SimpleDateFormat df = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");

	@Override
	public void configure(Map<String, Object> parserConfig) {

	}

	@Override
	public void init() {

	}

	@SuppressWarnings({ "unchecked", "unused" })
	public List<JSONObject> parse(byte[] msg) {

		String message = "";
		List<JSONObject> messages = new ArrayList<>();
		JSONObject payload = new JSONObject();
		
		try {
			message = new String(msg, "UTF-8");
			
			
			String[] parts = message.split("<|>|\\(|\\)| ");
			payload.put("original_string", message);
			payload.put("priority", parts[1]);
			
			int year = Calendar.getInstance().get(Calendar.YEAR);
			Date date = df.parse(parts[2] + " " + parts[3] + " " + year + " "+ parts[4]);
		    long epoch = date.getTime();
		    	    
			payload.put("timestamp", epoch);
			payload.put("event_code", parts[6]);
			
			if(parts[6].equals("250017"))
			{
				payload.put("event_type", "authentication failure");
				payload.put("eid", parts[12].substring(1, parts[12].length()-1));
				payload.put("designated_host", parts[10].substring(0, parts[10].length()-1));
				payload.put("realm", parts[15]);
				payload.put("message", message.split("250017")[1]);
			}
			else if(parts[6].equals("250018"))
			{
				payload.put("event_type", "authentication failure");
				payload.put("realm", parts[24].substring(1, parts[24].length()-1));
				payload.put("message", message.split("250018")[1]);
			}
			else if(parts[6].equals("E0000"))
			{
				payload.put("event_type", "authentication");
				payload.put("message", message.split("E0000")[1]);
			}
			else if(parts[6].equals("250001"))
			{
				payload.put("event_type", "authentication failure");
				payload.put("designated_host", parts[11].substring(0, parts[11].length()-1));
				payload.put("realm", parts[17].substring(1, parts[17].length()-1));
				payload.put("message", message.split("250001")[1]);
			}
			
			messages.add(payload);
			return messages;
		} catch (Exception e) {
			e.printStackTrace();
			_LOG.error("Failed to parse: " + message);
			return null;
		}
	}

}
