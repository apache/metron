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
package org.apache.metron.parsers.filters;

import org.apache.commons.configuration.Configuration;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BroMessageFilter implements MessageFilter<JSONObject>,
				Serializable {

	/**
	 * Filter protocols based on whitelists and blacklists
	 */
	
	private static final long serialVersionUID = -3824683649114625033L;
	private String _key;
	private final Set<String> _known_protocols;

	 /**
	 * @param  conf  Commons configuration for reading properties files
	 * @param  key Key in a JSON mesage where the protocol field is located
	 */
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BroMessageFilter(Configuration conf, String key) {
		_key = key;
		_known_protocols = new HashSet<>();
		List known_protocols = conf.getList("source.known.protocols");
		_known_protocols.addAll(known_protocols);
	}

	 /**
	 * @param  message  JSON representation of a message with a protocol field
	 * @return      False if message if filtered and True if message is not filtered
	 */
	
	public boolean emitTuple(JSONObject message) {
		String protocol = (String) message.get(_key);
		return _known_protocols.contains(protocol);
	}
}
