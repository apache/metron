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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActiveDirectoryParser extends BasicParser {

    private static final long serialVersionUID = -2198315298358794599L;
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ActiveDirectoryParser.class);
    protected int TIMEZONE_OFFSET;


    public ActiveDirectoryParser() throws IOException {
        super();
    }

    @Override
    public void init() {

    }

    @Override
    public List<JSONObject> parse(byte[] rawMessage) {

        ArrayList<JSONObject> toReturn = new ArrayList<JSONObject>();

        try {
            toReturn.add(getActiveDirectoryJSON(new String(rawMessage)));
            return toReturn;
        } catch (IOException e) {
            LOGGER.error("UnsupportedEncodingException when trying to create String", e);
        }

        return toReturn;
    }

    private JSONObject getActiveDirectoryJSON(String fileName) throws IOException {
        // if using test generator, read from file
        if (fileName.matches("^/vagrant/resources/activedirectory/\\d+\\.txt")) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                String currentLine;
                StringBuffer sb = new StringBuffer();
                while ((currentLine = br.readLine()) != null) {
                    sb.append(currentLine + "\n");
                }
                fileName = sb.toString();
                br.close();
            } catch (IOException e) {
                LOGGER.error("Unable to locate test file.", e);
            }
        }

        SimpleDateFormat dateFormatLong = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
        BufferedReader br = null;
        String currentLine;
        String[] mapValues;
        String[] memberMapValues;
        String[] memberOfMapValues;
        String[] memberInfo;
        String[] memberOfInfo;
        ArrayList membersList;
        ArrayList membersOfList;
        JSONObject jsonMain;
        JSONObject jsonNames;
        JSONObject jsonObject;
        JSONObject jsonEvent;
        JSONObject jsonAdditional;
        JSONObject jsonTemp;

        /* Members and MemberOF  need to be split up by their members*/
        jsonMain = new JSONObject();
        jsonNames = new JSONObject();
        jsonObject = new JSONObject();
        jsonEvent = new JSONObject();
        jsonAdditional = new JSONObject();
        mapValues = null;
        jsonTemp = jsonMain;
        br = new BufferedReader(new StringReader(fileName));

        jsonMain.put("original_string", fileName);

        while (null != ((currentLine = br.readLine()))) {
            /*catching the line if it is the first line in the file
             which contains the priority number, timestamp, and the simple computer name*/
            if (currentLine.length() > 1 && (currentLine.startsWith("<") && currentLine.contains(">"))) {
                mapValues = currentLine.split(" ", 3);
                jsonMain.put("priority", mapValues[0].substring(1, mapValues[0].length() -1));
                jsonMain.put("simpleMachineName", mapValues[1]);
                try {
                    Date test = dateFormatLong.parse(mapValues[2]);
                    jsonMain.put("timestamp", dateFormatLong.parse(mapValues[2]).getTime());
                } catch (ParseException e) {
                    LOGGER.error("Unable to parse date", e);
                }
            } /* if this line does not hold any data, but identifies which
             * jsonObject it should fall under then it will get caught in
             * this if statement. If it doesnt co
             */ else if (currentLine.length() > 1 && !currentLine.contains("=")) {
                if (currentLine.startsWith("Names:")) {
                    jsonTemp = jsonNames;
                } else if (currentLine.startsWith("Object Details:")) {
                    jsonTemp = jsonObject;
                } else if (currentLine.startsWith("Event Details:")) {
                    jsonTemp = jsonEvent;
                } else if (currentLine.startsWith("Additional Details:")) {
                    jsonTemp = jsonAdditional;
                } else {
                    jsonTemp = jsonMain;
                }
            } /* catching the remaining key and value pairs in the log
             which contain '=' sign*/ else if (currentLine.length() > 1) {
                mapValues = currentLine.split("=", 2);
                /*SPECIAL CASE: if it starts with memberOF, we have to
                 split up the values using the pipe delimiter and only
                 taking the "CN" value */
                if (mapValues[0].trim().equalsIgnoreCase("memberOf")) {
                    membersOfList = new ArrayList<String>();
                    memberMapValues = mapValues[1].split("\\|");
                    for (String value : memberMapValues) {
                        memberOfInfo = value.split(",");//Splitting the fields
                        memberOfInfo = memberOfInfo[0].split("=");//splitting the key value pairs
                        membersOfList.add(memberOfInfo[1]);//taking the "value" part of the key value field(
                    }
                    jsonAdditional.put("memberOf", membersOfList);
                } /*SPECIAL CASE: if it starts with member, we have to
                 split up the values using the pipe delimiter and only
                 taking the "CN" value */ else if (mapValues[0].trim().equalsIgnoreCase("member")) {
                    membersList = new ArrayList<String>();
                    memberMapValues = mapValues[1].split("\\|");//Splitting the individual users up
                    for (String value : memberMapValues) {
                        //sometimes the last value is cut off, so we need to check to see if it is
                        //The first value is hte eid which is what we want, so if there is at least one ",", the eid is there
                        if(value.contains(",")){
                            memberInfo = value.split(",");//Splitting the indiviual users fields up
                            memberInfo = memberInfo[0].split("=");//splitting the virst key pair value up
                            membersList.add(memberInfo[1]);//adding the "value" part of the key value field
                        }
                    }
                    jsonAdditional.put("member", membersList);
                } //NORMAL CASE: if it is a regular key value pair
                else {
                    jsonTemp.put(mapValues[0].trim(), mapValues[1]);
                }
            }
            //do nothing if the line's length is 0.(Implied else)
        }
        br.close();
        jsonMain.put("names", jsonNames);
        jsonMain.put("object", jsonObject);
        jsonMain.put("event", jsonEvent);
        jsonMain.put("additional", jsonAdditional);

        cleanJSON(jsonMain, "ActiveDirectory");
        System.out.println("jsonMain: " + jsonMain);
        return jsonMain;
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
     * Gets the size of a JSON after a failed parse
     * @return The size of a JSON after a failed parse
     */
    public int getFailedParseSize() {
        JSONObject json = new JSONObject();
        cleanJSON(json, "getting_size");
        return json.size();
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
            parsedJSON.put("device_generated_timestamp", parsedJSON.get("timestamp"));
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
        if (TIMEZONE_OFFSET != 24)
        {
            newTimestamp = newTimestamp + (TIMEZONE_OFFSET * 3600000);
            parsedJSON.put("timestamp", newTimestamp);
        }
        else
        {
            long timeDifference = (long) parsedJSON.get("ingest_timestamp") - (long) parsedJSON.get("device_generated_timestamp");
            long estimateOffset = timeDifference/3600000;
            newTimestamp = newTimestamp + (estimateOffset * 3600000);
            parsedJSON.put("timestamp", newTimestamp);
        }
    }
}
