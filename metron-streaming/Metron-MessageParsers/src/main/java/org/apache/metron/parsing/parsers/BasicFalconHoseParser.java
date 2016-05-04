package org.apache.metron.parsing.parsers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.metron.tldextractor.BasicTldExtractor;

@SuppressWarnings("serial")
public class BasicFalconHoseParser extends AbstractParser {

    protected static final Logger _LOG = LoggerFactory.getLogger(BasicFalconHoseParser.class);
    private JSONCleaner cleaner = new JSONCleaner();

    @SuppressWarnings("unchecked")
    public JSONObject parse(byte[] msg) {
        _LOG.trace("[OpenSOC] Starting to parse incoming message");

        String rawMessage = null;

        try {

            rawMessage = new String(msg, "UTF-8");
            _LOG.trace("[OpenSOC] Received message: " + rawMessage);

            JSONObject cleanedMessage = cleaner.Clean(rawMessage);
            _LOG.debug("[OpenSOC] Cleaned message: " + rawMessage);

            if (cleanedMessage == null || cleanedMessage.isEmpty()) {
                throw new Exception("Unable to clean message: " + rawMessage);
            }

            JSONObject payload = (JSONObject)cleanedMessage.get("event");

            if (payload == null) {
                throw new Exception("Unable to retrieve payload for message: "
                        + rawMessage);
            }

            String originalString = "";
            for (Object k : payload.keySet()) {
                originalString += " " + k.toString() + ":" + payload.get(k).toString();
            }
            payload.put("original_string", originalString);

            if (payload.containsKey("LoginTime")) {
                Long ts = Long.parseLong(payload.remove("LoginTime").toString());
                payload.put("timestamp", ts * 1000);
                _LOG.trace("[OpenSOC] Added ts to: " + payload);
            } else if (payload.containsKey("ProcessStartTime")) {
                Long ts = Long.parseLong(payload.remove("ProcessStartTime").toString());
                payload.put("timestamp", ts);
                _LOG.trace("[OpenSOC] Added ts to: " + payload);
            } else {
                payload.put("timestamp", System.currentTimeMillis());
            }

            if (payload.containsKey("UserIp")) {
                String ip = payload.remove("UserIp").toString();
                payload.put("ip_src_addr", ip);
                payload.put("ip_dst_addr", ip);
                payload.put("ip_src_port", 0);
                payload.put("ip_dst_port", 0);
            } else if (payload.containsKey("ComputerName")) {
                String name = payload.remove("ComputerName").toString();
                payload.put("ip_src_addr", name);
                payload.put("ip_dst_addr", name);
                payload.put("ip_src_port", 0);
                payload.put("ip_dst_port", 0);
            }

            _LOG.trace("[OpenSOC] Inner message: " + payload);

            payload.put("protocol", "http");
            _LOG.debug("[OpenSOC] Returning parsed message: " + payload);

            return payload;
        } catch (Exception e) {
            _LOG.error("Unable to Parse Message: " + rawMessage);
            _LOG.error(e.getMessage(), e);
            return null;
        }

    }


}
