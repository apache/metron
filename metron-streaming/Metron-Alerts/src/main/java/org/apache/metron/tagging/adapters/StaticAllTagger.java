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

package org.apache.metron.tagging.adapters;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class StaticAllTagger extends AbstractTaggerAdapter {

	/**
	 * Attaches a static alerts tag to JSON telemetry messages
	 */
	private static final long serialVersionUID = 7759427661169094065L;
	private JSONObject _static_tag_message;
	JSONArray ja = new JSONArray();

	/**
	 * 
	 * @param static_tag_message
	 *            static alerts tag to attach to the message as a JSON
	 */
	@SuppressWarnings("unchecked")
	public StaticAllTagger(JSONObject static_tag_message) {
		_static_tag_message = static_tag_message;
		ja.add(_static_tag_message);
	}

	/**
	 * @param raw_message
	 *            message to tag
	 */
	public JSONArray tag(JSONObject raw_message) {

		return ja;
	}

}
