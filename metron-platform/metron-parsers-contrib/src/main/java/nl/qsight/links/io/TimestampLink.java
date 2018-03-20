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
package nl.qsight.links.io;

import nl.qsight.chainlink.ChainLinkIO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static nl.qsight.common.Constants.TIMESTAMP;

public class TimestampLink extends ChainLinkIO<String> {

    private List<List> patterns;
    private Map<String, List<Object>> predefined_patterns;

    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> config) {
        this.initPredefinedPatterns();

        if (config.containsKey("patterns")) {
            assert config.get("patterns") instanceof List;
            this.setPatterns((List<List>) config.get("patterns"));
        }
    }

    public void initPredefinedPatterns() {
        Map<String, List<Object>> patterns = new HashMap<>();
        patterns.put("timestamp", Arrays.asList("([0-9]{10,13})", "t"));
        this.setPredefinedPatterns(patterns);
    }

    @SuppressWarnings("unchecked")
    private void addTimestampToMessage(JSONObject message, Long timestamp) {
        message.put(TIMESTAMP, timestamp);
    }

    private String extractLocalTime(String regex_result, String timestamp_mapping, String mapping_format) {
        DateTimeFormatter dtf = DateTimeFormat.forPattern(timestamp_mapping);
        DateTimeFormatter df = DateTimeFormat.forPattern(timestamp_mapping);
        DateTime temp = df.withOffsetParsed().parseDateTime(regex_result);
        DateTimeZone theZone = temp.getZone();
        DateTime dt = dtf.withZone(theZone).parseDateTime(regex_result);
        DateTimeFormatter fmt = DateTimeFormat.forPattern(mapping_format);
        return fmt.print(dt);
    }

    private long regexToTimestamp(String regex_result, String timestamp_mapping) {
        try {
            long result;
            if (timestamp_mapping.equals("t")) {
                result = Long.parseLong(regex_result);
            } else {
                DateTimeFormatter dtf = DateTimeFormat.forPattern(timestamp_mapping);
                DateTime dt = dtf.withZoneUTC().parseDateTime(regex_result);
                result = dt.getMillis();
            }

            // Convert to ms
            if (String.valueOf(result).length() < 13) result *= 1000;

            return result;
        } catch (Exception e) {
            throw new IllegalStateException("A pattern (" + regex_result + ") was found, but could not be mapped using the following mapping: " + timestamp_mapping);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object parseInputField(String input) {
        JSONObject message = new JSONObject();

        // Initiate objects
        Date datetime = null;
        String mapping_format = null;
        DateFormat format = null;
        String formatted_datetime = null;
        int timezone_available = 0;

        // In the remainder of the code, a loop will run through the configured patterns in order to regex for the
        // proper elements of the logline, and create output based on the regex result(s).
        for (List pattern_mapping_object : this.getPatterns()) {
            String regex_pattern;
            String timestamp_mapping;
            Object selection_index;

            // A pattern is a tuple (regex, mapping, indexing) or (predefined_pattern, indexing) where the predefined
            // pattern is defined in the constructor of this class and also consists of a (regex, mapping) tuple.
            if (pattern_mapping_object == null) {
                throw new IllegalStateException("Pattern type not recognized.");
            }

            // The pattern should consist of either 2 or 3 elements
            if (pattern_mapping_object.size() == 2) {
                // If 2 elements, then the pattern is a (predefined_pattern, indexing) tuple
                assert pattern_mapping_object.get(0) instanceof String;
                assert pattern_mapping_object.get(1) instanceof String || pattern_mapping_object.get(1) instanceof Integer;
                List predefined_pattern = this.getPredefinedPatterns().get(pattern_mapping_object.get(0));
                regex_pattern = (String) predefined_pattern.get(0);
                timestamp_mapping = (String) predefined_pattern.get(1);
                selection_index = pattern_mapping_object.get(1);
            } else if (pattern_mapping_object.size() == 3) {
                // If 2 elements, then the pattern is a (regex, mapping, indexing) tuple
                assert pattern_mapping_object.get(0) instanceof String;
                assert pattern_mapping_object.get(1) instanceof String;
                assert pattern_mapping_object.get(2) instanceof String || pattern_mapping_object.get(2) instanceof Integer;
                regex_pattern = (String) pattern_mapping_object.get(0);
                timestamp_mapping = (String) pattern_mapping_object.get(1);
                selection_index = pattern_mapping_object.get(2);
            } else {
                throw new IllegalStateException("Pattern does not contain 2 or 3 elements.");
            }

            // Compile the pattern and try to find matches
            Pattern compiled_pattern = Pattern.compile(regex_pattern);
            Matcher matcher = compiled_pattern.matcher(input);

            // Concatenate all matches (separated with spaces), so afterwards configured indexing criteria can be used
            // (e.g. first, last, oldest, newest).
            List<String> results = new ArrayList<>();
            while (matcher.find()) {
                StringBuilder regex_match = new StringBuilder();
                for (int group = 1; group <= matcher.groupCount(); group++) {
                    String item = matcher.group(group);
                    regex_match.append(" ").append(item);
                }
                if (regex_match.length() > 0) {
                    regex_match = new StringBuilder(regex_match.substring(1));
                }
                results.add(regex_match.toString());
            }

            // Try to convert the found patterns to timestamps
            List<Long> timestamps = new ArrayList<>();
            for (String result : results) {
                timestamps.add(regexToTimestamp(result, timestamp_mapping));
            }

            // Order the found timestamps according to the selection criteria
            int selected_timestamp = -1;
            if (timestamps.size() == 0) {
                // No timestamps found, continue to the next pattern
                continue;
            } else if (timestamps.size() == 1) {
                // There is only one timestamp, so select the first element
                selected_timestamp = 0;
                addTimestampToMessage(message, timestamps.get(0));
            } else {
                // Advanced selection criteria
                if (selection_index instanceof Integer) {
                    // Select the (select_index)th element
                    int selection_index_integer = (int) selection_index;
                    if (selection_index_integer < results.size()) {
                        addTimestampToMessage(message, timestamps.get(selection_index_integer));
                        selected_timestamp = selection_index_integer;
                    }
                } else if (selection_index instanceof String) {
                    // If it is a string, select by newest or by oldest
                    if (selection_index.equals("newest")) {
                        // Find the newest timestamp
                        long max_timestamp = Collections.max(timestamps);
                        selected_timestamp = timestamps.indexOf(max_timestamp);
                        addTimestampToMessage(message, max_timestamp);
                    } else if (selection_index.equals("oldest")) {
                        // Find the oldest timestamp
                        long min_timestamp = Collections.min(timestamps);
                        selected_timestamp = timestamps.indexOf(min_timestamp);
                        addTimestampToMessage(message, min_timestamp);
                    } else {
                        // Unknown selection criteria (if it is a string)
                        throw new IllegalStateException("The selection index should be either \"newest\" or \"oldest\".");
                    }
                }
            }

            if (selected_timestamp != -1) {
                // Scenario 2: There is a timestamp which has been selected
                String orig_regex = results.get(selected_timestamp);
                timezone_available = 0;
                if (timestamp_mapping.contains("X") || timestamp_mapping.contains("Z")) {
                    timezone_available = 1;
                }
                message.put("timezone_available", timezone_available);

                // if datetime found is an epoch timestamp, length is checked
                if (timestamp_mapping.equals("t")) {
                    long timestamp_epoch = Long.parseLong(orig_regex);
                    if (String.valueOf(timestamp_epoch).length() < 13) {
                        timestamp_epoch *= 1000;
                    }
                    // timezone_available set to 1, in order to avoid that the fieldtransformation adds the
                    // probe-timezone and converts accordingly to timestamp, since any epoch timestamp is assumed to
                    // be UTC.
                    timezone_available = 1;
                    message.put("timezone_available", timezone_available);

                    // formatted_datetime
                    datetime = new Date(timestamp_epoch);
                    mapping_format = "yyyy-MM-dd HH:mm:ss.SSS Z";
                    format = new SimpleDateFormat(mapping_format);
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    formatted_datetime = format.format(datetime);

                    // since the datetime found is no epoch timestamp, there are two possible mapping_formats that should be
                    // used to normalize the datetime output.
                } else {
                    if (timezone_available == 0) {
                        mapping_format = "yyyy-MM-dd HH:mm:ss.SSS";
                    } else {
                        mapping_format = "yyyy-MM-dd HH:mm:ss.SSS Z";
                    }
                    // formatted_datetime
                    formatted_datetime = extractLocalTime(orig_regex, timestamp_mapping, mapping_format);
                }
            }
        }

        if(formatted_datetime == null) {
            // The script starts with (fallback) scenario 1 in which case a timestamp is not found/missing. In such a
            // situation, datetime (=the variable used for fieldtransformation(s) in the config file of the parser) is set
            // to current time.
            datetime = new Date();

            // First, datetime is formatted to a datetime with timezone "+0000" (in order to make sure that a timezone is
            // always available during later fieldtransformation configuration in the config file of the parser).
            mapping_format = "yyyy-MM-dd HH:mm:ss.SSS Z";
            format = new SimpleDateFormat(mapping_format);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            formatted_datetime = format.format(datetime);

            // lastly for scenario 1, timezone_available is set to 1, so fieldtransformation configuration will not append
            // the datetime field before converting it to epoch timestamp (see the config file of a parser).
            timezone_available = 1;
        }

        // The code above produced a (formatted) datetime, a mapping_format and a timezone_available variable, all for
        // proper use of fieldtransformation(s) to produce the proper normalised epoch time.
        message.put("datetime", formatted_datetime);
        message.put("mapping", mapping_format);
        message.put("timezone_available", timezone_available);

        // Lastly, original_timestamp will be set as a non-timezone version of the formatted(datetime).
        String original_datetime_output_mapping = "yyyy-MM-dd HH:mm:ss.SSS";
        String original_timestamp;
        if (original_datetime_output_mapping.equals(mapping_format)) {
            original_timestamp = formatted_datetime;
        } else {
            original_timestamp = extractLocalTime(formatted_datetime, mapping_format, original_datetime_output_mapping);
        }
        message.put("original_timestamp", original_timestamp);

        return message;
    }

    public void setPatterns(List<List> patterns) {
        this.patterns = patterns;
    }

    public List<List> getPatterns() {
        return this.patterns;
    }

    public void setPredefinedPatterns(Map<String, List<Object>> predefined_patterns) {
        this.predefined_patterns = predefined_patterns;
    }

    public Map<String, List<Object>> getPredefinedPatterns() {
        return this.predefined_patterns;
    }

}
