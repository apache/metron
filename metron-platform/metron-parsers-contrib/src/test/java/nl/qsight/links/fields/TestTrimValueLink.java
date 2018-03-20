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
package nl.qsight.links.fields;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestTrimValueLink {

    private TrimValueLink link;

    @Before
    public void setUp() {
        this.link = new TrimValueLink();
    }

    @After
    public void tearDown() {
        this.link = null;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTrimValueFields() {
        JSONObject input = new JSONObject();
        input.put("whitespace test", "    test 1  ");

        JSONObject result = this.link.parse(input);
        assertTrue(result.containsKey("whitespace test"));
        assertEquals("test 1", result.get("whitespace test"));
        assertTrue(result.size() == 1);
    }
}
