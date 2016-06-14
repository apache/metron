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

package org.apache.metron.parsers.passthrough;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class BasicPassThroughParser extends BasicParser{

	private static final Logger _LOG = LoggerFactory.getLogger(BasicPassThroughParser.class);

	@Override
	public void configure(Map<String, Object> parserConfig) {

	}

	@Override
	public void init() {

	}

	@SuppressWarnings({ "unchecked", "unused" })
	public List<JSONObject> parse(byte[] msg) {

		String message = new String(msg);
		List<JSONObject> messages = new ArrayList<>();
		JSONObject payload = new JSONObject();
		
		try {
			payload.put("original_string", message);
            payload.put("is_parsed", "false");

			Date date = new Date();
		    long epoch = date.getTime();
		    	    
			payload.put("timestamp", epoch);
			
			messages.add(payload);
			return messages;
		} catch (Exception e) {
			_LOG.error("Failed to parse: " + message, e);
			throw new IllegalStateException("Unable to Parse Message: " + message + " due to " + e.getMessage(), e);
		}
	}

}
