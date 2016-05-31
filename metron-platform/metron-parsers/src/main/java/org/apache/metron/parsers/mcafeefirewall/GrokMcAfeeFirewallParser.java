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

package org.apache.metron.parsers.mcafeefirewall;

import org.apache.metron.parsers.GrokParser;
import org.json.simple.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;

public class GrokMcAfeeFirewallParser extends GrokParser {



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

	}

	@SuppressWarnings("unchecked")
	private void removeEmptyFields(JSONObject json) {
		Iterator<Object> keyIter = json.keySet().iterator();
		while (keyIter.hasNext()) {
			Object key = keyIter.next();
			Object value = json.get(key);
			if (null == value || "".equals(value.toString()) || "N/A".equals(value.toString())) {
				keyIter.remove();
			}
		}
	}

}
