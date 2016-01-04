package com.opensoc.dataloads.interfaces;

import java.util.Iterator;
import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONObject;

public interface ThreatIntelSource extends Iterator<JSONObject> {

	void initializeSource(Configuration config);
	void cleanupSource();
}
