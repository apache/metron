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
package org.apache.metron.parsers.contrib.links.fields;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestNormalizeFieldLink {

    private NormalizeFieldLink link;

    @Before
    public void setUp() {
        this.link = new NormalizeFieldLink();
    }

    @After
    public void tearDown() {
        this.link = null;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNormalizeKeys() {
        JSONObject input = new JSONObject();
        input.put("Ĉool.keyCamelIPCase23TestI", "test1");

        JSONObject result = this.link.parse(input);
        assertTrue(result.containsKey("cool_keycamelipcase23testi"));
        assertEquals("test1", result.get("cool_keycamelipcase23testi"));
        assertTrue(result.size() == 1);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testDuplicateKeys() {
        // Test whether an exception is thrown when two fields are normalized to the same key which can lead to
        // dangerous situations
        JSONObject input = new JSONObject();
        input.put("Test", "test1");
        input.put("test", "test1");

        this.link.parse(input);
    }

}
