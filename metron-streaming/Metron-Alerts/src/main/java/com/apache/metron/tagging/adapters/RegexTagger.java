/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opensoc.tagging.adapters;

import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class RegexTagger extends AbstractTaggerAdapter{
	
	/**
	 * Reads a regex rules file and tags a message with alerts if any rule from that file
	 * matches anything in the telemetry message
	 */
	private static final long serialVersionUID = -6091495636459799411L;
	Map <String, JSONObject> _rules;
	
	/**
	 * 
	 * @param rules rules read from a properties XML file
	 */
	public RegexTagger(Map<String, JSONObject> rules)
	{
		_rules = rules;
	}

	/**
	 * @param raw_message telemetry message to be tagged
	 */
	@SuppressWarnings("unchecked")
	public JSONArray tag(JSONObject raw_message) {

		JSONArray ja = new JSONArray();
		String message_as_string = raw_message.toString();
		
		for(String rule : _rules.keySet())
		{		
			if (message_as_string.matches(rule))
			{
				ja.add(_rules.get(rule));
			}
		}	
		
		return ja;
	}

}
