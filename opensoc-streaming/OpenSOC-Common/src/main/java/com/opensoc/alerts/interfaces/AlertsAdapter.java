package com.opensoc.alerts.interfaces;

import java.util.Map;

import org.json.simple.JSONObject;

public interface AlertsAdapter {

	boolean initialize();

	boolean refresh() throws Exception;

	Map<String, JSONObject> alert(JSONObject raw_message);

	boolean containsAlertId(String alert);
}
