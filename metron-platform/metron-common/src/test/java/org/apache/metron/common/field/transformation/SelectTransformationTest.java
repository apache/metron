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

import java.util.HashMap;

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Iterables;

public class SelectTransformationTest {
	
	public static String selectSingleFieldConfig = "{ \"fieldTransformations\" : [{\"output\": [\"field1\"] , \"transformation\": \"SELECT\" } ] }";

	public static String selectMultiFieldConfig = "{ \"fieldTransformations\" : [{\"output\": [\"field1\", \"field2\"] , \"transformation\": \"SELECT\" } ] }";

	@Test
	public void testSingleFieldReturned() throws Exception {
		SensorParserConfig sensorConfig = SensorParserConfig.fromBytes(Bytes.toBytes(selectSingleFieldConfig));
		FieldTransformer handler = Iterables.getFirst(sensorConfig.getFieldTransformations(), null);
		JSONObject input = new JSONObject(new HashMap<String, Object>() {
			{
				put("field1", "foo");
				put("field2", "bar");
			}
		});
		handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());

		Assert.assertTrue(input.containsKey("field1"));
		Assert.assertFalse(input.containsKey("field2"));
		Assert.assertEquals(1, input.size());
	}

	@Test
	public void testMulitpleFieldReturned() throws Exception {
		SensorParserConfig sensorConfig = SensorParserConfig.fromBytes(Bytes.toBytes(selectMultiFieldConfig));
		FieldTransformer handler = Iterables.getFirst(sensorConfig.getFieldTransformations(), null);
		JSONObject input = new JSONObject(new HashMap<String, Object>() {
			{
				put("field1", "foo");
				put("field2", "bar");
				put("field3", "bar2");
			}
		});
		handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());

		Assert.assertTrue(input.containsKey("field1"));
		Assert.assertTrue(input.containsKey("field2"));
		Assert.assertFalse(input.containsKey("field3"));
		Assert.assertEquals(2, input.size());
	}
	
	@Test
	public void testPreserveSystemFields() throws Exception { 
		SensorParserConfig sensorConfig = SensorParserConfig.fromBytes(Bytes.toBytes(selectSingleFieldConfig));
		FieldTransformer handler = Iterables.getFirst(sensorConfig.getFieldTransformations(), null);
		JSONObject input = new JSONObject(new HashMap<String, Object>() {
			{
				put("timestamp", 12345);
				put("original_string", "foo,bar");
				put("source.type", "test");
				put("field1", "foo");
				put("field2", "bar");
			}
		});
		handler.transformAndUpdate(input, Context.EMPTY_CONTEXT());
		
		Assert.assertTrue(input.containsKey("timestamp"));
		Assert.assertTrue(input.containsKey("original_string"));
		Assert.assertTrue(input.containsKey("source.type"));
		Assert.assertTrue(input.containsKey("field1"));
		Assert.assertFalse(input.containsKey("field2"));
		Assert.assertEquals(4, input.size());
	}

}
