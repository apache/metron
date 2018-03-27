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

        patterns.add("^AV\\s-\\sAlert\\s-\\s(?P<timestamp>\\d+)");
        logline = "AV - Alert - 1234";
        this.link.setPatterns(patterns);
        output = (JSONObject) this.link.parseInputField(logline);
        System.out.println(output);
    }

}
