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

/**
 * Created by zra564 on 5/26/16.
 */
public class GrokInfobloxParser extends GrokParser {

    public GrokInfobloxParser(String grokHdfsPath, String patternLabel) {
        super(grokHdfsPath, patternLabel);
    }

    @Override
    protected void postParse(JSONObject message) {
        System.out.println("in post");
        removeEmptyFields(message);
        fixTimestamp(message);
        fixTags(message);

        // add dns result if necessary
        if (!message.containsKey("dns_result") && "named".equals(message.get("process"))) {
            message.put("dns_result", "success");
        }

        // fix dns action type if necessary
        if (message.containsKey("dns_action_type") && "resolving".equals(message.get("dns_action_type"))) {
            message.put("dns_action_type", "query");
        }

        // fix dns_action_type if necessary
        if (message.containsKey("dns_action_type") && "zone".equals(message.get("dns_action_type"))) {
            message.put("dns_action_type", "zone_update");
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
    }

    private void fixTags(JSONObject json) {
        json.clear();
        JSONObject fixedJSON = new JSONObject();
        for (Object o : json.keySet()) {
            String key = (String) o;
            Object value = json.get(key).toString();

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
                    System.out.println("key: " + key + ", tag: " + ", value: " + value);
                    isKnownTag = true;
                    break;
                case "clientunknown_":
                case "dnsunknown_":
                case "dhcpdunknwon_":
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
//        json.putAll(fixedJSON);
    }

    private void fixTimestamp(JSONObject json) {
        if (json.containsKey("timestamp") && null != json.get("timestamp")) {
            json.put("timestamp", formatTimestamp(json.get("timestamp").toString()));
        } else if (json.containsKey("unknown_timestamp") && null != json.get("unknown_timestamp")) {
            json.put("timestamp", formatTimestamp(json.get("unknown_timestamp").toString()));
            json.remove("unknwon_timestamp");
        } else if (json.containsKey("dhcpdunknwon_timestamp") && null != json.get("dhcpdunknwon_timestamp")) {
            json.put("timestamp", formatTimestamp(json.get("dhcpdunknwon_timestamp").toString()));
            json.remove("unknwon_timestamp");
        } else if (json.containsKey("dnsunknwon_timestamp") && null != json.get("dnsunknwon_timestamp")) {
            json.put("timestamp", formatTimestamp(json.get("dnsunknwon_timestamp").toString()));
            json.remove("dnsunknwon_timestamp");
        }
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
