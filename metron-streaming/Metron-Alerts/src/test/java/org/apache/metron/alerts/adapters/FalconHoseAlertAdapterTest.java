package org.apache.metron.alerts.adapters;

import java.util.Map;

import org.json.simple.JSONObject;

import org.apache.metron.test.AbstractConfigTest;
import org.apache.metron.alerts.adapters.FalconHoseAlertAdapter;

 /**
 * <ul>
 * <li>Title: FalconHoseAlertAdapterTest</li>
 * <li>Description: Tests for FalconHoseAlertAdapter</li>
 * <li>Created: January 20, 2016</li>
 * </ul>
 */
public class FalconHoseAlertAdapterTest extends AbstractConfigTest {

    public FalconHoseAlertAdapterTest(String name) {
        super(name);
    }

    public void testInitializeAdapter() {
        FalconHoseAlertAdapter adapter = new FalconHoseAlertAdapter();
        boolean initialized = adapter.initialize();
        assertTrue(initialized);
    }

    public void testRefresh() throws Exception {
        FalconHoseAlertAdapter adapter = new FalconHoseAlertAdapter();
        boolean refreshed = adapter.refresh();
        assertFalse(refreshed);
    }
    
    public void testContainsAlertId(){
        FalconHoseAlertAdapter adapter = new FalconHoseAlertAdapter();
        boolean containsAlert = adapter.containsAlertId("test");
        assertFalse(containsAlert);
    }

    public void testAlertNoMessage() {
        FalconHoseAlertAdapter adapter = new FalconHoseAlertAdapter();

        JSONObject message = new JSONObject();

        Map<String, JSONObject> alerts = adapter.alert(message);

        assertEquals(0, alerts.size());
    }

    public void testAlertEmptyMessage() {
        FalconHoseAlertAdapter adapter = new FalconHoseAlertAdapter();

        JSONObject internalMessage = new JSONObject();
        JSONObject message = new JSONObject();
        message.put("message", internalMessage);

        Map<String, JSONObject> alerts = adapter.alert(message);

        assertEquals(1, alerts.size());

        String alertId = alerts.keySet().iterator().next();
        JSONObject alert = alerts.get(alertId);
        assertEquals(alertId, alert.get("alert_id"));
        assertEquals("unknown", alert.get("designated_host"));
        assertEquals("", alert.get("description"));
        assertEquals("MED", alert.get("priority"));
    }

    public void testAlert() {
        FalconHoseAlertAdapter adapter = new FalconHoseAlertAdapter();

        JSONObject internalMessage = new JSONObject();
        internalMessage.put("ip_dst_addr", "192.168.0.50");
        internalMessage.put("original_string", "this is original");
        internalMessage.put("SeverityName", "High");
        JSONObject message = new JSONObject();
        message.put("message", internalMessage);

        Map<String, JSONObject> alerts = adapter.alert(message);

        assertEquals(1, alerts.size());
        
        String alertId = alerts.keySet().iterator().next();
        JSONObject alert = alerts.get(alertId);
        assertEquals(alertId, alert.get("alert_id"));
        assertEquals("192.168.0.50", alert.get("designated_host"));
        assertEquals("this is original", alert.get("description"));
        assertEquals("High", alert.get("priority"));
    }
 
}

