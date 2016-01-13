package com.apache.metron.tagging.adapters;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public interface TaggerAdapter {

	JSONArray tag(JSONObject raw_message);
}
