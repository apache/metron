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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;

public class GrokCiscoACSParser  extends GrokParser {

    private static final long serialVersionUID = 1297186928520950925L;
    private static final Logger LOGGER = LoggerFactory
            .getLogger(GrokCiscoACSParser.class);

    /**
     * Creates an ArubaParser object with the default filepath and pattern.
     * The default filepath being DEFAULT_FILE_PATH.
     * The default pattern being DEFAULT_PATTERN.
     * Same as calling ArubaParser("") or ArubaParser(null).
     * @throws GrokException
     */
    public ArubaParser() throws GrokException {
        this("");
    }

    /**
     * Creates an ArubaParser object with the given file path and
     * default pattern. The default pattern being DEFAULT_PATTERN.
     * If filepath is given as null or as an empty string, DEFAULT_FILE_PATH
     * will be used.
     * @throws GrokException
     */
    public ArubaParser(String filepath) throws GrokException {
        this(filepath, "");
    }

    /**
     * Creates an ArubaParser object with the given filepath and pattern.
     * If filepath is given as null or as an empty string, DEFAULT_FILE_PATH
     * will be used.
     * @throws IOException
     * @throws GrokException
     */
    public ArubaParser(String filepath, String pattern) throws GrokException {
        this.DATE_FORMAT = ParserConfig.getArubaDateFormat();
        this.DEFAULT_FILE_PATH = ParserConfig.getArubaDefaultFilePath();
        this.DEFAULT_PATTERN = ParserConfig.getArubaDefaultPattern();
        this.TIMEZONE_OFFSET = ParserConfig.getArubaTimezoneOffset();

        setupParser(filepath, pattern);
    }

    /**
     * Parse the message. A single line is parsed at a time.
     * @param raw_message The message being parsed in byte[] form.
     * @return JSONObject containing the elements parsed from the message.
     */
    @Override
    public JSONObject parse(byte[] raw_message) {
        JSONObject toReturn = new JSONObject();

        try {
            String toParse = new String(raw_message, "UTF-8");
            Match gm = grok.match(toParse);
            gm.captures();

            toReturn.putAll(gm.toMap());

            // Move the whole message into the tag "original_string"
            // for consistency between parsers
            if (toReturn.containsKey(DEFAULT_PATTERN)) {
                toReturn.put("original_string", toParse);
                toReturn.remove(DEFAULT_PATTERN);
            } else {
                LOGGER.error("Line was not able to be parsed as an Aruba message.");
                return toReturn;
            }

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
                        toReturn.put(ParserUtils.cleanseFieldName(me.getKey().toString()), me.getValue().toString()); // add the field and value
                    } else {
                        toReturn.put(ParserUtils.cleanseFieldName(me.getKey().toString()), "EMPTY_FIELD");   // there was no value for this field
                    }
                }

                toReturn.remove("message"); // remove message. If something goes wrong, the message is preserved within the original_string
            }
        } catch (ParseException e) {
            LOGGER.error("ParseException when trying to parse date");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("UnsupportedEncodingException when trying to create String");
        }
        cleanJSON(toReturn, "Aruba");
        return toReturn;
    }
}