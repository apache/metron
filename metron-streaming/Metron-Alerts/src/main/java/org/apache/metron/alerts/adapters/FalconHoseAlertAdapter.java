package org.apache.metron.alerts.adapters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.apache.log4j.Logger;
import org.apache.metron.alerts.interfaces.AlertsAdapter;

/**
 * All messages from FalconHose are considered alerts, so this adapter
 * simply constructs a normalized alert object for every message that comes
 * in.
 */
public class FalconHoseAlertAdapter implements AlertsAdapter, Serializable {

	protected static final Logger LOG = Logger
			.getLogger(FalconHoseAlertAdapter.class);

    public FalconHoseAlertAdapter() {
    }

    public FalconHoseAlertAdapter(Map<String, String> config) {
    }

    @Override
    public boolean initialize() {
        return true;
    }

    @Override
    public boolean refresh() throws Exception {
        return false;
    }

    @Override
    public Map<String, JSONObject> alert(JSONObject rawMessage) {
        Map<String, JSONObject> alerts = new HashMap<String, JSONObject>();

        if (!rawMessage.containsKey("message")) {
            return alerts;
        }

        JSONObject content = (JSONObject)rawMessage.get("message");

        String host = "unknown";
        if (content.containsKey("ip_dst_addr")) {
            host = content.get("ip_dst_addr").toString();
        }

        String description = "";
        if (content.containsKey("original_string")) {
            description = content.get("original_string").toString();
        }

        String alertId = generateAlertId();

        JSONObject alert = new JSONObject();

        alert.put("alert_id", alertId);
        alert.put("designated_host", host);
        alert.put("description", description);

        if (content.containsKey("SeverityName")) {
            alert.put("priority", content.get("SeverityName").toString());
        } else {
            alert.put("priority", "MED");
        }

        alerts.put(alertId, alert);

        return alerts;
    }

    @Override
    public boolean containsAlertId(String alert) {
        return false;
    }

	protected String generateAlertId() {
		String new_UUID = System.currentTimeMillis() + "-" + UUID.randomUUID();
		return new_UUID;

	}
}
