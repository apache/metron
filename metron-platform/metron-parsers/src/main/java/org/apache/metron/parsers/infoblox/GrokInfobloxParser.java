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

package org.apache.metron.parsers.infoblox;

import org.apache.metron.parsers.GrokParser;
import org.json.simple.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;

public class GrokInfobloxParser extends GrokParser {

    @Override
    protected void postParse(JSONObject message) {
        removeEmptyFields(message);
        fixTimestamp(message);
        fixTags(message);

        // fix dns action type if necessary
        if (message.containsKey("dns_action_type") && "resolving".equals(message.get("dns_action_type"))) {
            message.put("dns_action_type", "query");
        }

        // fix dns_action_type if necessary
        if (message.containsKey("dns_action_type") && "zone".equals(message.get("dns_action_type"))) {
            message.put("dns_action_type", "zone_update");
        }

        // add dns result if necessary
        if (!message.containsKey("dns_result") && "named".equals(message.get("process")) && !"zone_update".equals(message.get("dns_action_type"))) {
            message.put("dns_result", "success");
        }

        // fix dst mac if necessary
        if (message.containsKey("dst_mac_addr") && !message.containsKey("dst_mac")) {
            message.put("dst_mac", message.get("dst_mac_addr"));
            message.remove("dst_mac_addr");
        }

        // fix src mac if necessary
        if (message.containsKey("src_mac_addr") && !message.containsKey("src_mac")) {
            message.put("src_mac", message.get("src_mac_addr"));
            message.remove("src_mac_addr");
        }

        // format dns record types
        if (message.containsKey("dns_record_type")) {
            if (message.get("dns_record_type") != null) {
                String dnt = message.get("dns_record_type").toString();
                dnt = dnt.replace(" ", ",").replace("/", ",");
                message.put("dns_record_type", dnt);
            }
        }
    }

    private void fixTags(JSONObject json) {
        JSONObject fixedJSON = new JSONObject();
        for (Object o : json.keySet()) {
            String key = (String) o;
            Object value = json.get(key);

            String tag = key.substring(0, (key).indexOf("_") + 1);
            boolean isKnownTag = false;
            boolean isUnknownTag = false;

            switch (tag) {
                case "clientquery_":
                case "clientupdatefail_":
                case "clientupdatesuccess_":
                case "error_":
                case "zone_":
                case "checkhints_":
                case "request_":
                case "ack_":
                case "inform_":
                    isKnownTag = true;
                    break;
                case "clientunknown_":
                case "dnsunknown_":
                case "dhcpdunknown_":
                case "unknown_":
                    isUnknownTag = true;
            }

            if (isKnownTag) {
                String shortKey = key.replaceFirst(tag, "");    // remove the tag from the key
                fixedJSON.put(shortKey, value);                 // add the shortKey/value pair to toReturn
            } else if (isUnknownTag) {
                String shortKey = key.replaceFirst(tag, "");
                if (!fixedJSON.containsKey(shortKey)) {
                    fixedJSON.put(shortKey, value);
                }
            } else {
                fixedJSON.put(key, value);
            }
        }
        json.clear();
        json.putAll(fixedJSON);
    }

    private void fixTimestamp(JSONObject json) {
        if (hasValidTimestampString(json, "unknown_timestamp")) {
            json.put("timestamp", (Long) formatTimestamp(json.get("unknown_timestamp")));
            json.remove("unknown_timestamp");
        } else if (hasValidTimestampString(json, "dhcpunknown_timestamp")) {
            json.put("timestamp", (Long) formatTimestamp(json.get("dhcpdunknown_timestamp")));
            json.remove("unknown_timestamp");
        } else if (hasValidTimestampString(json, "dnsunknown_timestamp")) {
            json.put("timestamp", (Long) formatTimestamp(json.get("dnsunknown_timestamp")));
            json.remove("dnsunknown_timestamp");
        } else if (hasValidTimestampString(json, "timestamp")) {
            json.put("timestamp", (Long) formatTimestamp(json.get("timestamp")));
        }
    }

    private boolean hasValidTimestampString(JSONObject json, String key) {
        return json.containsKey(key) && null != json.get(key) && !"".equals(json.get(key)) &&
                (json.get(key) instanceof String);
    }

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

}
