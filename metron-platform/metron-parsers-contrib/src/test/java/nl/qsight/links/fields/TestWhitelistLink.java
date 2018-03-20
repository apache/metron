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

import nl.qsight.links.fields.WhitelistLink;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestWhitelistLink {

    private WhitelistLink link;

    @Before
    public void setUp() {
        this.link = new WhitelistLink();
    }

    @After
    public void tearDown() {
        this.link = null;
    }

    @Test
    public void testGetSetFields() {
        List<String> fields = new ArrayList<>();
        fields.add("field");
        this.link.setFields(fields);
        assertEquals(fields, this.link.getFields());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWhitelistKeys() {
        JSONObject input = new JSONObject();
        input.put("variable1", "test1");
        input.put("variable2", "test2");
        input.put("variable3", "test3");

        List<String> fields = new ArrayList<>();
        fields.add("variable2");
        fields.add("variable3");
        fields.add("variable4");
        this.link.setFields(fields);

        JSONObject output = this.link.parse(input);

        assertTrue(output.containsKey("variable2"));
        assertTrue(output.containsKey("variable3"));
        assertEquals(2, output.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testNoFields() {
        JSONObject input = new JSONObject();
        this.link.parse(input);
    }

}
