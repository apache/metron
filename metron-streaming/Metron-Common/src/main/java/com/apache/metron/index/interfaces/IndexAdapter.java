package com.apache.metron.index.interfaces;

import java.util.Map;

import org.json.simple.JSONObject;

public interface IndexAdapter {

	boolean initializeConnection(String ip, int port, String cluster_name,
			String index_name, String document_name, int bulk, String date_format) throws Exception;

	int bulkIndex(JSONObject raw_message);

	void setOptionalSettings(Map<String, String> settings);
}
