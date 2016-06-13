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

        String processDataWithPriority;
        if (indexOfColon == -1) {
            processDataWithPriority = message;
        }
        else {
            processDataWithPriority = message.substring(0, indexOfColon).trim();
        }

        String[] tokens = processDataWithPriority.split(">");

        outputMessage.put("priority", tokens[0].substring(1));

        if (indexOfColon == -1) {
            outputMessage.put("message", tokens[1].trim());
        }
        else {
            String processData = tokens[1];
            int indexOfSquareBracketOpen = processData.indexOf("[");
            int indexOfSquareBracketClose = processData.indexOf("]");

            if (indexOfSquareBracketOpen > -1) {
                outputMessage.put("processName", processData.substring(0, indexOfSquareBracketOpen));
                outputMessage.put("processId", processData.substring(indexOfSquareBracketOpen + 1, indexOfSquareBracketClose));
            } else {
                outputMessage.put("processName", processData);
            }

            outputMessage.put("message", message.substring(indexOfColon + 1).trim());
        }

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

