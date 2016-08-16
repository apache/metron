package org.apache.metron.parsers.asa;

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;
import org.apache.metron.common.Constants;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BasicAsaParser implements MessageParser<JSONObject>, Serializable {

    protected static final Logger LOG = LoggerFactory.getLogger(BasicAsaParser.class);

    private Grok asaGrok;

    @Override
    public void configure(Map<String, Object> config) {

    }

    @Override
    public void init() {
        asaGrok = new Grok();
        InputStream patternStream = this.getClass().getClassLoader().getResourceAsStream("patterns/asa");
        try {
            asaGrok.addPatternFromReader(new InputStreamReader(patternStream));
        } catch (GrokException e) {
            e.printStackTrace();
        }
        LOG.info("[Metron] Cisco ASA Parser Initialized");
    }

    @Override
    public List<JSONObject> parse(byte[] rawMessage) {
        String pattern = "";
        JSONObject metronJson = new JSONObject();
        List<JSONObject> messages = new ArrayList<>();
        try {
            String logLine = new String(rawMessage, "UTF-8");
            LOG.debug("[Metron] Started parsing message: " + logLine);

            pattern = asaGrok.discover(logLine);
            //System.out.println("Discovered Pattern: " + pattern);
            LOG.debug("[Metron] Grok discovered message pattern: " + pattern);

            asaGrok.compile(pattern);
            Match gm = asaGrok.match(logLine);
            gm.captures();
            Map<String, Object> grokJson = gm.toMap();
            //System.out.println(gm.toJson(true));
            LOG.debug("[Metron] Grok returned matches: " + gm.toJson());

            metronJson.put(Constants.Fields.ORIGINAL.getName(), logLine);
            metronJson.put(Constants.Fields.TIMESTAMP.getName(), convertToEpoch((String) grokJson.get("CISCOTIMESTAMP")));
            metronJson.put(Constants.Fields.SRC_ADDR.getName(), grokJson.get("src_ip"));
            metronJson.put(Constants.Fields.SRC_PORT.getName(), grokJson.get("src_port"));
            metronJson.put(Constants.Fields.DST_ADDR.getName(), grokJson.get("dst_ip"));
            metronJson.put(Constants.Fields.DST_PORT.getName(), grokJson.get("dst_port"));
            metronJson.put(Constants.Fields.PROTOCOL.getName(), normalizeString((String) grokJson.get("protocol"))); //TODO: Handle null values
            metronJson.put("action", normalizeString((String) grokJson.get("action")));
            metronJson.put("ciscotag", grokJson.get("CISCOTAG"));
            metronJson.put("syslog_severity", getSeverityFromPriority((int) grokJson.get("syslog_pri")));
            metronJson.put("syslog_facility", getFacilityFromPriority((int) grokJson.get("syslog_pri")));
            LOG.debug("[Metron] Final parsed message: " + metronJson.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        messages.add(metronJson);
        return messages;
    }

    @Override
    public boolean validate(JSONObject message) {
        return false;
    }

    private long convertToEpoch(String logTimestamp) {
        ZonedDateTime timestamp = ZonedDateTime.parse(logTimestamp, DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss").withZone(ZoneOffset.UTC));
        return timestamp.toEpochSecond();
    }

    private String normalizeString(String str) {
        return str.toLowerCase();
    }

    private int getSeverityFromPriority(int priority) {
        return priority & 0x07; //TODO: Add map to string representation
    }

    private int getFacilityFromPriority(int priority) {
        return priority >> 3; //TODO: Add map to string representation
    }
}
