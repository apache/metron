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
package org.apache.metron.parsers.contrib.links.io;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestRegexFileLink {

    private RegexFileLink link;

    @Before
    public void setUp() {
        this.link = new RegexFileLink();
    }

    @After
    public void tearDown() {
        this.link = null;
    }

    @Test
    public void testRegexFileLink() {
        List<String> patterns = new ArrayList<>();
        patterns.add("NUM:(?P<number>\\d+)");
        String logline = "NUM:1234";
        this.link.setPatterns(patterns);
        JSONObject output = (JSONObject) this.link.parseInputField(logline);
        assertTrue(output.containsKey("number"));
        assertEquals("1234", output.get("number"));

        patterns.add("AV\\s-\\sAlert\\s-\\s\"(?P<timestamp>\\d+)\"");
        logline = "AV - Alert - \"1234\"";
        this.link.setPatterns(patterns);
        output = (JSONObject) this.link.parseInputField(logline);
        System.out.println(output);
    }

    @Test
    public void testCompiledRegex() {
        String file = "file:///C:/Users/kevjac/Desktop/ossec_regex_compiled.txt";
        this.link.readFile(file);

        String data = "AV - Alert - \"1498521603\" --> RID: \"18149\"; RL: \"3\"; RG: \"windows,\"; RC: \"Windows User Logoff.\"; USER: \"C1238C89R$\"; SRCIP: \"None\"; HOSTNAME: \"(123-nl) 123.123.123.123->WinEvtLog\"; LOCATION: \"(123-nl) 123.123.123.123->WinEvtLog\"; EVENT: \"[INIT]2017 Jun 27 01:59:59 WinEvtLog: Security: AUDIT_SUCCESS(4634): Microsoft-Windows-Security-Auditing: 1238C89R$: EXID: 123.nl: An account was logged off. Subject:  Security ID:  S-1-5-21-123-123-123-12628  123 Name:  123$  Account Name: myusername  Account Domain:  EXID  Logon ID:  0x222d4cc7  Logon Type:   3  This event is generated when a logon session is destroyed. It may be positively correlated with a logon event using the Logon ID value. Logon IDs are only unique between reboots on the same computer.\"  4646,1[END]\";";
        String[] lines = data.split("[\\r\\n]+");
        for (String line: lines) {
            line = line.trim();
            JSONObject output = (JSONObject) this.link.parseInputField(line);
            System.out.println(line);
            System.out.println(output.keySet().size());
            System.out.println(output);
        }
        //this.link.parseInputField("AV - Alert - \"1498521603\" --> RID: \"18107\"; RL: \"3\"; RG: \"windows,authentication_success,\"; RC: \"Windows Logon Success.\"; USER: \"SVC00014\"; SRCIP: \"None\"; HOSTNAME: \"(xidaddsp10-exid-umcn-nl) 131.174.163.148->WinEvtLog\"; LOCATION: \"(xidaddsp10-exid-umcn-nl) 131.174.163.148->WinEvtLog\"; EVENT: \"[INIT]2017 Jun 27 01:59:58 WinEvtLog: Security: AUDIT_SUCCESS(4624): Microsoft-Windows-Security-Auditing: SVC00014: EXID: XIDADDSP10.exid.umcn.nl: An account was successfully logged on. Subject:  Security ID:  S-1-0-0  Account Name:  -  Account Domain:  -  Logon ID:  0x0  Logon Type:   3  New Logon:  Security ID:  S-1-5-21-2303058550-3105691012-4221217832-1694  Account Name:  SVC00014  Account Domain:  EXID  Logon ID:  0x222d4cfa  Logon GUID:  {75763528-BF37-2C36-640E-B8B179973B8B}  Process Information:  Process ID:  0x0  Process Name:  -  Network Information:  Workstation Name:   Source Network Address: 131.174.244.80  Source Port:  49047  Detailed Authentication Information:  Logon Process:  Kerberos  Authentication Package: Kerberos  Transited Services: -  Package Name (NTLM only): -  Key Length:  0  This event is generated when a logon session is created. It is generated on the computer that was accessed. [END]\";");
    }

    @Test
    public void testTesting() {
        final String regex = "^AV\\s+\\-\\sAlert\\s+\\-\\s\\\"(?<timestamp>\\d+)\\\"\\s\\-\\->\\sRID\\:\\s\\\"(?<ruleid>\\d+)\\\"\\;\\s+RL\\:\\s+\\\"(?<rulelevel>\\d+)\\\"\\;\\s+RG\\:\\s+\\\"(?<rulegroup>[^\\\"]*)\\\"\\;\\s+RC\\:\\s+\\\"(?<rulecomment>[^\\\"]*)\\\";.*?HOSTNAME\\:\\s*\"?\\((?<hostname>[^\\)]*)\\)\\s(?<winip>\\S+)->.*?AUDIT_SUCCESS\\((?<wineventid>(4634|4647))\\).*?Account\\s+Name\\:\\s+(?<username>.*?)\\s*Account\\s+Domain\\:\\s+(?<domain>.*?)\\s*Logon\\s+ID\\:\\s+(?<logonid>\\S+)(\\s+Logon\\s+Type\\:\\s+(?<logontype>\\d+))?.*";
        final String string = "AV - Alert - \"1498521603\" --> RID: \"18149\"; RL: \"3\"; RG: \"windows,\"; RC: \"Windows User Logoff.\"; USER: \"C1238C89R$\"; SRCIP: \"None\"; HOSTNAME: \"(123-nl) 123.123.123.123->WinEvtLog\"; LOCATION: \"(123-nl) 123.123.123.123->WinEvtLog\"; EVENT: \"[INIT]2017 Jun 27 01:59:59 WinEvtLog: Security: AUDIT_SUCCESS(4634): Microsoft-Windows-Security-Auditing: 1238C89R$: EXID: 123.nl: An account was logged off. Subject:  Security ID:  S-1-5-21-123-123-123-12628  123 Name:  123$  Account Name: myusername  Account Domain:  EXID  Logon ID:  0x222d4cc7  Logon Type:   3  This event is generated when a logon session is destroyed. It may be positively correlated with a logon event using the Logon ID value. Logon IDs are only unique between reboots on the same computer.\"  4646,1[END]\";\n";

        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(string);

        while (matcher.find()) {
            System.out.println("Full match: " + matcher.group(0));
            for (int i = 1; i <= matcher.groupCount(); i++) {
                System.out.println("Group " + i + ": " + matcher.group(i));
            }
        }
    }

    @Test
    public void testChangeFile() throws IOException {
        String file = "C:/Users/kevjac/Desktop/ossec_regex.txt";

        // Open the file
        FileInputStream fstream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;
        String output = "";

        //Read File Line By Line
        while ((strLine = br.readLine()) != null)   {
            // Print the content on the console
            String line = strLine;
            if (line.trim().length() == 0 || line.trim().startsWith("[")) {
                //output += line + "\n";
            } else {

                String pattern = line;
                if (pattern.substring(0, 1).equals("\"") && pattern.substring(pattern.length() - 1).equals("\"")) {
                    pattern = pattern.substring(1, pattern.length() - 1);
                }

                // Make sure the patterns do not start with ?P
                pattern = pattern.replaceAll("\\?P\\<", "\\?\\<");

                // Clean the groups such that these can be used by the Regex parser
                pattern = pattern.replaceAll("\\<([^>]*)[^a-zA-Z0-9>]{1,}([^>]*)\\>", "<$1$2>");
                pattern = pattern.replaceAll("\\<([^>]*)[^a-zA-Z0-9>]{1,}([^>]*)\\>", "<$1$2>");
                pattern = pattern.replaceAll("\\?\\(([a-zA-Z0-9_]+)\\)", "\\\\k<$1>");
                pattern = pattern.replaceAll("\\\\k\\<([^>]*)[^a-zA-Z0-9>]([^>]*)\\>", "\\\\k<$1$2>");

                // Custom rules
                pattern = pattern.replaceAll("[\\\\]{1}Type", "Type");
                pattern = pattern.replaceAll("[\\\\]{1}IPV4", "IPV4");
                pattern = pattern.replaceAll("[\\\\]{1}User", "User");

                output += pattern + "\n";
            }
        }

        //Close the input stream
        br.close();

        System.out.println(output);
    }

}
