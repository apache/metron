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

package org.apache.metron.enrichment.adapters.host;

import java.util.Map;

import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class HostFromPropertiesFileAdapter extends AbstractHostAdapter {
	
	Map<String, JSONObject> _known_hosts;
	
	public HostFromPropertiesFileAdapter(Map<String, JSONObject> known_hosts)
	{
		_known_hosts = known_hosts;
	}

	@Override
	public boolean initializeAdapter()
	{
		
		if(_known_hosts.size() > 0)
			return true;
		else
			return false;
	}

	@Override
	public void logAccess(String value) {

	}

	@SuppressWarnings("unchecked")
    @Override
	public JSONObject enrich(String metadata) {
		
		
		if(!_known_hosts.containsKey(metadata))
			return new JSONObject();
		
		JSONObject enrichment = new JSONObject();
		enrichment.put("known_info", (JSONObject) _known_hosts.get(metadata));
		return enrichment;
	}
	
	
}
