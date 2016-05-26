/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.parsers.paloalto;


import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasicPaloAltoFirewallParser extends BasicParser {

    private static final Logger _LOG = LoggerFactory.getLogger
            (BasicPaloAltoFirewallParser.class);

    private static final long serialVersionUID = 3147090149725343999L;
    private static final String[] TRAFFIC_FIELDS = {
            "receiveTime",
            "serialNumber",
            "type",
            "subtype",
            "futureUse2",
            "generatedTime",
            "ipSrcAddr",
            "ipDstAddr",
            "natSourceIp",
            "natDestinationIp",
            "ruleName",
            "srcUserName",
            "dstUserName",
            "application",
            "virtualSystem",
            "sourceZone",
            "destinationZone",
            "ingressInterface",
            "egressInterface",
            "logForwardingProfile",
            "futureUse3",
            "sessionId",
            "repeatCount",
            "ipSrcPort",
            "ipDstPort",
            "natSourcePort",
            "natDestinationPort",
            "flags",
            "protocol",
            "action",
            "bytes",
            "bytesSent",
            "bytesReceived",
            "packets",
            "startTime",
            "elapsedTime",
            "category",
            "futureUse4",
            "sequenceNumber",
            "actionFlags",
            "sourceLocation",
            "destinationLocation",
            "futureUse5",
            "packetsSent",
            "packetsReceived",
            "sessionEndReason",
            "deviceGroupHierarchyLevel1",
            "deviceGroupHierarchyLevel2",
            "deviceGroupHierarchyLevel3",
            "deviceGroupHierarchyLevel4",
            "virtualSystemName",
            "deviceName",
            "actionSource"};

    private static final String[] THREAT_FIELDS = {
            "receiveTime",
            "serialNumber",
            "type",
            "subtype",
            "futureUse2",
            "generatedTime",
            "ipSrcAddr",
            "ipDstAddr",
            "natSourceIp",
            "natDestinationIp",
            "ruleName",
            "srcUserName",
            "dstUserName",
            "application",
            "virtualSystem",
            "sourceZone",
            "destinationZone",
            "ingressInterface",
            "egressInterface",
            "logForwardingProfile",
            "futureUse3",
            "sessionId",
            "repeatCount",
            "ipSrcPort",
            "ipDstPort",
            "natSourcePort",
            "natDestinationPort",
            "flags",
            "protocol",
            "action",
            "miscellaneous",
            "threatId",
            "category",
            "severity",
            "direction",
            "sequenceNumber",
            "actionFlags",
            "sourceLocation",
            "destinationLocation",
            "futureUse4",
            "contentType",
            "pcapId",
            "fileDigest",
            "cloud",
            "urlIndex",
            "userAgent",
            "fileType",
            "xForwardedFor",
            "referrer",
            "sender",
            "subject",
            "recipient",
            "reportId",
            "deviceGroupHierarchyLevel1",
            "deviceGroupHierarchyLevel2",
            "deviceGroupHierarchyLevel3",
            "deviceGroupHierarchyLevel4",
            "virtualSystemName",
            "deviceName",
            "futureUse5"};

    private static final String[] CONFIG_FIELDS = {
            "receiveTime",
            "serialNumber",
            "type",
            "subtype",
            "futureUse2",
            "generatedTime",
            "host",
            "virtualSystem",
            "command",
            "admin",
            "client",
            "result",
            "configurationPath",
            "sequenceNumber",
            "actionFlags",
            "deviceGroupHierarchyLevel1",
            "deviceGroupHierarchyLevel2",
            "deviceGroupHierarchyLevel3",
            "deviceGroupHierarchyLevel4",
            "virtualSystemName",
            "deviceName"};

    private static final String[] SYSTEM_FIELDS = {
            "receiveTime",
            "serialNumber",
            "type",
            "subtype",
            "futureUse2",
            "generatedTime",
            "virtualSystem",
            "eventId",
            "object",
            "futureUse3",
            "futureUse4",
            "module",
            "severity",
            "description",
            "sequenceNumber",
            "actionFlags",
            "deviceGroupHierarchyLevel1",
            "deviceGroupHierarchyLevel2",
            "deviceGroupHierarchyLevel3",
            "deviceGroupHierarchyLevel4",
            "virtualSystemName",
            "deviceName"};


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

    @SuppressWarnings("unchecked")
    private void parseMessage(String message, JSONObject outputMessage) {

        ArrayList<String> tokens = new ArrayList<>(Arrays.asList(message.split(",")));
        String lastValue = message.substring(message.lastIndexOf(",")+1);
        if (lastValue == "")
            tokens.add(lastValue);
        //populate common objects
        parseFirstField(tokens.get(0), outputMessage);

        String type = tokens.get(3).trim();
        switch(type) {
            case "TRAFFIC": parseTuple(tokens, outputMessage, TRAFFIC_FIELDS);
                break;
            case "THREAT":  parseTuple(tokens, outputMessage, THREAT_FIELDS);
                break;
            case "CONFIG":  parseTuple(tokens, outputMessage, CONFIG_FIELDS);
                break;
            case "SYSTEM":  parseTuple(tokens, outputMessage, SYSTEM_FIELDS);
                break;
        }
    }

    private void parseTuple(ArrayList<String> tokens, JSONObject outputMessage, String[] fields) {
        int numFields = fields.length;
        int numTokens = tokens.size() - 1;
        int count;
        for(count = 0; count < numTokens; count++)
            outputMessage.put(fields[count],tokens.get(count+1));

        for(; count < numFields; count++)
            outputMessage.put(fields[count],"");
        removeEmptyFields(outputMessage);
    }

    private void parseFirstField(String firstField, JSONObject outputMessage) {
        //split first field by empty space
        String[] tokens = firstField.split("\\s+");
        //get priority inside of < >
        Pattern pattern = Pattern.compile("<.*>");
        Matcher matcher = pattern.matcher(tokens[0]);
        //add priority
        if(matcher.find())
        {
            String priorityNum = matcher.group(0);
            outputMessage.put("priority", priorityNum.substring(1, priorityNum.length()-1));
        }
        //add date
        String tempDate = tokens[0].substring(tokens[0].indexOf(">") +1) + " " + tokens[1] + " " + tokens[2];
        outputMessage.put("timestamp", this.formatTimestamp(tempDate));

        //add hostname
        outputMessage.put("hostname", tokens[3]);
        //add future use
        outputMessage.put("futureUse", tokens[4]);
    }

    protected long formatTimestamp(Object value) {
        long epochTimestamp = System.currentTimeMillis();
        if (value != null) {
            try {
                epochTimestamp = toEpoch(Calendar.getInstance().get(Calendar.YEAR)  + " " + value);
            } catch (java.text.ParseException e) {
                //default to current time
            }
        }
        return epochTimestamp;
    }

    protected long toEpoch(String datetime) throws java.text.ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Parser converting timestamp to epoch: " + datetime);
        }

        dateFormat.setTimeZone(timeZone);
        Date date = dateFormat.parse(datetime);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Parser converted timestamp to epoch: " + date);
        }

        return date.getTime();
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

}
