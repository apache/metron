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
package nl.qsight.chainlink;

import nl.qsight.chainlink.ChainLinkIO;
import nl.qsight.common.Constants;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The ChainLinkIO is capable of transforming a single input field to one or more output fields.
 */
public class TestChainLinkIO {

    private AddExclamationMarkLink link_str;
    private IncrementListLink link_list;
    private MultiOutputLink link_multi;

    private class AddExclamationMarkLink extends ChainLinkIO<String> {

        @Override
        public Object parseInputField(String input) {
            return input + "!";
        }
    }

    private class IncrementListLink extends ChainLinkIO<List<Integer>> {

        @Override
        public Object parseInputField(List<Integer> input) {
            List<Integer> newList = new ArrayList<>();
            for (int i : input) {
                newList.add(i + 1);
            }
            return newList;
        }
    }

    @SuppressWarnings("unchecked")
    private class MultiOutputLink extends ChainLinkIO<String> {

        @Override
        public Object parseInputField(String input) {
            JSONObject result = new JSONObject();
            result.put("result_1", input + "?");
            result.put("result_2", input + "!");
            return result;
        }
    }

    @Before
    public void setUp() {
        this.link_str = new AddExclamationMarkLink();
        this.link_list = new IncrementListLink();
        this.link_multi = new MultiOutputLink();
    }

    @After
    public void tearDown() {
        this.link_str = null;
        this.link_list = null;
        this.link_multi = null;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStringOperations() {
        JSONObject input = new JSONObject();
        input.put("var1", "test1");
        input.put("var2", "test2");
        input.put(Constants.INPUT_MARKER, "test3");

        JSONObject output = this.link_str.parse(input);

        assertTrue(output.containsKey(Constants.OUTPUT_MARKER));
        assertTrue(output.containsKey("var1"));
        assertTrue(output.containsKey("var2"));
        assertEquals(3, output.size());
        assertEquals("test1", output.get("var1"));
        assertEquals("test2", output.get("var2"));
        assertEquals("test3!", output.get(Constants.OUTPUT_MARKER));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testListOperations() {
        JSONObject input = new JSONObject();
        List<Integer> intList = new ArrayList<>();
        intList.add(1);
        intList.add(2);
        intList.add(3);
        input.put(Constants.INPUT_MARKER, intList);

        JSONObject output = this.link_list.parse(input);

        assertTrue(output.containsKey(Constants.OUTPUT_MARKER));
        List<Integer> result = ((List<Integer>) output.get(Constants.OUTPUT_MARKER));
        assertEquals(1, output.size());
        assertEquals(3, result.size());
        assertTrue(result.get(0) == 2);
        assertTrue(result.get(1) == 3);
        assertTrue(result.get(2) == 4);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMultiOutput() {
        JSONObject input = new JSONObject();
        input.put(Constants.INPUT_MARKER, "test");

        JSONObject output = this.link_multi.parse(input);

        assertFalse(output.containsKey(Constants.OUTPUT_MARKER));
        assertTrue(output.containsKey("result_1"));
        assertTrue(output.containsKey("result_2"));
        assertEquals(2, output.size());
        assertEquals("test?", output.get("result_1"));
        assertEquals("test!", output.get("result_2"));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoInputField() {
        JSONObject input = new JSONObject();
        this.link_multi.parse(input);
    }

}
