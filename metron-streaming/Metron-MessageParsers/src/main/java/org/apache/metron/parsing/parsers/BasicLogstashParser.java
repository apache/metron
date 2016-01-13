package org.apache.metron.parsing.parsers;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BasicLogstashParser extends AbstractParser {

	@Override
	public JSONObject parse(byte[] raw_message) {
		
		try {
			
			/*
			 * We need to create a new JSONParser each time because its 
			 * not serializable and the parser is created on the storm nimbus
			 * node, then transfered to the workers.
			 */
			JSONParser jsonParser = new JSONParser();
			String rawString = new String(raw_message, "UTF-8");
			JSONObject rawJson = (JSONObject) jsonParser.parse(rawString);
			
			// remove logstash meta fields
			rawJson.remove("@version");
			rawJson.remove("type");
			rawJson.remove("host");
			rawJson.remove("tags");
			
			// rename other keys
			rawJson = mutate(rawJson, "message", "original_string");
			rawJson = mutate(rawJson, "src_ip", "ip_src_addr");
			rawJson = mutate(rawJson, "dst_ip", "ip_dst_addr");
			rawJson = mutate(rawJson, "src_port", "ip_src_port");
			rawJson = mutate(rawJson, "dst_port", "ip_dst_port");
			rawJson = mutate(rawJson, "src_ip", "ip_src_addr");
			
			// convert timestamp to milli since epoch
			rawJson.put("timestamp", LogstashToEpoch((String) rawJson.remove("@timestamp")));

			return rawJson;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}	
	}
	
	private JSONObject mutate(JSONObject json, String oldKey, String newKey) {
		if (json.containsKey(oldKey)) {
			json.put(newKey, json.remove(oldKey));
		}	
		return json;
	}
	
	private long LogstashToEpoch(String timestamp) throws java.text.ParseException {
		SimpleDateFormat logstashDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		return logstashDateFormat.parse(timestamp).getTime();
		
	}

	

}
