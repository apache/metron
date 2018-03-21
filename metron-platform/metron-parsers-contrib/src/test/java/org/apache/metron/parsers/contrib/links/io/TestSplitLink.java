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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSplitLink {

    private SplitLink link;

    @Before
    public void setUp() {
        this.link = new SplitLink();
    }

    @After
    public void tearDown() {
        this.link = null;
    }

    @Test
    public void testSplitLink() {
        String input = "some_header|some_message|some_footer";
        Map<String, Object> selector = new HashMap<>();
        selector.put("-1", "test_1");
        selector.put("1", "test_2");

        this.link.setDelimiter("|");
        this.link.setSelector(selector);

        Object outputObject = this.link.parseInputField(input);
        assertTrue(outputObject instanceof JSONObject);
        JSONObject output = (JSONObject) outputObject;
        assertEquals(2, output.size());
        assertTrue(output.containsKey("test_1"));
        assertTrue(output.containsKey("test_2"));
        assertEquals("some_footer", output.get("test_1"));
        assertEquals("some_message", output.get("test_2"));
    }

    @Test
    public void testSplitLinkRegex() {
        String input = "some_header||||some_message||some_footer";
        Map<String, Object> selector = new HashMap<>();
        selector.put("-1", "test_1");
        selector.put("1", "test_2");

        this.link.setDelimiter("\\|+", true);
        this.link.setSelector(selector);

        Object outputObject = this.link.parseInputField(input);
        assertTrue(outputObject instanceof JSONObject);
        JSONObject output = (JSONObject) outputObject;
        assertEquals(2, output.size());
        assertTrue(output.containsKey("test_1"));
        assertTrue(output.containsKey("test_2"));
        assertEquals("some_footer", output.get("test_1"));
        assertEquals("some_message", output.get("test_2"));
    }

    @Test
    public void testOutOfBounds() {
        String input = "some_header|some_message|some_footer";
        Map<String, Object> selector = new HashMap<>();
        selector.put("5", "test_1");

        this.link.setDelimiter("|");
        this.link.setSelector(selector);

        Object outputObject = this.link.parseInputField(input);
        assertTrue(outputObject instanceof JSONObject);
        JSONObject output = (JSONObject) outputObject;
        assertEquals(0, output.size());
    }

    @Test
    public void testIntegerPositions() {
        String input = "some_header|some_message|some_footer";
        Map<Integer, Object> selector = new HashMap<>();
        selector.put(1, "test_1");

        this.link.setDelimiter("|");
        this.link.setSelector(selector);

        Object outputObject = this.link.parseInputField(input);
        assertTrue(outputObject instanceof JSONObject);
        JSONObject output = (JSONObject) outputObject;
        assertEquals(1, output.size());
        assertTrue(output.containsKey("test_1"));
        assertEquals("some_message", output.get("test_1"));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalIndex() {
        String input = "some_header|some_message|some_footer";
        Map<String, Object> selector = new HashMap<>();
        selector.put("illegal_index", "test_1");

        this.link.setDelimiter("|");
        this.link.setSelector(selector);

        this.link.parseInputField(input);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalDelimiter() {
        String input = "some_header|some_message|some_footer";
        Map<String, Object> selector = new HashMap<>();
        selector.put("0", "test_1");
        this.link.setSelector(selector);
        this.link.parseInputField(input);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalSelector() {
        String input = "some_header|some_message|some_footer";
        this.link.setDelimiter("|");
        this.link.parseInputField(input);
    }

    @Test
    public void testGetSetDelimiter() {
        this.link.setDelimiter("|");
        assertEquals("|", this.link.getDelimiter());
        assertEquals(false, this.link.isDelimiterRegex());
        this.link.setDelimiter(".*", true);
        assertEquals(".*", this.link.getDelimiter());
        assertEquals(true, this.link.isDelimiterRegex());
    }

    @Test
    public void testGetSetSelector() {
        Map<String, Object> selector = new HashMap<>();
        selector.put("0", "test_1");
        this.link.setSelector(selector);
        assertEquals(selector, this.link.getSelector());
    }

}
