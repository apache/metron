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

package org.apache.metron.parsers.checkpoint_traffic;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class BasicCheckpointTrafficParser extends BasicParser {

    protected static final Logger LOGGER = LoggerFactory
            .getLogger(BasicCheckpointTrafficParser.class);

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void configure(Map<String, Object> parserConfig) {

    }

    @Override
    public void init() {


    }

    @SuppressWarnings("unchecked")
    public List<JSONObject> parse(byte[] msg) {

        LOGGER.trace("[Metron] Starting to parse incoming message");

        String rawMessage = new String(msg);
        List<JSONObject> messages = new ArrayList<>();
        try {
            LOGGER.trace("[Metron] Received message: " + rawMessage);

            JSONObject payload = new JSONObject();
            payload.put("original_string", rawMessage);

            String priority = rawMessage.substring(1, rawMessage.indexOf(">"));
            payload.put("priority", priority);
            int timestampEnd = rawMessage.indexOf(">") + "Jun  2 15:49:24".length()+1;
            String syslog_timestamp = rawMessage.substring(rawMessage.indexOf(">")+ 1, timestampEnd);
            payload.put("syslog_timestamp", syslog_timestamp);

            int hostnameEnd = rawMessage.indexOf(" ", timestampEnd+1);
            String hostname = rawMessage.substring(timestampEnd + 1, hostnameEnd);
            payload.put("hostname", hostname);

            int processEnd = rawMessage.indexOf("[", hostnameEnd + 1);
            String process = rawMessage.substring(hostnameEnd+1, processEnd);
            payload.put("process", process);


            int pidEnd = rawMessage.indexOf("]", processEnd + 1);
            String pid = rawMessage.substring(processEnd+1, pidEnd);
            payload.put("pid", pid);

            //log header ends with a colon and space, so we can just cut it off
            rawMessage = rawMessage.substring(rawMessage.indexOf(": ") + 2);

            String[] keypairs = rawMessage.split("\\|");
            for (String keypair : keypairs) {
                String[] split = keypair.split("=",2);

                switch(split[0]){
                    case "src" : split[0] = "ip_src_addr"; break;
                    case "s_port": split[0] = "ip_src_port"; break;
                    case "dst": split[0] = "ip_dst_addr"; break;
                    case "service": split[0] = "ip_dst_port"; break;
                    case "proto": split[0] = "protocol"; break;
                    case "client_ip": split[0] = "ip_src_addr"; break;
                }

                if(split[0].equals("time")){
                    dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                    payload.put("timestamp", dateFormat.parse(split[1]).getTime());
                }else{
                    split[0] = split[0].replaceAll("[^\\._a-zA-Z0-9]+","");
                    payload.put(split[0], split[1]);
                }
            }
            LOGGER.debug("[Metron] Returning parsed message: " + payload);
            messages.add(payload);
            return messages;
        } catch (Exception e) {
            LOGGER.error("Unable to Parse Message: " + rawMessage, e);
            throw new IllegalStateException("Unable to Parse Message: " + rawMessage + " due to " + e.getMessage(), e);
        }

    }


}
