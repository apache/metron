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
package nl.qsight.links.io;

import nl.qsight.chainlink.ChainLinkIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;

/**
 * A link for decoding JSON.
 */
public class JSONDecoderLink extends ChainLinkIO<String> {

    @Override
    public Object parseInputField(String input) {
        JSONParser parser = new JSONParser();
        JSONObject result;
        try {
            result = flatten((JSONObject) parser.parse(input));
        } catch (ParseException e) {
            throw new IllegalStateException("Could not parse JSON in the message.");
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private JSONObject flatten(JSONObject original) {
        JSONObject result = new JSONObject();
        for (Object key : original.keySet()) {
            Object value = original.get(key);
            if (value instanceof JSONObject) {
                JSONObject subjson = flatten((JSONObject) value);
                for (Object subitem : subjson.entrySet()) {
                    Map.Entry entry = (Map.Entry) subitem;
                    String new_key = key + "." + entry.getKey();
                    result.put(new_key, entry.getValue());
                }
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

}
