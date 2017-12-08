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

package org.apache.metron.common.field.transformation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.metron.stellar.dsl.Context;

public class SelectTransformation implements FieldTransformation {

	private static final List<String> systemFields = Arrays.asList("timestamp", "original_string", "source.type");

	@Override
	public Map<String, Object> map(Map<String, Object> input, List<String> outputField,
			LinkedHashMap<String, Object> fieldMappingConfig, Context context, Map<String, Object>... sensorConfig) {
		// note that we have to set the value to null for any field not in the output
		// list, since FieldTransformer will otherwise put them back again from the
		// original output.

		// note, this cannot be implemented with streams because HashMap merge guards
		// against null values

		HashMap<String, Object> output = new HashMap<String, Object>();
		for (Entry<String, Object> e : input.entrySet()) {
			if (outputField.contains(e.getKey())) {
				output.put(e.getKey(), e.getValue());
			} else {
				if (!systemFields.contains(e.getKey())) {
					output.put(e.getKey(), null);
				}
			}
		}
		return output;
	}

}
