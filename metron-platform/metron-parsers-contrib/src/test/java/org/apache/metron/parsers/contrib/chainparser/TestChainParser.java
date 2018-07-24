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
package org.apache.metron.parsers.contrib.chainparser;

import org.apache.metron.parsers.contrib.chainlink.ChainLink;
import org.apache.metron.parsers.contrib.chainlink.ChainLink;
import org.apache.metron.parsers.contrib.common.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.parsers.contrib.common.Constants.ORIGINAL_STRING;
import static org.apache.metron.parsers.contrib.common.Constants.TIMESTAMP;
import static org.junit.Assert.*;

public class TestChainParser {

    private ChainParser parser;

    private class FieldRemoveLink extends ChainLink {

        String fieldToRemove = "";

        @Override
        public JSONObject parse(JSONObject input) {
            input.remove(this.fieldToRemove);
            return input;
        }
    }

    @Before
    public void setUp() {
        this.parser = new ChainParser();
    }

    @After
    public void tearDown() {
        this.parser = null;
    }

    @Test
    public void testRequiredFields() {
        // Even without a specified link, the required fields should be set
        byte[] exampleMessage = "example_message".getBytes();
        List<JSONObject> resultSet = this.parser.parse(exampleMessage);

        // Should contain 1 message
        assertEquals(1, resultSet.size());

        // Therefore, it is possible to access the first element of the result
        JSONObject state = resultSet.get(0);

        // Check whether the state contains the required "original_string" message
        assertTrue("The parsed JSONObject should contain \"original_string\" field.",
                state.containsKey(Constants.ORIGINAL_STRING));
        assertTrue("The parsed JSONObject should contain \"timestamp\" field.",
                state.containsKey(Constants.TIMESTAMP));
    }

    @Test
    public void testGetSetInitialLink() {
        FieldRemoveLink link = new FieldRemoveLink();
        this.parser.setInitialLink(link);
        ChainLink initialLink = this.parser.getInitialLink();
        assertEquals("The getter of the initial link should equal the item set by its setter.", link, initialLink);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoOriginalString() {
        // An exception should be thrown when a link removes the "original_string" field
        FieldRemoveLink link = new FieldRemoveLink();
        link.fieldToRemove = Constants.ORIGINAL_STRING;
        this.parser.setInitialLink(link);
        this.parser.parse("".getBytes());
    }

    @Test(expected = IllegalStateException.class)
    public void testNoTimestamp() {
        // An exception should be thrown when a link removes the "timestamp" field
        FieldRemoveLink link = new FieldRemoveLink();
        link.fieldToRemove = Constants.TIMESTAMP;
        this.parser.setInitialLink(link);
        this.parser.parse("".getBytes());
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalEncoding() {
        this.parser.setEncoding("unknown");
        this.parser.parse("".getBytes());
    }

    @Test
    public void testEncodingGetterAndSetter() {
        this.parser.setEncoding("encoding");
        assertEquals("encoding", this.parser.getEncoding());
    }

    @Test
    public void testConfiguration() {
        Map<String, String> link_1_config = new HashMap<>();
        Map<String, Object> link_2_config = new HashMap<>();
        link_1_config.put("class", "org.apache.metron.parsers.contrib.links.fields.IdentityLink");
        link_2_config.put("class", "org.apache.metron.parsers.contrib.links.io.SplitLink");
        Map<String, String> selector = new HashMap<>();
        selector.put("0", "first");
        link_2_config.put("delimiter", "|");
        link_2_config.put("selector", selector);
        Map<String, Object> parser_config = new HashMap<>();
        Map<String, Object> links_config = new HashMap<>();
        links_config.put("identity", link_1_config);
        links_config.put("split", link_2_config);
        parser_config.put("parsers", links_config);
        List<String> chain = new ArrayList<>();
        chain.add("identity");
        chain.add("split");
        parser_config.put("chain", chain);

        this.parser.configure(parser_config);
        List<JSONObject> output = this.parser.parse("1|2|3".getBytes());
        assertTrue(output.size() == 1);
        JSONObject message = output.get(0);
        assertTrue(message.containsKey("first"));
        assertEquals("1", message.get("first"));
    }

}
