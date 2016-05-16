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

package org.apache.metron.parsers.activedirectory;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;

public class ActiveDirectoryParser extends BasicParser {

    private static final long serialVersionUID = 4860439408055777358L;

    public GrokWebSphereParser(String grokHdfsPath, String patternLabel) {
        super(grokHdfsPath, patternLabel);
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

    @Override
    protected void postParse(JSONObject message) {
        removeEmptyFields(message);
        message.remove("timestamp_string");
        if (message.containsKey("message")) {
            String messageValue = (String) message.get("message");
            if (messageValue.contains("logged into")) {
                parseLoginMessage(message);
            }
            else if (messageValue.contains("logged out")) {
                parseLogoutMessage(message);
            }
            else if (messageValue.contains("rbm(")) {
                parseRBMMessage(message);
            }
            else {
                parseOtherMessage(message);
            }
        }
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

    //Extracts the appropriate fields from login messages
    @SuppressWarnings("unchecked")
    private void parseLoginMessage(JSONObject json) {
        json.put("event_subtype", "login");
        String message = (String) json.get("message");
        if (message.contains(":")){
            String parts[] = message.split(":");
            String user = parts[0];
            String ip_src_addr = parts[1];
            if (user.contains("user(") && user.contains(")")) {
                user = user.substring(user.indexOf("user(") + "user(".length());
                user = user.substring(0, user.indexOf(")"));
                json.put("username", user);
            }
            if (ip_src_addr.contains("[") && ip_src_addr.contains("]")) {
                ip_src_addr = ip_src_addr.substring(ip_src_addr.indexOf("[") + 1);
                ip_src_addr = ip_src_addr.substring(0, ip_src_addr.indexOf("]"));
                json.put("ip_src_addr", ip_src_addr);
            }
            json.remove("message");
        }
    }

    //Extracts the appropriate fields from logout messages
    @SuppressWarnings("unchecked")
    private void parseLogoutMessage(JSONObject json) {
        json.put("event_subtype", "logout");
        String message = (String) json.get("message");
        if (message.matches(".*'.*'.*'.*'.*")) {
            String parts[] = message.split("'");
            String ip_src_addr = parts[0];
            if (ip_src_addr.contains("[") && ip_src_addr.contains("]")) {
                ip_src_addr = ip_src_addr.substring(ip_src_addr.indexOf("[") + 1);
                ip_src_addr = ip_src_addr.substring(0, ip_src_addr.indexOf("]"));
                json.put("ip_src_addr", ip_src_addr);
            }
            json.put("username", parts[1]);
            json.put("security_domain", parts[3]);
            json.remove("message");
        }
    }

    //Extracts the appropriate fields from RBM messages
    @SuppressWarnings("unchecked")
    private void parseRBMMessage(JSONObject json) {
        String message = (String) json.get("message");
        if (message.contains("(")) {
            json.put("process", message.substring(0, message.indexOf("(")));
            if (message.contains(":")) {
                json.put("message", message.substring(message.indexOf(":") + 2));
            }
        }
    }

    //Extracts the appropriate fields from other messages
    @SuppressWarnings("unchecked")
    private void parseOtherMessage(JSONObject json) {
        String message = (String) json.get("message");
        if (message.contains("(")) {
            json.put("process", message.substring(0, message.indexOf("(")));
            if (message.contains(":")) {
                json.put("message", message.substring(message.indexOf(":") + 2));
            }
        }
    }
}
