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

package org.apache.metron.parsers.ciscoacs;

import org.apache.metron.parsers.GrokParser;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oi.thekraken.grok.api.Match;


import org.apache.metron.parsers.GrokParser;
import org.json.simple.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;


public class GrokCiscoACSParser  extends GrokParser {

    protected DateFormat dateFormat;

    protected String DATE_FORMAT;
    protected int TIMEZONE_OFFSET;

    private static final long serialVersionUID = 1297186928520950925L;
    private static final Logger LOGGER = LoggerFactory
            .getLogger(GrokCiscoACSParser.class);

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

    @SuppressWarnings("unchecked")
    private void removeEmptyFields(JSONObject json)
    {
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
    protected void postParse(JSONObject message)
    {
        removeEmptyFields(message);
        message.remove("timestamp_string");
        if (message.containsKey("messageGreedy")) {
            String messageValue = (String) message.get("messageGreedy");

            JSONObject toReturn = message;
            try {
                // if url is in IP form, replace url tag with ip_src_addr
                if (toReturn.containsKey("url")) {
                    String ip = (String) toReturn.get("url");
                    if (ip.matches("[\\.\\d]+")) {
                        toReturn.put("ip_src_addr", ip);
                        toReturn.remove("url");
                    }
                }

                // sort out the fields within message
                    Pattern pattern = Pattern.compile("=");

                    //Matcher matcher = pattern.matcher(toReturn.get("messageGreedy").toString());
                    Matcher matcher = pattern.matcher(messageValue);

                    // Check first occurrences
                    ArrayList<String> keys = new ArrayList<String>();
                    if( matcher.find() ){
                        keys.add(matcher.group().toString().substring(0,matcher.group().toString().length()-1));
                    }
                    //Check all occurrences
                    pattern = Pattern.compile(",");
                    //matcher = pattern.matcher(toReturn.get("messageGreedy").toString());
                    matcher = pattern.matcher(messageValue);

                    while (matcher.find()) {
                        if(matcher.group().toString().equals(",timestamp=")){
                            keys.add("log_timestamp1");
                        }
                        else {
                            keys.add(matcher.group().toString().substring(0,matcher.group().toString().length()-1));
                        }
                    }

                    String[] fields = messageValue.split(",");

                    HashMap<String, String> pairs = new HashMap<String, String>();
                    HashMap<String, String> newPairs = new HashMap<String, String>();
                    JSONObject steps = new JSONObject();
                    for (int i = 0; (i < fields.length) && (i < keys.size()); i++) {
                        String[] pairArray = fields[i].split("=");

                        if("Step".equals(pairArray[0].replaceAll("\\s+", "")))
                        {
                            steps.put((pairArray[0]+""+i).replaceAll("\\s+", ""),pairArray[1].replaceAll("\\s+", ""));
                        }
                        if("CmdSet".equals(pairArray[0].replaceAll("\\s+", "")))
                        {
                            String cmdSet = fields[i].substring(fields[i].indexOf("["));
                            newPairs.put(pairArray[0].replaceAll("\\s+", ""),cmdSet.replaceAll("\\s+", ""));
                        }
                        else
                        {
                            newPairs.put(pairArray[0].replaceAll("\\s+", ""),pairArray[1].replaceAll("\\s+", ""));
                        }
                    }
                    newPairs.put("Steps",steps.toJSONString());

                Set set = newPairs.entrySet();
                    // Get an iterator
                    Iterator i = set.iterator();
                    // Display elements
                    while(i.hasNext()) {
                        Map.Entry me = (Map.Entry)i.next();
                        if (me.getValue() != null || me.getValue().toString().length() != 0) {
                            if ("Steps".equals(me.getKey().toString())) {
                                toReturn.put("Steps", steps);
                            } else {
                                toReturn.put((me.getKey().toString()).replaceAll("\\s+", ""), (me.getValue().toString()).replaceAll("\\s+", "")); // add the field and value
                            }
                        }else {
                            toReturn.put((me.getKey().toString()), "EMPTY_FIELD");   // there was no value for this field
                        }
                    }

                    toReturn.remove("messageGreedy"); // remove message. If something goes wrong, the message is preserved within the original_string
            } catch (Exception e) {
                LOGGER.error("ParseException when trying to parse date");
            }

            cleanJSON(toReturn, "ciscoacs");
            ArrayList<JSONObject> toReturnList = new ArrayList<JSONObject>();
            toReturnList.add(toReturn);
        }
    }

    /**
     * Adds the current timestamp so we know when the file was ingested
     * @param parsedJSON the json that the parser created
     */
    private void addIngestTimestamp(JSONObject parsedJSON){
        parsedJSON.put("ingest_timestamp", System.currentTimeMillis());
    }

    /**
     * Adds the source type of the log
     * @param parsedJSON the json that the parser created
     * @param sourceType The source type of the log
     */
    private void addSourceType(JSONObject parsedJSON, String sourceType) {
        parsedJSON.put("source_type", sourceType);
    }

    /**
     * Cleans the json created by the parser
     * @param parsedJSON the json that the parser created
     * @param sourceType The source type of the log
     */
    protected void cleanJSON(JSONObject parsedJSON, String sourceType) {
        removeEmptyAndNullKeys(parsedJSON);
        removeUnwantedKey(parsedJSON);
        //addIngestTimestamp(parsedJSON);
        //timestampCheck(sourceType, parsedJSON);
        //addSourceType(parsedJSON, sourceType);
    }

    /**
     * Removes the 'UNWANTED' key from the json
     * @param parsedJSON the json the parser created
     */
    private void removeUnwantedKey(JSONObject parsedJSON) {
        parsedJSON.remove("UNWANTED");
    }

    /**
     * Removes empty and null keys from the json
     * @param parsedJSON the json the parser created
     */
    private void removeEmptyAndNullKeys(JSONObject parsedJSON) {
        Iterator<Object> keyIter = parsedJSON.keySet().iterator();
        while (keyIter.hasNext()) {
            Object key = keyIter.next();
            Object value = parsedJSON.get(key);
            // if the value is null or an empty string, remove that key.
            if (null == value || "".equals(value.toString())) {
                keyIter.remove();
            }
        }
    }

    /**
     * Checks if a timestamp key exists. If it does not, it creates one.
     * @param parsedJSON the json the parser created
     */
    private void timestampCheck(String sourceType, JSONObject parsedJSON) {
        if (!parsedJSON.containsKey("timestamp")) {
            parsedJSON.put("timestamp", System.currentTimeMillis());
            //parsedJSON.put("device_generated_timestamp", parsedJSON.get("timestamp"));
        }
        else {
            if (parsedJSON.get("timestamp") instanceof String){
                long longTimestamp = 0;
                try {
                    longTimestamp = Long.parseLong( (String) parsedJSON.get("timestamp"));
                } catch (NumberFormatException e) {
                    LOGGER.error("Unable to parse a long from the timestamp field.", e);
                }
                parsedJSON.put("timestamp", longTimestamp);
            }
            convertTimezoneToUTC(sourceType, parsedJSON);
        }
    }

    /**
     * Checks if a timestamp key exists. If it does not, it creates one.
     * Converts the timezone to UTC based on the value in the timezone map
     * @param parsedJSON the json the parser created
     */
    private void convertTimezoneToUTC(String sourceType, JSONObject parsedJSON) {
        parsedJSON.put("device_generated_timestamp", parsedJSON.get("timestamp"));
        long newTimestamp = (long) parsedJSON.get("timestamp");
        if (TIMEZONE_OFFSET != 24) {
            newTimestamp = newTimestamp + (TIMEZONE_OFFSET * 3600000);
            parsedJSON.put("timestamp", newTimestamp);
        }
        else {
            long timeDifference = (long) parsedJSON.get("ingest_timestamp") - (long) parsedJSON.get("device_generated_timestamp");
            long estimateOffset = timeDifference/3600000;
            newTimestamp = newTimestamp + (estimateOffset * 3600000);
            parsedJSON.put("timestamp", newTimestamp);
        }
    }
}