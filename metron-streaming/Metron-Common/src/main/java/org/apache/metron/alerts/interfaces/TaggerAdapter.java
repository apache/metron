package org.apache.metron.alerts.interfaces;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public interface TaggerAdapter {

	JSONArray tag(JSONObject raw_message);
}
