/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opensoc.json.serialization;

import static com.opensoc.json.serialization.JSONDecoderHelper.getObject;
import static com.opensoc.json.serialization.JSONEncoderHelper.putBoolean;
import static com.opensoc.json.serialization.JSONEncoderHelper.putNull;
import static com.opensoc.json.serialization.JSONEncoderHelper.putNumber;
import static com.opensoc.json.serialization.JSONEncoderHelper.putString;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import kafka.serializer.Decoder;
import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * JSON Serailization class for kafka. Implements kafka Encoder and Decoder
 * String, JSONObject, Number, Boolean,JSONObject.NULL JSONArray
 * 
 * @author kiran
 * 
 */

public class JSONKafkaSerializer implements Encoder<JSONObject>,
		Decoder<JSONObject> {

	// Object ID's for different types
	public static final byte StringID = 1;
	public static final byte JSONObjectID = 2;
	public static final byte NumberID = 3;
	public static final byte BooleanID = 4;
	public static final byte NULLID = 5;
	public static final byte JSONArrayID = 6;

	public JSONKafkaSerializer() {
		// Blank constructor needed by Storm

	}

	public JSONKafkaSerializer(VerifiableProperties props) {
		// Do Nothing. constructor needed by Storm
	}

	/*
	 * Main Method for unit testing
	 */
	public static void main(String args[]) throws IOException {

		//String Input = "/home/kiran/git/opensoc-streaming/OpenSOC-Common/BroExampleOutput";
		String Input = "/tmp/test";

		BufferedReader reader = new BufferedReader(new FileReader(Input));

		// String jsonString =
		// "{\"dns\":{\"ts\":[14.0,12,\"kiran\"],\"uid\":\"abullis@mail.csuchico.edu\",\"id.orig_h\":\"10.122.196.204\", \"endval\":null}}";
		String jsonString ="";// reader.readLine();
		JSONParser parser = new JSONParser();
		JSONObject json = null;
		int count = 1;

		if (args.length > 0)
			count = Integer.parseInt(args[0]);

		//while ((jsonString = reader.readLine()) != null) 
		jsonString = reader.readLine();
		{
			try {
				json = (JSONObject) parser.parse(jsonString);
				System.out.println(json);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String jsonString2 = null;

			JSONKafkaSerializer ser = new JSONKafkaSerializer();

			for (int i = 0; i < count; i++) {
				byte[] bytes = ser.toBytes(json);

				jsonString2 = ((JSONObject)ser.fromBytes(bytes)).toJSONString();
			}
			System.out.println((jsonString2));
			System.out
					.println(jsonString2.equalsIgnoreCase(json.toJSONString()));
		}

	}

	@SuppressWarnings("unchecked")
	public JSONObject fromBytes(byte[] input) {

		ByteArrayInputStream inputBuffer = new ByteArrayInputStream(input);
		DataInputStream data = new DataInputStream(inputBuffer);

		JSONObject output = new JSONObject();

		try {
			int mapSize = data.readInt();

			for (int i = 0; i < mapSize; i++) {
				String key = (String) getObject(data);
				// System.out.println("Key Found"+ key);
				Object val = getObject(data);
				output.put(key, val);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return output;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject fromBytes1(DataInputStream data) {

		//ByteArrayInputStream inputBuffer = new ByteArrayInputStream(input);
		//DataInputStream data = new DataInputStream(inputBuffer);

		JSONObject output = new JSONObject();

		try {
			int mapSize = data.readInt();

			for (int i = 0; i < mapSize; i++) {
				String key = (String) getObject(data);
				// System.out.println("Key Found"+ key);
				Object val = getObject(data);
				output.put(key, val);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return output;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public byte[] toBytes(JSONObject input) {

		ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(outputBuffer);

		Iterator it = input.entrySet().iterator();
		try {

			// write num of entries into output. 
			//each KV pair is counted as an entry
			data.writeInt(input.size());

			// Write every single entry in hashmap
			//Assuming key to be String.
			while (it.hasNext()) {
				Map.Entry<String, Object> entry = (Entry<String, Object>) it
						.next();
				putObject(data, entry.getKey());
				putObject(data, entry.getValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return outputBuffer.toByteArray();
	}

	private void putObject(DataOutputStream data, Object value)
			throws IOException {

		//Check object type and invoke appropriate method
		if (value instanceof JSONObject) {
			putJSON(data, (JSONObject) value);
			return;

		}

		if (value instanceof String) {
			putString(data, (String) value);
			return;
		}

		if (value instanceof Number) {
			putNumber(data, (Number) value);
			return;
		}

		if (value instanceof Boolean) {
			putBoolean(data, (Boolean) value);
			return;
		}

		if (value == null) {
			putNull(data, value);
			return;
		}

		if (value instanceof JSONArray) {
			putArray(data, (JSONArray) value);
			return;
		}

	}

	private void putJSON(DataOutputStream data, JSONObject value)
			throws IOException {

		// JSON ID is 2
		data.writeByte(JSONKafkaSerializer.JSONObjectID);
		data.write(toBytes(value));

	}

	public void putArray(DataOutputStream data, JSONArray array)
			throws IOException {

		data.writeByte(JSONKafkaSerializer.JSONArrayID);

		data.writeInt(array.size());

		for (Object o : array)
			putObject(data, o);

	}


	

}