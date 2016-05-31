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

package org.apache.metron.parsers.bro;

import org.apache.metron.common.Constants;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class BasicBroParser extends BasicParser {

    protected static final Logger _LOG = LoggerFactory
            .getLogger(BasicBroParser.class);
    private JSONCleaner cleaner = new JSONCleaner();

    @Override
    public void configure(Map<String, Object> parserConfig) {

    }

    @Override
    public void init() {

    }

    @SuppressWarnings("unchecked")
    public List<JSONObject> parse(byte[] msg) {

        _LOG.trace("[Metron] Starting to parse incoming message");

        String rawMessage = null;
        List<JSONObject> messages = new ArrayList<>();
        try {
            rawMessage = new String(msg, "UTF-8");
            _LOG.trace("[Metron] Received message: " + rawMessage);

            JSONObject cleanedMessage = cleaner.clean(rawMessage);
            _LOG.debug("[Metron] Cleaned message: " + cleanedMessage);

            if (cleanedMessage == null || cleanedMessage.isEmpty()) {
                throw new Exception("Unable to clean message: " + rawMessage);
            }

            String key;
            JSONObject payload;
            if (cleanedMessage.containsKey("type")) {
                key = cleanedMessage.get("type").toString();
                payload = cleanedMessage;
            } else {
                key = cleanedMessage.keySet().iterator().next().toString();

                if (key == null) {
                    throw new Exception("Unable to retrieve key for message: "
                            + rawMessage);
                }

                payload = (JSONObject) cleanedMessage.get(key);
            }

            if (payload == null) {
                throw new Exception("Unable to retrieve payload for message: "
                    + rawMessage);
            }

            String originalString = key.toUpperCase() + " |";
            for (Object k : payload.keySet()) {
                String value = payload.get(k).toString();
                originalString += " " + k.toString() + ":" + value;
            }
            payload.put("original_string", originalString);

            replaceKey(payload, Constants.Fields.TIMESTAMP.getName(), new String[]{ "ts" });

            long timestamp = 0L;
            if (payload.containsKey(Constants.Fields.TIMESTAMP.getName())) {
                try {
                    String broTimestamp = payload.get(Constants.Fields.TIMESTAMP.getName()).toString();
                    String convertedTimestamp = broTimestamp.replace(".","");
                    convertedTimestamp = convertedTimestamp.substring(0,13);
                    timestamp = Long.parseLong(convertedTimestamp);
                    payload.put(Constants.Fields.TIMESTAMP.getName(), timestamp);
                    payload.put("bro_timestamp",broTimestamp);
                    _LOG.trace(String.format("[Metron] new bro record - timestamp : %s", payload.get(Constants.Fields.TIMESTAMP.getName())));
                } catch (NumberFormatException nfe) {
                    _LOG.error(String.format("[Metron] timestamp is invalid: %s", payload.get("timestamp")));
                    payload.put(Constants.Fields.TIMESTAMP.getName(), 0);
                }
            }

            boolean ipSrcReplaced = replaceKey(payload, Constants.Fields.SRC_ADDR.getName(), new String[]{"source_ip", "id.orig_h"});
            if (!ipSrcReplaced) {
                replaceKeyArray(payload, Constants.Fields.SRC_ADDR.getName(), new String[]{ "tx_hosts" });
            }

            boolean ipDstReplaced = replaceKey(payload, Constants.Fields.DST_ADDR.getName(), new String[]{"dest_ip", "id.resp_h"});
            if (!ipDstReplaced) {
                replaceKeyArray(payload, Constants.Fields.DST_ADDR.getName(), new String[]{ "rx_hosts" });
            }

            replaceKey(payload, Constants.Fields.SRC_PORT.getName(), new String[]{"source_port", "id.orig_p"});
            replaceKey(payload, Constants.Fields.DST_PORT.getName(), new String[]{"dest_port", "id.resp_p"});

            payload.put(Constants.Fields.PROTOCOL.getName(), key);
            _LOG.debug("[Metron] Returning parsed message: " + payload);
            messages.add(payload);
            return messages;

        } catch (Exception e) {

            _LOG.error("Unable to Parse Message: " + rawMessage);
            e.printStackTrace();
            return null;
        }

    }

    private boolean replaceKey(JSONObject payload, String toKey, String[] fromKeys) {
        for (String fromKey : fromKeys) {
            if (payload.containsKey(fromKey)) {
                Object value = payload.remove(fromKey);
                payload.put(toKey, value);
                _LOG.trace(String.format("[Metron] Added %s to %s", toKey, payload));
                return true;
            }
        }
        return false;
    }

    private boolean replaceKeyArray(JSONObject payload, String toKey, String[] fromKeys) {
        for (String fromKey : fromKeys) {
            if (payload.containsKey(fromKey)) {
                JSONArray value = (JSONArray) payload.remove(fromKey);
                if (value != null && !value.isEmpty()) {
                    payload.put(toKey, value.get(0));
                    _LOG.trace(String.format("[Metron] Added %s to %s", toKey, payload));
                    return true;
                }
            }
        }
        return false;
    }

}
