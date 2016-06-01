package org.apache.metron.parsers.checkpointsyslog;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BasicCheckPointSyslogParser extends BasicParser {

    private static final long serialVersionUID = 4860659629055777358L;
    private static final Logger _LOG = LoggerFactory.getLogger
            (BasicCheckPointSyslogParser.class);

    @Override
    public void init() {

    }

    @SuppressWarnings({"unchecked", "unused"})
    public List<JSONObject> parse(byte[] msg) {
        JSONObject outputMessage = new JSONObject();
        String toParse = "";
        List<JSONObject> messages = new ArrayList<>();
        try {
            toParse = new String(msg, "UTF-8");
            _LOG.debug("Received message: " + toParse);

            parseMessage(toParse, outputMessage);

            outputMessage.put("original_string", toParse);
            messages.add(outputMessage);
            return messages;
        } catch (Exception e) {
            _LOG.error("Failed to parse: " + toParse);
            return null;
        }
    }

    private void parseMessage(String message, JSONObject outputMessage) {
        int indexOfColon = message.indexOf(":");

        String processDataWithPriority = message.substring(0, indexOfColon).trim();

        String[] tokens = processDataWithPriority.split(">");

        outputMessage.put("priority", tokens[0].substring(1));

        String processData = tokens[1];
        int indexOfSquareBracketOpen = processData.indexOf("[");
        int indexOfSquareBracketClose = processData.indexOf("]");

        if (indexOfSquareBracketOpen > -1) {
            outputMessage.put("processName", processData.substring(0,indexOfSquareBracketOpen));
            outputMessage.put("processId", processData.substring(indexOfSquareBracketOpen+1, indexOfSquareBracketClose));
        }
        else {
            outputMessage.put("processName", processData);
        }

        outputMessage.put("message", message.substring(indexOfColon + 1).trim());
        outputMessage.put("timestamp", System.currentTimeMillis());

        removeEmptyFields(outputMessage);
    }

    @SuppressWarnings("unchecked")
    private void removeEmptyFields(JSONObject json) {
        Iterator<Object> keyIter = json.keySet().iterator();
        while (keyIter.hasNext()) {
            Object key = keyIter.next();
            Object value = json.get(key);
            if (null == value || "".equals(value.toString())) {
                keyIter.remove();
            }
        }
    }

    @Override
    public void configure(Map<String, Object> config) {

    }
}

