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
package org.apache.metron.parsers.contrib.utils;

import org.apache.metron.parsers.contrib.links.fields.RenderLink;
import org.apache.metron.parsers.contrib.common.Constants;
import org.apache.metron.parsers.contrib.links.fields.RenderLink;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.apache.metron.parsers.contrib.common.Constants.INPUT_MARKER;
import static org.junit.Assert.assertEquals;

public class TestConfigUtils {

    @Test
    public void testUnfoldInput() throws ParseException, IOException {
        String strConfig = "{\"chain\": [\"example\"], \"parsers\": {\"example\": {\"input\": \"{{var1}}\"}}}";
        JSONParser parseJSON = new JSONParser();
        JSONObject configJSON = (JSONObject) parseJSON.parse(strConfig);
        Map<String, Object> config = JSONUtils.JSONToMap(configJSON);
        Map<String, Object> parserConfig = ConfigUtils.unfoldInput(config);

        assertTrue(parserConfig.containsKey("chain"));
        assertTrue(parserConfig.get("chain") instanceof List);
        List chain = (List) parserConfig.get("chain");
        assertTrue(chain.size() == 2);
        assertTrue(parserConfig.get("parsers") instanceof Map);
        Map linksConfig = (Map) parserConfig.get("parsers");

        assertTrue(chain.get(0) instanceof String);
        String autolink = (String) chain.get(0);
        assertTrue(linksConfig.containsKey(autolink));
        assertTrue(linksConfig.get(autolink) instanceof Map);
        Map configAutolink = (Map) linksConfig.get(autolink);

        assertTrue(configAutolink.containsKey("class"));
        assertTrue(configAutolink.containsKey("template"));
        assertTrue(configAutolink.containsKey("output"));

        assertTrue(configAutolink.get("class") instanceof String);
        assertTrue(configAutolink.get("template") instanceof String);
        assertTrue(configAutolink.get("output") instanceof String);

        assertEquals(RenderLink.class.getName(), configAutolink.get("class"));
        assertEquals("{{var1}}", configAutolink.get("template"));
        Assert.assertEquals(Constants.INPUT_MARKER, configAutolink.get("output"));
    }

}
