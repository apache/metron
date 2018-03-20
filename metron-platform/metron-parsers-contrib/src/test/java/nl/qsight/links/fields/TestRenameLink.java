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

import nl.qsight.links.fields.RenameLink;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestRenameLink {

    private RenameLink link;

    @Before
    public void setUp() {
        this.link = new RenameLink();
    }

    @After
    public void tearDown() {
        this.link = null;
    }

    @Test
    public void testGetSetRenames() {
        Map<String, String> renames = new HashMap<>();
        renames.put("from", "to");
        this.link.setRenames(renames);
        assertEquals(renames, this.link.getRenames());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRename() {
        JSONObject input = new JSONObject();
        input.put("variable1", "test1");
        input.put("variable2", "test2");
        input.put("variable3", "test3");

        Map<String, String> renames = new HashMap<>();
        renames.put("variable1", "variable3");
        renames.put("variable2", "variable4");
        this.link.setRenames(renames);

        JSONObject output = this.link.parse(input);

        assertTrue(output.containsKey("variable3"));
        assertTrue(output.containsKey("variable4"));
        assertEquals(2, output.size());
        assertEquals(output.get("variable3"), "test1");
        assertEquals(output.get("variable4"), "test2");
    }

}
