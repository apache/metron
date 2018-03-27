/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.parsers.contrib.links.io;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.metron.parsers.contrib.chainlink.ChainLinkIO;
import org.apache.hadoop.fs.Path;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A link for reading a file contatining Regex patterns and parsing the variables when a match is found.
 */
public class RegexFileLink extends ChainLinkIO<String> {

    private List<String> patterns;

    public void configure(Map<String, Object> config) {
        if (config.containsKey("file")) {
            assert config.get("file") instanceof String;
            this.readFile((String) config.get("file"));
        }
        if (config.containsKey("patterns")) {
            assert config.get("patterns") instanceof List;
            this.setPatterns((List) config.get("patterns"));
        }
    }


    @Override
    public Object parseInputField(String input) {
        JSONObject result = new JSONObject();

        for (String pattern : this.patterns) {
            JSONObject variables = extractVariables(input, pattern);
            System.out.println(pattern);
            if (variables.keySet().size() > 0) {
                System.out.println("Match found.");
                return variables;
            }
        }

        return result;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public JSONObject extractVariables(String logline, String pattern) {
        JSONObject vars = new JSONObject();

        // Skip some lines
        if (pattern.startsWith("[")) return new JSONObject();
        if (pattern.trim().length() == 0) return new JSONObject();

        if (pattern.substring(0, 1).equals("\"") && pattern.substring(pattern.length() - 1).equals("\"")) {
            pattern = pattern.substring(1, pattern.length() - 1);
        }

        // Make sure the patterns do not start with ?P
        pattern = pattern.replaceAll("\\?P\\<", "\\?\\<");

        // Find the non-preprocessed group names
        List<String> originalGroups = findGroups(pattern, "\\?\\<([^>]+)\\>");

        // Clean the groups such that these can be used by the Regex parser
        pattern = pattern.replaceAll("\\<([^>]*)[^a-zA-Z0-9>]{1,}([^>]*)\\>", "<$1$2>");
        pattern = pattern.replaceAll("\\<([^>]*)[^a-zA-Z0-9>]{1,}([^>]*)\\>", "<$1$2>");
        pattern = pattern.replaceAll("\\?\\(([a-zA-Z0-9_]+)\\)", "\\\\k<$1>");
        pattern = pattern.replaceAll("\\\\k\\<([^>]*)[^a-zA-Z0-9>]([^>]*)\\>", "\\\\k<$1$2>");

        // Custom rules
        pattern = pattern.replaceAll("[\\\\]{1}Type", "Type");
        pattern = pattern.replaceAll("[\\\\]{1}IPV4", "IPV4");
        pattern = pattern.replaceAll("[\\\\]{1}User", "User");

        System.out.println(pattern);

        // Find all group names
        List<String> groups = findGroups(pattern, "\\?\\<([a-zA-Z0-9]+)\\>");

        // Find all groups
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(logline);
        if (matcher.matches()) {
            for (String group : groups) {
                String value = matcher.group(group);

                // Find the original group name
                int index = groups.indexOf(group);
                String varName = originalGroups.get(index);

                vars.put(varName, value);
            }
        }

        // If the number of groups are not correct, return an empty JSON Object
        if (vars.keySet().size() != groups.size()) return new JSONObject();

        return vars;
    }

    private List<String> findGroups(String input, String pattern) {
        // Find all group names
        Pattern groupPattern = Pattern.compile(pattern);
        Matcher groupMatcher = groupPattern.matcher(input);
        List<String> groups = new ArrayList<>();
        while (groupMatcher.find()) {
            groups.add(groupMatcher.group(1));
        }

        return groups;
    }

    public void readFile(String filepath) {
        List<String> patterns = new ArrayList<>();

        Path path = new Path(filepath);
        try {
            FileSystem fs = FileSystem.get(URI.create(filepath), new Configuration());
            if (fs.exists(path)) {
                InputStream inputStream = fs.open(path);
                java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\n");
                while (scanner.hasNext()) {
                    String pattern = scanner.next();
                    if (pattern.length() > 0) {
                        patterns.add(pattern);
                    }
                }
            } else {
                throw new IllegalStateException("[Metron] Could not find the file: " + filepath);
            }
        } catch (IOException e) {
            throw new IllegalStateException("[Metron] Could not load the file: " + filepath);
        }

        this.setPatterns(patterns);
    }

}
