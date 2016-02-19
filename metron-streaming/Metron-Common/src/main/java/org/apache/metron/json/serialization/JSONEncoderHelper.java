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

package org.apache.metron.json.serialization;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONObject;

/**
 * Helper class used for encoding objects into byte arrays 
 *
 * @author kiran
 * 
 */
public class JSONEncoderHelper {

	public static void putNull(DataOutputStream data, Object value)
			throws IOException {
		// TODO Auto-generated method stub
		data.writeByte(JSONKafkaSerializer.NULLID);

	}

	public static void putBoolean(DataOutputStream data, Boolean value)
			throws IOException {
		// TODO Auto-generated method stub
		data.writeByte(JSONKafkaSerializer.BooleanID);
		data.writeBoolean(value);

	}

	public static void putNumber(DataOutputStream data, Number value)
			throws IOException {
		// TODO Auto-generated method stub
		data.writeByte(JSONKafkaSerializer.NumberID);
		if (value instanceof Double) {
			data.writeByte(0);
			data.writeDouble((Double) value);
			return;
		}
		data.writeByte(1);
		data.writeLong((Long) value);

	}

	public static void putString(DataOutputStream data, String str)
			throws IOException {
		// String ID is 1
		data.writeByte(JSONKafkaSerializer.StringID);
		data.writeInt(str.length());
		data.write(str.getBytes());

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static JSONObject getJSON(Configuration config) {

		JSONObject output = new JSONObject();

		if (!config.isEmpty()) {
			Iterator it = config.getKeys();
			while (it.hasNext()) {
				String k = (String) it.next();
				// noinspection unchecked
				String v = (String) config.getProperty(k);
				output.put(k, v);
			}
		}
		return output;
	}

}
