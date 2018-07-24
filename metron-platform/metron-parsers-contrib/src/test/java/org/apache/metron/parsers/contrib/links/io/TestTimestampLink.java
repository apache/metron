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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestTimestampLink {

    private TimestampLink link;

    @Before
    public void setUp() {
        this.link = new TimestampLink();
    }

    @After
    public void tearDown() {
        this.link = null;
    }

    @Test
    @java.lang.SuppressWarnings("unchecked")
    public void testTimestamp() {
        String input = "1488801958";

        List<List> patterns = new ArrayList();
        List<String> pattern = new ArrayList();
        pattern.add("timestamp");
        pattern.add("newest");
        patterns.add(pattern);
        this.link.setPatterns(patterns);
        this.link.initPredefinedPatterns();
        Object outputObject = this.link.parseInputField(input);
        assertTrue(outputObject instanceof JSONObject);
        JSONObject output = (JSONObject) outputObject;

        assertTrue("Output should contain a \"datetime\" field.", output.containsKey("datetime"));
        assertTrue("Output should contain a \"mapping\" field.", output.containsKey("mapping"));
        assertTrue("Output should contain a \"timezone_available\" field.", output.containsKey("timezone_available"));

        assertEquals("Value for \"datetime\" field is not correct.", "2017-03-06 12:05:58.000 +0000", output.get("datetime"));
        assertEquals("Value for \"mapping\" field is not correct.", "yyyy-MM-dd HH:mm:ss.SSS Z", output.get("mapping"));
        assertEquals("Value for \"timezone_available\" field is not correct.", 1, output.get("timezone_available"));
    }

    @Test(expected = IllegalStateException.class)
    @java.lang.SuppressWarnings("unchecked")
    public void testIllegalInput() {
        String input = "9999-99-99 99:99:99";

        List<List> patterns = new ArrayList();
        List<String> pattern = new ArrayList();
        pattern.add("([0-9]{4})-([0-9]{2})-([0-9]{2}) ([0-9]{2}):([0-9]{2}):([0-9]{2})");
        pattern.add("yyyy MM dd HH mm ss");
        pattern.add("oldest");
        patterns.add(pattern);
        this.link.setPatterns(patterns);
        this.link.parseInputField(input);
    }

    @Test
    @java.lang.SuppressWarnings("unchecked")
    public void testDatetimeWithoutTimezone() {
        String input = "2017-01-01 01:01:01";

        List<List> patterns = new ArrayList();
        List<String> pattern = new ArrayList();
        pattern.add("([0-9]{4})-([0-9]{2})-([0-9]{2}) ([0-9]{2}):([0-9]{2}):([0-9]{2})");
        pattern.add("yyyy MM dd HH mm ss");
        pattern.add("oldest");
        patterns.add(pattern);
        this.link.setPatterns(patterns);
        Object outputObject = this.link.parseInputField(input);
        assertTrue(outputObject instanceof JSONObject);
        JSONObject output = (JSONObject) outputObject;

        assertTrue("Output should contain a \"datetime\" field.", output.containsKey("datetime"));
        assertTrue("Output should contain a \"mapping\" field.", output.containsKey("mapping"));
        assertTrue("Output should contain a \"timezone_available\" field.", output.containsKey("timezone_available"));

        assertEquals("Value for \"datetime\" field is not correct.", "2017-01-01 01:01:01.000", output.get("datetime"));
        assertEquals("Value for \"mapping\" field is not correct.", "yyyy-MM-dd HH:mm:ss.SSS", output.get("mapping"));
        assertEquals("Value for \"timezone_available\" field is not correct.", 0, output.get("timezone_available"));
    }

    @Test
    @java.lang.SuppressWarnings("unchecked")
    public void testSelectionCriteria() {
        String input = "2017-01-01 01:01:01 | 2017-01-01 02:01:01";

        List<List> patterns = new ArrayList();
        List<String> pattern = new ArrayList();
        pattern.add("([0-9]{4})-([0-9]{2})-([0-9]{2}) ([0-9]{2}):([0-9]{2}):([0-9]{2})");
        pattern.add("yyyy MM dd HH mm ss");
        pattern.add("oldest");
        patterns.add(pattern);
        this.link.setPatterns(patterns);
        Object outputObject = this.link.parseInputField(input);
        assertTrue(outputObject instanceof JSONObject);
        JSONObject output = (JSONObject) outputObject;

        assertTrue("Output should contain a \"datetime\" field.", output.containsKey("datetime"));
        assertTrue("Output should contain a \"mapping\" field.", output.containsKey("mapping"));
        assertTrue("Output should contain a \"timezone_available\" field.", output.containsKey("timezone_available"));

        assertEquals("Value for \"datetime\" field is not correct.", "2017-01-01 01:01:01.000", output.get("datetime"));
        assertEquals("Value for \"mapping\" field is not correct.", "yyyy-MM-dd HH:mm:ss.SSS", output.get("mapping"));
        assertEquals("Value for \"timezone_available\" field is not correct.", 0, output.get("timezone_available"));
    }

    @Test
    @java.lang.SuppressWarnings("unchecked")
    public void testPatternsGetterAndSetter() {
        List<List> patterns = new ArrayList();
        this.link.setPatterns(patterns);
        assertEquals(patterns, this.link.getPatterns());
    }

    @Test
    @java.lang.SuppressWarnings("unchecked")
    public void testPredefinedPatternsGetterAndSetter() {
        Map<String, List<Object>> predefinedPatterns = new HashMap();
        this.link.setPredefinedPatterns(predefinedPatterns);
        assertEquals(predefinedPatterns, this.link.getPredefinedPatterns());
    }

}
