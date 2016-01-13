package com.apache.metron.alerts.interfaces;

import org.json.simple.JSONObject;

public interface AlertsInterface {

	public JSONObject getContent();
	public void setContent(JSONObject content);
	public String getUuid();
	public void setUuid(String uuid);
}
