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
            if(parts.length < 2){
                _LOG.error("Failed to parse: " + message);
                return null;
            }
            payload.put("original_string", message);
            payload.put("priority", parts[1]);

            String timestamp = "";
            for(int i = 3; i < parts.length; i++){
                String[] keypair = parts[i].split("=\"");
                if(keypair.length != 2){

                    _LOG.error("Failed to parse: " + message);
                    return null;
                }
                if(keypair[0].equals("src_ip"))
                    keypair[0] = "ip_src_addr";
                if(keypair[0].equals("dest_ip"))
                    keypair[0] = "ip_dst_addr";

                if(keypair[0].equals("timestamp")){
                    timestamp = keypair[1];

                } else if(!keypair[1].equals("NULL") && !keypair[1].equals("_")){
                    payload.put(keypair[0], keypair[1]);
                }
            }

            //No standard way to go between the timezone field value and a timezone, so they have to be done manually
            String timezone = (String)payload.get("timezone");
            if(timezone != null){
                switch(timezone){
                    case "Eastern Standard Time": df.setTimeZone(TimeZone.getTimeZone("EST"));break;
                    case "Central Standard Time": df.setTimeZone(TimeZone.getTimeZone("US/Central"));break;
                    case "Pacific Standard Time": df.setTimeZone(TimeZone.getTimeZone("PST"));break;
                    case "GMT Standard Time": df.setTimeZone(TimeZone.getTimeZone("GMT"));break;
                    case "India Standard Time": df.setTimeZone(TimeZone.getTimeZone("IST"));break;
                    case "Est": df.setTimeZone(TimeZone.getTimeZone("EST"));break;
                    case "CST": df.setTimeZone(TimeZone.getTimeZone("US/Central"));;break;
                    case "China Standard Time": df.setTimeZone(TimeZone.getTimeZone("Etc/GMT+8"));break;
                    case "Malay Peninsula Standard Time": df.setTimeZone(TimeZone.getTimeZone("Etc/GMT+8"));break;
                }
            }else{
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
            }

            int missingZeros = "yyyy-MM-dd HH:mm:ss.SSS".length() - timestamp.length();
            timestamp += new String(new char[missingZeros]).replace("\0", "0"); // add on the missing zeros
            payload.put("timestamp", df.parse(timestamp).getTime());

            messages.add(payload);
            return messages;
        } catch (Exception e) {
            e.printStackTrace();
            _LOG.error("Failed to parse: " + message);
            return null;
        }
    }
}
