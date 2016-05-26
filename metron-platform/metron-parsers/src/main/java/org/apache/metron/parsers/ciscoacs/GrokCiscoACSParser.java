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

    public GrokCiscoACSParser(String grokHdfsPath, String patternLabel) {
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
        if (message.containsKey("message")) {
            String messageValue = (String) message.get("message");
            System.out.println("messageValue: " + messageValue);
            //parse(messageValue.getBytes());
        }
    }

    /**
     * Parse the message. A single line is parsed at a time.
     * @param message The message being parsed in json form.
     * @return JSONObject containing the elements parsed from the message.
     */

    public List<JSONObject> parse(JSONObject message) {
        JSONObject toReturn = message;

        try {
            //String toParse = new String(raw_message, "UTF-8");
            //Match gm = grok.match(toParse);
           // gm.captures();

            //toReturn.putAll(gm.toMap());

            // Move the whole message into the tag "original_string"
            // for consistency between parsers
            //if (toReturn.containsKey(DEFAULT_PATTERN)) {
            //    toReturn.put("original_string", toParse);
            //    toReturn.remove(DEFAULT_PATTERN);
            //} else {
            //    LOGGER.error("Line was not able to be parsed as an Aruba message.");
            //    return toReturn;
            //}

            // Convert time to epoch time/timestamp
            if (toReturn.containsKey("timestamp")) {
                Date date = dateFormat.parse((String) toReturn.get("timestamp"));
                toReturn.put("timestamp", date.getTime());
            }

            // if url is in IP form, replace url tag with ip_src_addr
            if (toReturn.containsKey("url")) {
                String ip = (String) toReturn.get("url");
                if (ip.matches("[\\.\\d]+")) {
                    toReturn.put("ip_src_addr", ip);
                    toReturn.remove("url");
                }
            }

            // sort out the fields within message
            if (toReturn.containsKey("message")) {

                Pattern pattern = Pattern.compile("\\S[^=\\s]{1,}=");
                Matcher matcher = pattern.matcher(toReturn.get("message").toString());

                // Check first occurrences
                ArrayList<String> keys = new ArrayList<String>();
                if( matcher.find() ){
                    keys.add(matcher.group().toString().substring(0,matcher.group().toString().length()-1));
                }
                //Check all occurrences
                pattern = Pattern.compile(",\\S[^=\\s]{1,}=");
                matcher = pattern.matcher(toReturn.get("message").toString());
                while (matcher.find()) {
                    if(matcher.group().toString().equals(",timestamp=")){
                        keys.add("log_timestamp1");
                    }
                    else {
                        keys.add(matcher.group().toString().substring(1,matcher.group().toString().length()-1));
                    }
                }

                String[] fields = ((String) toReturn.get("message")).split(",\\S[^=\\s]{1,}=");

                HashMap<String, String> pairs = new HashMap<String, String>();
                for (int i = 0; (i < fields.length) && (i < keys.size()); i++) {
                    if( i == 0 ){
                        int index = fields[i].indexOf("=");
                        fields[i]= fields[i].substring(index+1);
                    }
                    if(keys.get(i).toString().length() >= 1 && fields[i].toString().length() >= 1)
                    {
                        pairs.put(keys.get(i), fields[i]);
                    }
                }
                Set set = pairs.entrySet();
                // Get an iterator
                Iterator i = set.iterator();
                // Display elements
                while(i.hasNext()) {
                    Map.Entry me = (Map.Entry)i.next();
                    if (me.getValue() != null || me.getValue().toString().length() != 0) {
                        toReturn.put((me.getKey().toString()), me.getValue().toString()); // add the field and value
                    } else {
                        toReturn.put((me.getKey().toString()), "EMPTY_FIELD");   // there was no value for this field
                    }
                }

                toReturn.remove("message"); // remove message. If something goes wrong, the message is preserved within the original_string
            }
        } catch (ParseException e) {
            LOGGER.error("ParseException when trying to parse date");
        }

        cleanJSON(toReturn, "ciscoasa");
        ArrayList<JSONObject> toReturnList = new ArrayList<JSONObject>();
        toReturnList.add(toReturn);
        return toReturnList;
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
        timestampCheck(sourceType, parsedJSON);
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