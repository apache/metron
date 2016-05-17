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

package org.apache.metron.parsers.mcafeeepo;


import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class McAfeeEpoParser extends BasicParser {
    private static final Logger _LOG = LoggerFactory.getLogger(McAfeeEpoParser.class);
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public void init() {
    }

    @SuppressWarnings({ "unchecked", "unused" })
    public List<JSONObject> parse(byte[] msg) {
        df.setTimeZone(TimeZone.getTimeZone("UTC"));


        String message = "";
        List<JSONObject> messages = new ArrayList<>();
        JSONObject payload = new JSONObject();

        try {
            message = new String(msg, "UTF-8");

            String[] parts = message.split("<|>|\", |\" |\"$");
            payload.put("original_string", message);
            payload.put("priority", parts[1]);

            for(int i = 3; i < parts.length; i++){
                String[] keypair = parts[i].split("=\"");
                if(keypair[0].equals("src_ip"))
                    keypair[0] = "ip_src_addr";
                if(keypair[0].equals("dest_ip"))
                    keypair[0] = "ip_dst_addr";

                if(keypair[0].equals("timestamp")){
                    String timestamp = keypair[1];
                    int missingZeros = "yyyy-MM-dd HH:mm:ss.SSS".length() - timestamp.length();
                    timestamp += new String(new char[missingZeros]).replace("\0", "0"); // add on the missing zeros
                    payload.put(keypair[0], df.parse(timestamp).getTime());
                } else if(!keypair[1].equals("NULL") && !keypair[1].equals("_")){
                    payload.put(keypair[0], keypair[1]);
                }
            }

            messages.add(payload);
            return messages;
        } catch (Exception e) {
            e.printStackTrace();
            _LOG.error("Failed to parse: " + message);
            return null;
        }
    }
}
