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
package nl.qsight.utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestJSONUtils {

    @Test
    public void testJSONStringToMap() throws IOException {
        String json = "{\"a\": \"b\"}";
        Map<String, Object> result = JSONUtils.JSONToMap(json);
        assertTrue(result.containsKey("a"));
        assertEquals(1, result.size());
        assertTrue(result.get("a") instanceof String);
        assertEquals("b", result.get("a"));
    }

    @Test
    public void testJSONObjectToMap() throws ParseException, IOException {
        String json = "{\"a\": \"b\"}";
        JSONObject jsonObject = JSONUtils.stringToJSON(json);
        Map<String, Object> result = JSONUtils.JSONToMap(jsonObject);
        assertTrue(result.containsKey("a"));
        assertEquals(1, result.size());
        assertTrue(result.get("a") instanceof String);
        assertEquals("b", result.get("a"));
    }

    @Test
    public void testStringToJSON() throws ParseException {
        String json = "{\"a\": \"b\"}";
        JSONObject jsonObject = JSONUtils.stringToJSON(json);
        assertTrue(jsonObject.containsKey("a"));
        assertTrue(jsonObject.get("a") instanceof String);
        assertEquals(jsonObject.get("a"), "b");
    }

    @Test
    public void testMapToJSON() throws IOException {
        String json = "{\"a\": \"b\"}";
        Map<String, Object> map = JSONUtils.JSONToMap(json);
        JSONObject jsonObject = JSONUtils.mapToJSON(map);
        assertTrue(jsonObject.containsKey("a"));
        assertTrue(jsonObject.get("a") instanceof String);
        assertEquals(jsonObject.get("a"), "b");
    }

}
