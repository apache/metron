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

package org.apache.metron.parsers.checkpointfirewall;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasicCheckPointFirewallParser extends BasicParser {

    private static final long serialVersionUID = 4860548518055777358L;

    private static final Logger LOGGER = LoggerFactory.getLogger
            (BasicCheckPointFirewallParser.class);

    private static final String[] LOG_FIELDS = {
            "action",
            "origin",
            "interfaceDirection",
            "interfaceName",
            "sourceIp",
            "sourcePort",
            "destinationIp",
            "destinationPort",
            "service",
            "protocol",
            "ruleNumber",
            "tbd1",
            "tbd2",
            "tbd3",
            "tbd4",
            "tbd5",
            "tbd6",
            "tbd7",
            "tbd8",
            "messageType1",
            "tbd9",
            "tbd10",
            "tbd11",
            "tbd12",
            "tbd13",
            "tbd14",
            "tbd15",
            "messageType2",
            "tbd16",
            "tbd17",
            "tbd18",
            "tbd19",
            "tbd20",
            "tbd21",
            "eventDate",
            "tbd22",
            "eventSource"};

    @Override
    public void init() {

    }

    @SuppressWarnings({"unchecked", "unused"})
    public List<JSONObject> parse(byte[] msg) throws Exception {
        JSONObject outputMessage = new JSONObject();
        String toParse = "";
        List<JSONObject> messages = new ArrayList<>();
        try {
            toParse = new String(msg, "UTF-8");
            LOGGER.debug("Received message: " + toParse);

            parseMessage(toParse, outputMessage);

            outputMessage.put("original_string", toParse);
            messages.add(outputMessage);
            return messages;
        } catch (Exception e) {
            LOGGER.error("Failed to parse: " + toParse, e);
            throw e;
        }
    }

    private void parseMessage(String message, JSONObject outputMessage) {
        String patternToMatch = "(%CHKPNT-)\\d(-)\\d{6}(: )";
        Pattern pattern = Pattern.compile(patternToMatch);
        Matcher matcher = pattern.matcher(message);
        //add priority
        if(matcher.find())
        {
            String firewallName = matcher.group(0).trim();
            outputMessage.put("firewallName", firewallName.substring(0, firewallName.length() - 1));
        }

        String[] initialTokens = message.split(patternToMatch);

        // to avoid splitting on commas which are within quotes
        StringBuilder stringBuilder = new StringBuilder(initialTokens[1]);
        boolean inQuotes = false;
        int messageLength = stringBuilder.length();
        for (int currentIndex = 0; currentIndex < messageLength; currentIndex++) {
            char currentChar = stringBuilder.charAt(currentIndex);
            if (currentChar == '\"') inQuotes = !inQuotes; // toggle state
            if (currentChar == ',' && inQuotes) {
                stringBuilder.setCharAt(currentIndex, '^'); // and replace later
            }
        }

        ArrayList<String> tokens = new ArrayList<>(Arrays.asList(stringBuilder.toString().split(",")));

        Boolean caseValue = tokens.get(0).contains("=");

        //populate common objects
        parseFirstField(initialTokens[0], outputMessage);

        if (caseValue)
            parseTupleWithKeyValue(tokens, outputMessage);
        else {
            String lastValue = message.substring(message.lastIndexOf(",")+1);
            if (lastValue == "")
                tokens.add(lastValue);
            parseTupleWithoutKeyValue(tokens, outputMessage, LOG_FIELDS);
        }
        removeEmptyFields(outputMessage);
    }

    private void parseTupleWithKeyValue(ArrayList<String> tokens, JSONObject outputMessage) {
        Iterator<String> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            String keyAndValue = iterator.next();
            String[] keyValueList = keyAndValue.split("=");
            outputMessage.put(keyValueList[0].toLowerCase().trim().replace(" ", "_"), keyValueList[1].trim().replace('^', ','));
        }
    }

    private void parseTupleWithoutKeyValue(ArrayList<String> tokens, JSONObject outputMessage, String[] fields) {
        int numFields = fields.length;
        int numTokens = tokens.size();
        int count;
        for(count = 0; count < numFields; count++)
            outputMessage.put(fields[count],tokens.get(count).trim().replace('^', ','));

        for(int tbdFieldCounter = 23; count < numTokens; count++,tbdFieldCounter++)
            outputMessage.put("tbd"+tbdFieldCounter,tokens.get(count).trim().replace('^', ','));
    }

    private void parseFirstField(String firstField, JSONObject outputMessage) {
        String patternToMatch = "\\[\\d{1,3}(\\.)\\d{1,3}(\\.)\\d{1,3}(\\.)\\d{1,3}\\]";
        //split first field on IP
        String[] tokens = firstField.split(patternToMatch);
        //get IP
        Pattern pattern = Pattern.compile(patternToMatch);
        Matcher matcher = pattern.matcher(firstField);
        //add priority
        if(matcher.find())
        {
            String ipAddress = matcher.group(0);
            outputMessage.put("ipAddress", ipAddress.substring(1, ipAddress.length()-1));
        }
        //add timestamp
        outputMessage.put("timestamp", this.formatTimestamp(tokens[0]));

        //add second timestamp
        String timestamp2 = tokens[1];
        outputMessage.put("timestamp2", timestamp2.substring(1, timestamp2.length()-2));
    }

    protected long formatTimestamp(Object value) {
        long epochTimestamp = System.currentTimeMillis();
        if (value != null) {
            try {
                epochTimestamp = toEpoch(Calendar.getInstance().get(Calendar.YEAR)  + " " + value);
            } catch (ParseException e) {
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

    @Override
    public void configure(Map<String, Object> config) {

    }
}
