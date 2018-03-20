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

import nl.qsight.links.fields.RenderLink;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TestRenderLink {

    private RenderLink link;

    @Before
    public void setUp() {
        this.link = new RenderLink();
    }

    @After
    public void tearDown() {
        this.link = null;
    }

    @Test
    public void testGetSetTemplate() {
        String template = "test";
        this.link.setTemplate(template);
        assertEquals(template, this.link.getTemplate());
    }

    @Test
    public void testGetSetOutputField() {
        String field = "output_field";
        this.link.setOutputField(field);
        assertEquals(field, this.link.getOutputField());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRenderTemplate() {
        // Test whether a template is rendered correctly
        JSONObject input = new JSONObject();
        input.put("variable1", "test1");
        input.put("variable2", "test2");

        List<String> variables = new ArrayList<>();
        variables.add("variable1");
        variables.add("variable2");
        this.link.setTemplate("{{variable1}} {{variable2}}.");
        this.link.setOutputField("result");
        this.link.setVariables(variables);
        JSONObject result = this.link.parse(input);

        assertTrue("The resulting object should have a \"result\" field.", result.containsKey("result"));
        assertEquals("The template \"{{variable1}} {{variable2}}.\" should be rendered as " +
                        "\"test1 test2.\".",
                "test1 test2.", result.get("result"));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoOutputField() {
        JSONObject input = new JSONObject();

        this.link.setTemplate("");
        this.link.parse(input);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoTemplate() {
        JSONObject input = new JSONObject();

        this.link.setOutputField("result");
        this.link.parse(input);
    }

    @Test
    public void testCurrentYearMethod() {
        JSONObject input = new JSONObject();

        this.link.setTemplate("{{year}}");
        this.link.setOutputField("result");
        List<String> variables = new ArrayList<>();
        variables.add("year");
        this.link.setVariables(variables);
        JSONObject result = this.link.parse(input);

        assertEquals("The year variable should be rendered as the current year.",
                Year.now().toString(), result.get("result"));
    }

}
