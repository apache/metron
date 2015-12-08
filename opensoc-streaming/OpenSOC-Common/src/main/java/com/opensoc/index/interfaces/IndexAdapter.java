package com.opensoc.index.interfaces;

import org.json.simple.JSONObject;

public interface IndexAdapter {

	boolean initializeConnection(String ip, int port, String cluster_name,
			String index_name, String document_name, int bulk) throws Exception;

	int bulkIndex(JSONObject raw_message);
}
